package com.xml.guard.tasks

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.utils.allDependencyAndroidProjects
import com.xml.guard.utils.findPackage
import com.xml.guard.utils.findXmlDirs
import com.xml.guard.utils.insertImportXxxIfAbsent
import com.xml.guard.utils.javaDirs
import com.xml.guard.utils.manifestFile
import com.xml.guard.utils.replaceWords
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
open class MoveDirTask @Inject constructor(
    private val guardExtension: GuardExtension,
    private val variantName: String,
) : DefaultTask() {

    init {
        group = "guard"
    }

    @TaskAction
    fun execute() {
        val moveFile = guardExtension.moveDir
        if (moveFile.isEmpty()) return
        val androidProjects = allDependencyAndroidProjects()
        androidProjects.forEach { it.moveDir(moveFile) }
    }

    private fun Project.moveDir(moveFile: Map<String, String>) {
        val manifestPackage = findPackage()  //查找清单文件里的package属性值
        //1、替换manifest文件 、layout、navigation、xml目录下的文件、Java、Kt文件
        val dirs = findXmlDirs(variantName, "layout", "navigation", "xml")
        dirs.add(manifestFile())
        val javaDirs = javaDirs(variantName)
        dirs.addAll(javaDirs)
        files(dirs).asFileTree.forEach {
            it.replaceText(moveFile, manifestPackage)
        }

        // 2、开始移动目录
        moveFile.forEach { (oldPath, newPath) ->
            val oldPackagePath = oldPath.replace(".", File.separator)
            for (javaDir in javaDirs) {
                val oldDir = File(javaDir, oldPackagePath)
                if (!oldDir.exists()) {
                    continue
                }
                if (oldPath == manifestPackage) {
                    //包名目录下的直接子类移动位置，需要重新手动导入R类及BuildConfig类(如果有用到的话)
                    oldDir.listFiles { f -> !f.isDirectory }?.forEach { file ->
                        file.insertImportXxxIfAbsent(oldPath)
                    }
                }
                val oldRelativePath = oldPath.replace(".", File.separator)
                val newRelativePath = newPath.replace(".", File.separator)
                val toFile = File(oldDir.absolutePath.replace(oldRelativePath, newRelativePath))
                copy {
                    it.from(oldDir)
                    it.into(toFile)
                }
                delete(oldDir)
            }
        }
    }

    private fun File.replaceText(map: Map<String, String>, manifestPackage: String?) {
        var replaceText = readText()
        map.forEach { (oldPath, newPath) ->
            replaceText =
                if (name == "AndroidManifest.xml" && oldPath == manifestPackage) {
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
                    .replaceWords("$newPath.BR", "$oldPath.BR")
                    .replaceWords("$newPath.BuildConfig", "$oldPath.BuildConfig")
                    .replaceWords("$newPath.databinding", "$oldPath.databinding")
            }
        }
        writeText(replaceText)
    }

}