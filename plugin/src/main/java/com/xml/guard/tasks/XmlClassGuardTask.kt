package com.xml.guard.tasks

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.model.ClassInfo
import com.xml.guard.model.MappingParser
import com.xml.guard.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import javax.inject.Inject

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:06
 */
open class XmlClassGuardTask @Inject constructor(
    guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val mappingFile = guardExtension.mappingFile ?: project.file("xml-class-mapping.txt")
    private val mapping = MappingParser.parse(mappingFile)
    private val hasNavigationPlugin = project.plugins.hasPlugin("androidx.navigation.safeargs")
    private val fragmentDirectionList = mutableListOf<String>()
    private val topFunVarClassPath = mutableMapOf<String, String>()

    @TaskAction
    fun execute() {
        val androidProjects = allDependencyAndroidProjects()
        //1、遍历res下的xml文件，找到自定义的类(View/Fragment/四大组件等)，并将混淆结果同步到xml文件内
        androidProjects.forEach { handleResDir(it) }
        //2、混淆文件名及文件路径，返回本次混淆的类
        val classMapping = mapping.obfuscateAllClass(project, variantName)
        if (hasNavigationPlugin && fragmentDirectionList.isNotEmpty()) {
            fragmentDirectionList.forEach {
                classMapping["${it}Directions"] = "${classMapping[it]}Directions"
            }
        }
        //3、替换Java/kotlin文件里引用到的类
        if (classMapping.isNotEmpty()) {
            androidProjects.forEach { replaceJavaText(it, classMapping) }
        }
        findTopFunVarClass(project, classMapping)
        if (topFunVarClassPath.isNotEmpty()) {
            androidProjects.forEach {
                replaceKotlinText(it)
            }
        }
        //4、混淆映射写出到文件
        mapping.writeMappingToFile(mappingFile)
    }

    //处理res目录
    private fun handleResDir(project: Project) {
        //过滤res目录下的layout、navigation、xml目录
        val xmlDirs = project.findXmlDirs(variantName, "layout", "navigation", "xml")
        xmlDirs.add(project.manifestFile())
        project.files(xmlDirs).asFileTree.forEach { xmlFile ->
            guardXml(project, xmlFile)
        }
    }

    private fun guardXml(project: Project, xmlFile: File) {
        var xmlText = xmlFile.readText()
        val classInfoList = mutableListOf<ClassInfo>()
        val parentName = xmlFile.parentFile.name
        var packageName: String? = null
        when {
            parentName.startsWith("navigation") -> {
                findFragmentInfoList(xmlText).let { classInfoList.addAll(it) }
            }

            listOf("layout", "xml").any { parentName.startsWith(it) } -> {
                findClassByLayoutXml(xmlText).let { classInfoList.addAll(it) }
            }

            xmlFile.name == "AndroidManifest.xml" -> {
                val tempPackageName = project.findPackage()
                packageName = tempPackageName
                findClassByManifest(xmlText, tempPackageName).let { classInfoList.addAll(it) }
            }
        }
        if (hasNavigationPlugin) {
            classInfoList.mapNotNullTo(fragmentDirectionList) {
                if (it.hasAction) it.classPath else null
            }
        }
        for (classInfo in classInfoList) {
            val classPath = classInfo.classPath
            val dirPath = classPath.getDirPath()
            //本地不存在这个文件
            if (project.findLocationProject(dirPath, variantName) == null) continue
            //已经混淆了这个类
            if (mapping.isObfuscated(classPath)) continue
            val obfuscatePath = mapping.obfuscatePath(classPath)
            xmlText = xmlText.replaceWords(classPath, obfuscatePath)
            if (packageName != null && classPath.startsWith(packageName)) {
                xmlText =
                    xmlText.replaceWords(classPath.substring(packageName.length), obfuscatePath)
            }
            if (classInfo.fromImportNode) {
                var classStartIndex = classPath.indexOfLast { it == '.' }
                if (classStartIndex == -1) continue
                val rawClassName = classPath.substring(classStartIndex + 1)
                classStartIndex = obfuscatePath.indexOfLast { it == '.' }
                if (classStartIndex == -1) continue
                val obfuscateClassName = obfuscatePath.substring(classStartIndex + 1)
                xmlText = xmlText.replaceWords("${rawClassName}.", "${obfuscateClassName}.")
            }
        }
        xmlFile.writeText(xmlText)
    }


    private fun replaceJavaText(project: Project, mapping: Map<String, String>) {
        val javaDirs = project.javaDirs(variantName)
        //遍历所有Java\Kt文件，替换混淆后的类的引用，import及new对象的地方
        project.files(javaDirs).asFileTree.forEach { javaFile ->
            var replaceText = javaFile.readText()
            mapping.forEach {
                replaceText = replaceText(javaFile, replaceText, it.key, it.value)
            }
            javaFile.writeText(replaceText)
        }
    }

    private fun replaceText(
        rawFile: File,
        rawText: String,
        rawPath: String,
        obfuscatePath: String,
    ): String {
        val rawIndex = rawPath.lastIndexOf(".")
        val rawPackage = rawPath.substring(0, rawIndex)
        val rawName = rawPath.substring(rawIndex + 1)

        val obfuscateIndex = obfuscatePath.lastIndexOf(".")
        val obfuscatePackage = obfuscatePath.substring(0, obfuscateIndex)
        val obfuscateName = obfuscatePath.substring(obfuscateIndex + 1)

        var replaceText = rawText
        when {
            rawFile.absolutePath.removeSuffix()
                .endsWith(obfuscatePath.replace(".", File.separator)) -> {
                //对于自己，替换package语句及类名即可
                replaceText = replaceText
                    .replaceWords("package $rawPackage", "package $obfuscatePackage")
                    .replaceWords(rawPath, obfuscatePath)
                    .replaceWords(rawName, obfuscateName)
            }

            rawFile.parent.endsWith(obfuscatePackage.replace(".", File.separator)) -> {
                //同一包下的类，原则上替换类名即可，但考虑到会依赖同包下类的内部类，所以也需要替换包名+类名
                replaceText = replaceText.replaceWords(rawPath, obfuscatePath)  //替换{包名+类名}
                    .replaceWords(rawName, obfuscateName)
            }

            else -> {
                replaceText = replaceText.replaceWords(rawPath, obfuscatePath)  //替换{包名+类名}
                    .replaceWords("$rawPackage.*", "$obfuscatePackage.*")
                //替换成功或已替换
                if (replaceText != rawText || replaceText.contains("$obfuscatePackage.*")) {
                    //rawFile 文件内有引用 rawName 类，则需要替换类名
                    replaceText = replaceText.replaceWords(rawName, obfuscateName)
                }
            }
        }
        return replaceText
    }

    private fun findTopFunVarClass(project: Project, classMapping: Map<String, String>) {
        val javaDirs = project.javaDirs(variantName)

        fun addToTopFunMap(key: String, value: String) {
            topFunVarClassPath.putAll(mapOf(key to value))
        }

        //判断移除class文件名相同的类之后文件内容存在以下关键词
        fun isTopKtFile(content: String): Boolean {
            val topFileKey = listOf("typealias", "val", "var", "class")
            topFileKey.forEach {
                if (content.contains(it)) {
                    return true
                }
            }
            return false
        }

        project.files(javaDirs).asFileTree.forEach { javaFile ->
            //这个是混淆过的name
            val fileName = javaFile.name

            if (!fileName.endsWith(".kt")) {
                return@forEach
            }

            val assumeClazzName = fileName.split(".")[0]
            val fileText = javaFile.readText()

            var javaFileReader: FileReader? = null
            var buffer: BufferedReader? = null
            var javaFileFirstLine = ""
            try {
                javaFileReader = FileReader(javaFile)
                buffer = BufferedReader(javaFileReader)
                javaFileFirstLine = buffer.readLine().removeSuffix(";")
            } catch (ex: Exception) {
                ex.printStackTrace()
            } finally {
                buffer?.close()
                javaFileReader?.close()
            }

            //获取文件的packagename
            val filePackageName = javaFileFirstLine.substring(8)

            //如果不存在当前文件名的class 表明这个文件存在其他的class或者顶层函数和变量
            if (!fileText.contains("class $assumeClazzName", ignoreCase = false)) {
                classMapping.forEach { (key, value) ->
                    if (value == "$filePackageName.$assumeClazzName") {
                        addToTopFunMap(key, value)
                    }
                }
                return@forEach
            }

            //下面移除和文件名同名的class文件内容
            val clazzStartIndex = fileText.indexOf("class $assumeClazzName")
            val needSearchBracketText = fileText.substring(clazzStartIndex)

            var brCounter = 0
            var clazzEndIndex = 0
            var charCounter = 0
            var findIsValid = false
            for (s in needSearchBracketText) {
                charCounter += 1
                if (s == '{') {
                    findIsValid = true
                    brCounter += 1
                } else if (s == '}') {
                    brCounter -= 1
                }
                if (brCounter == 0 && findIsValid) {
                    clazzEndIndex = (charCounter + clazzStartIndex)
                    break
                }
            }
            val noClazzFileContent = fileText.removeRange(clazzStartIndex, clazzEndIndex)

            if (isTopKtFile(noClazzFileContent)) {
                classMapping.forEach { (key, value) ->
                    if (value == "$filePackageName.$assumeClazzName") {
                        addToTopFunMap(key ,value)
                    }
                }
            }
        }
    }

    private fun replaceKotlinText(project: Project) {
        val javaDirs = project.javaDirs(variantName)
        //遍历所有Java\Kt文件，替换混淆后的类的引用，import及new对象的地方
        project.files(javaDirs).asFileTree.forEach { javaFile ->
            var replaceText = javaFile.readText()
            topFunVarClassPath.forEach {
                replaceText = replaceKotlinFunText(replaceText, it.key, it.value)
            }
            javaFile.writeText(replaceText)
        }
    }

    private fun replaceKotlinFunText(
        rawText: String,
        rawFilePackagePath: String,
        obfuscatePackagePath: String
    ): String {
        val rawPackage = rawFilePackagePath.removeSuffix()
        val obfuscatePackage = obfuscatePackagePath.removeSuffix()
        if (!rawText.contains(rawPackage)) {
            return rawText
        }
        var replaceText = rawText
        replaceText = replaceText.replace(rawPackage, obfuscatePackage)
            .replace("$obfuscatePackage.R", "$rawPackage.R")
            .replace("$obfuscatePackage.BR", "$rawPackage.BR")
            .replace("$obfuscatePackage.BuildConfig", "$rawPackage.BuildConfig")
            .replace("$obfuscatePackage.databinding", "$rawPackage.databinding")
        return replaceText
    }
}