package com.xml.guard.model

import com.xml.guard.utils.KtFileParser
import com.xml.guard.utils.findLocationProject
import com.xml.guard.utils.findPackage
import com.xml.guard.utils.getDirPath
import com.xml.guard.utils.inClassNameBlackList
import com.xml.guard.utils.inPackageNameBlackList
import com.xml.guard.utils.insertImportXxxIfAbsent
import com.xml.guard.utils.javaDirs
import com.xml.guard.utils.removeSuffix
import com.xml.guard.utils.toLetterStr
import com.xml.guard.utils.toUpperLetterStr
import org.gradle.api.Project
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer


/**
 * User: ljx
 * Date: 2022/3/16
 * Time: 22:02
 */
class Mapping {

    companion object {
        internal const val DIR_MAPPING = "dir mapping:"
        internal const val CLASS_MAPPING = "class mapping:"
    }

    internal val dirMapping = mutableMapOf<String, String>()
    internal val classMapping = mutableMapOf<String, String>()

    //类名索引
    internal var classIndex = -1L

    //包名索引
    internal var packageNameIndex = -1L

    //遍历文件夹下的所有直接子类，混淆文件名及移动目录
    fun obfuscateAllClass(project: Project, variantName: String): MutableMap<String, String> {
        val classMapped = mutableMapOf<String, String>()
        val iterator = dirMapping.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val rawDir = entry.key
            val locationProject = project.findLocationProject(rawDir, variantName)
            if (locationProject == null) {
                iterator.remove()
                continue
            }
            val manifestPackage = locationProject.findPackage()
            //过滤目录的直接子文件
            val dirPath = rawDir.replace(".", File.separator) //xx.xx  不带文件名
            val childFiles = locationProject.javaDirs(variantName).flatMap {
                File(it, dirPath).listFiles { f ->
                    val filename = f.name
                    f.isFile && (filename.endsWith(".java") || filename.endsWith(".kt"))
                }?.toList() ?: emptyList()
            }
            if (childFiles.isEmpty()) continue
            for (file in childFiles) {
                val rawClassPath = "${rawDir}.${file.name.removeSuffix()}" //原始 xx.Xxx
                //已经混淆
                if (isObfuscated(rawClassPath)) continue
                if (rawDir == manifestPackage) {
                    file.insertImportXxxIfAbsent(manifestPackage)
                }
                val obfuscatePath = obfuscatePath(rawClassPath)  //混淆后 xx.Xxx
                val obfuscateRelativePath = obfuscatePath.replace(".", File.separator) //混淆后 xx/Xxx
                val rawRelativePath = rawClassPath.replace(".", File.separator) //原始 xx/Xxx
                //替换原始类路径
                val newFile =
                    File(file.absolutePath.replace(rawRelativePath, obfuscateRelativePath))
                if (!newFile.exists()) newFile.parentFile.mkdirs()
                if (file.renameTo(newFile)) {
                    classMapped[rawClassPath] = obfuscatePath

                    //处理顶级类、方法及变量
                    val obfuscateDir = obfuscatePath.getDirPath()
                    val filename = file.name.removeSuffix()
                    val ktParser = KtFileParser(newFile, filename)
                    val jvmName = ktParser.jvmName
                    if (jvmName != null && jvmName != filename) {
                        classMapped["$rawDir.$jvmName"] = "$obfuscateDir.$jvmName"
                    } else if (jvmName == null &&
                        (ktParser.topFunNames.isNotEmpty() || ktParser.topFieldNames.isNotEmpty())
                    ) {
                        classMapped["${rawClassPath}Kt"] = "${obfuscatePath}Kt"
                    }
                    ktParser.getTopClassOrFunOrFieldNames().forEach {
                        classMapped["$rawDir.$it"] = "$obfuscateDir.$it"
                    }
                }
            }
        }
        return classMapped
    }

    fun isObfuscated(rawClassPath: String) = classMapping.containsValue(rawClassPath)

    //混淆包名+类名，返回混淆后的包名+类名
    fun obfuscatePath(classPath: String): String {
        var innerClassName: String? = null //内部类类名
        val rawClassPath = if (isInnerClass(classPath)) {
            val arr = classPath.split("$")
            innerClassName = arr[1]
            arr[0]
        } else {
            classPath
        }
        var obfuscateClassPath = classMapping[rawClassPath]
        if (obfuscateClassPath == null) {
            val rawPackage = rawClassPath.getDirPath()
            val obfuscatePackage = obfuscatePackage(rawPackage)
            obfuscateClassPath = "$obfuscatePackage.${generateObfuscateClassName()}"
            classMapping[rawClassPath] = obfuscateClassPath
        }
        return if (innerClassName != null) "$obfuscateClassPath\$$innerClassName" else obfuscateClassPath
    }


    fun writeMappingToFile(mappingFile: File) {
        val writer: Writer = BufferedWriter(FileWriter(mappingFile, false))

        writer.write("$DIR_MAPPING\n")
        for ((key, value) in dirMapping) {
            writer.write(String.format("\t%s -> %s\n", key, value))
        }
        writer.write("\n")
        writer.flush()

        writer.write("$CLASS_MAPPING\n")
        for ((key, value) in classMapping.entries) {
            writer.write(String.format("\t%s -> %s\n", key, value))
        }
        writer.flush()

        writer.close()
    }

    //混淆包名，返回混淆后的包名
    private fun obfuscatePackage(rawPackage: String): String {
        var obfuscatePackage = dirMapping[rawPackage]
        if (obfuscatePackage == null) {
            obfuscatePackage = generateObfuscatePackageName()
            dirMapping[rawPackage] = obfuscatePackage
        }
        return obfuscatePackage
    }

    //生成混淆的包名
    private fun generateObfuscatePackageName(): String {
        while (true) {
            val obfuscatePackage = (++packageNameIndex).toLetterStr()
            if (!obfuscatePackage.inPackageNameBlackList()) //过滤黑名单
                return obfuscatePackage
        }
    }

    //生成混淆的类名
    private fun generateObfuscateClassName(): String {
        while (true) {
            val obfuscateClassName = (++classIndex).toUpperLetterStr()
            if (!obfuscateClassName.inClassNameBlackList()) //过滤黑名单
                return obfuscateClassName
        }
    }

    private fun isInnerClass(classPath: String): Boolean {
        return classPath.contains("[a-zA-Z0-9_]+\\$[a-zA-Z0-9_]+".toRegex())
    }
}