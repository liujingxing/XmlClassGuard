package com.xml.guard.tasks

import com.android.build.gradle.BaseExtension
import com.xml.guard.entensions.GuardExtension
import com.xml.guard.utils.allDependencyAndroidProjects
import com.xml.guard.utils.insertImportXxxIfAbsent
import com.xml.guard.utils.javaDir
import com.xml.guard.utils.manifestFile
import com.xml.guard.utils.replaceWords
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:06
 */
open class PackageChangeTask @Inject constructor(
    private val guardExtension: GuardExtension
) : DefaultTask() {

    init {
        group = "guard"
    }

    @TaskAction
    fun execute() {
        val packageExtension = guardExtension.packageChange
        if (packageExtension.isEmpty()) return
        val androidProjects = allDependencyAndroidProjects()
        androidProjects.forEach { it.changePackage(packageExtension) }
    }

    private fun Project.changePackage(map: Map<String, String>) {
        //1.修改manifest文件 或 build.gradle namespace
        val pair = getNewPackageAndModifyOldPackage(map) ?: return
        val oldPackage = pair.first
        val newPackage = pair.second
        //2.修改 kt/java文件
        files("src/main/java").asFileTree.forEach { javaFile ->
            javaFile.readText()
                .replaceWords("$oldPackage.R", "$newPackage.R")
                .replaceWords("$oldPackage.BuildConfig", "$newPackage.BuildConfig")
                .replaceWords("$oldPackage.databinding", "$newPackage.databinding")
                .let { javaFile.writeText(it) }
        }

        //3.对旧包名下的直接子类，检测R类、BuildConfig类是否有用到，有的话，插入import语句
        javaDir(oldPackage.replace(".", File.separator))
            .listFiles { f -> !f.isDirectory }
            ?.forEach { file ->
                file.insertImportXxxIfAbsent(newPackage)
            }
    }

    private fun Project.getNewPackageAndModifyOldPackage(map: Map<String, String>): Pair<String, String>? {
        // 配置了 namespace
        val namespace = (project.extensions.getByName("android") as BaseExtension).namespace
        if (namespace == null) {
            val manifestFile = manifestFile()
            val oldPackage = manifestFile.findPackage() ?: return null
            val newPackage = map[oldPackage] ?: return null
            manifestFile.readText()
                .replaceWords("""package="$oldPackage"""", """package="$newPackage"""")
                .replaceWords("""android:name=".""", """android:name="$oldPackage.""")
                .let { manifestFile.writeText(it) }
            return Pair(oldPackage, newPackage)
        } else {
            val newPackage = map[namespace] ?: return null
            buildFile.readText()
                .replace("namespace\\s+['\"]${namespace}['\"]".toRegex(), "namespace '$newPackage'")
                .let { buildFile.writeText(it) }
            return Pair(namespace, newPackage)
        }
    }

    private fun File.findPackage(): String? {
        val rootNode = XmlParser(false, false).parse(this)
        return rootNode.attribute("package")?.toString()
    }

}