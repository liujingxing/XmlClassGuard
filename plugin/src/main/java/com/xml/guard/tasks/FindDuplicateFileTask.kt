package com.xml.guard.tasks

import com.xml.guard.utils.aidlDirs
import com.xml.guard.utils.allDependencyAndroidProjects
import com.xml.guard.utils.javaDirs
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

/**
 * User: debug
 * Date: 2025/01/15
 * Time: 13:06
 * 扫描项目中相同名字的类.
 * 扫描结果查看日志:发现相同名字的类.
 * 为什么要增加这个功能? 这个功能和其他功能不冲突,只是做一个补充.
 * 在使用xmlClassGuard任务混淆代码后,可能存在这样的问题.
 * 例如你的代码中有/data/Book.java和/app/test/Book.java甚至多个目录下存在相同文件
 * 名字,当混淆之后,存在导包混乱情况,在使用/app/test/Book.java的类可能导入了
 * data/Book.java的类. 在执行xmlClassGuard之前检测一次相同类名的类,自己进行重命名唯一的类
 * 可以避免上述问题.
 */
open class FindDuplicateFileTask @Inject constructor(
    private val variantName: String,
) : DefaultTask() {

    init {
        // 将任务设置在那个分组, gradle任务组中,例如install,help,build等
        group = "guard"
    }

    @TaskAction
    fun execute() {
        // 任务执行入口
        // 返回主module依赖的所有Android子module，包含间接依赖的
        val androidProjects = allDependencyAndroidProjects()
        androidProjects.forEach { it.startScan() }
    }

    /**
     * 扫描代码
     */
    private fun Project.startScan() {
        val dirs = mutableListOf<File>();
        val aidlDirs = aidlDirs(variantName)
        dirs.addAll(aidlDirs)
        val javaDirs = javaDirs(variantName)
        dirs.addAll(javaDirs)
        val filePaths = mutableListOf<String>()
        // 储存文件路径
        files(dirs).asFileTree.forEach {
            filePaths.add(it.absolutePath)
        }

        // 用于存储具有相同基本名称的文件
        val fileNameMap = mutableMapOf<String, MutableList<String>>()
        // 遍历文件路径列表，将文件名作为键，文件路径作为值存入 Map
        for (filePath in filePaths) {
            // 提取文件名（包含扩展名）
            val fileName = filePath.substringAfterLast("/")

            // 将文件路径添加到对应的列表中
            fileNameMap.computeIfAbsent(fileName) { mutableListOf() }.add(filePath)
        }
        // 过滤出那些具有多个路径的文件名
        val filteredList = fileNameMap.filter { it.value.size > 1 }.values.flatten()

        // 输出过滤后的文件路径列表
        if (filteredList.isEmpty()) {
            return
        }
        println("发现相同名字的类:")
        println(filteredList)
    }
}