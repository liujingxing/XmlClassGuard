package com.xml.guard.tasks

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.utils.insertImportXxxIfAbsent
import com.xml.guard.utils.javaDir
import com.xml.guard.utils.manifestFile
import com.xml.guard.utils.replaceWords
import com.xml.guard.utils.resDir
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:06
 */
open class MoveDirTask @Inject constructor(
    private val guardExtension: GuardExtension
) : DefaultTask() {

    init {
        group = "guard"
    }

    private val manifestPackage = project.manifestFile().findPackage()

    @TaskAction
    fun execute() {
        val moveFile = guardExtension.moveDir
        if (moveFile.isEmpty()) return
        // 1、替换manifest文件 、layout/navigation目录下的文件、Java、Kt文件
        val listFiles = project.resDir().listFiles { _, name ->
            //过滤res目录下的layout、navigation目录
            name.startsWith("layout") || name.startsWith("navigation")
        }?.toMutableList() ?: return
        listFiles.add(project.manifestFile())
        listFiles.add(project.javaDir())
        project.files(listFiles).asFileTree.forEach {
            it.replaceText(moveFile)
        }

        // 2、开始移动目录
        moveFile.forEach { (oldPath, newPath) ->
            val oldDir = project.javaDir(oldPath.replace(".", "/"))
            if (oldPath == manifestPackage) {
                //包名目录下的直接子类移动位置，需要重新手动导入R类及BuildConfig类(如果有用到的话)
                oldDir.listFiles { f -> !f.isDirectory }?.forEach { file ->
                    file.insertImportXxxIfAbsent(oldPath)
                }
            }
            project.copy {
                it.from(oldDir)
                it.into(project.javaDir(newPath.replace(".", "/")))
            }
            project.delete(oldDir)
        }
    }

    private fun File.findPackage(): String? {
        val rootNode = XmlParser(false, false).parse(this)
        return rootNode.attribute("package")?.toString()
    }

    private fun File.replaceText(map: Map<String, String>) {
        var replaceText = readText()
        map.forEach { (oldPath, newPath) ->
            replaceText = if (name == "AndroidManifest.xml" && oldPath == manifestPackage) {
                replaceText.replaceWords("$oldPath.", "$newPath.")
                    .replaceWords("""android:name=".""", """android:name="${newPath}.""")
            } else {
                replaceText.replaceWords(oldPath, newPath)
            }
            if (name.endsWith(".kt") || name.endsWith(".java")) {
                /*
                 移动目录时，manifest里的package属性不会更改
                 上面代码会将将R、BuildConfig等类路径替换掉，所以这里需要还原回去
                 */
                replaceText = replaceText.replaceWords("$newPath.R", "$oldPath.R")
                    .replaceWords("$newPath.BuildConfig", "$oldPath.BuildConfig")
                    .replaceWords("$newPath.databinding", "$newPath.databinding")
            }
        }
        writeText(replaceText)
    }

}