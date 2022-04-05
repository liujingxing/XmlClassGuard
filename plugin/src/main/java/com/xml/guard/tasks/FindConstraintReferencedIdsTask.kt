package com.xml.guard.tasks

import com.bytedance.android.plugin.extensions.AabResGuardExtension
import com.xml.guard.utils.isAndroidProject
import com.xml.guard.utils.resDir
import groovy.util.Node
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*

/**
 * User: ljx
 * Date: 2022/4/3
 * Time: 20:17
 */
open class FindConstraintReferencedIdsTask : DefaultTask() {

    private val aabResGuard: AabResGuardExtension =
        project.extensions.getByName("aabResGuard") as AabResGuardExtension

    init {
        group = "guard"
    }

    @TaskAction
    fun execute() {
        val layoutDirs = mutableListOf<File>()
        project.rootProject.subprojects {
            if (it.isAndroidProject()) {
                it.resDir().listFiles { file ->
                    file.isDirectory && file.name.startsWith("layout")   //过滤res目录下的layout
                }?.apply { layoutDirs.addAll(this) }
            }
        }
        val set = findReferencedIds(layoutDirs)
        (aabResGuard.whiteList as HashSet).addAll(set)
        println("ids=$set")
    }

    private fun findReferencedIds(layoutDirs: List<File>): Set<String> {
        val set = HashSet<String>()
        layoutDirs
            .flatMap {
                val listFiles: Array<File>? = it.listFiles { file -> file.name.endsWith(".xml") }
                listFiles?.toMutableList() ?: Collections.emptyList()
            }.forEach { layoutFile ->
                set.addAll(layoutFile.findReferencedIds())
            }
        return set
    }

    private fun File.findReferencedIds(): Set<String> {
        val set = HashSet<String>()
        val childrenList = XmlParser(false, false).parse(this).breadthFirst()
        for (children in childrenList) {
            val childNode = children as? Node ?: continue
            val ids = childNode.attribute("app:constraint_referenced_ids")?.toString()
            if (ids.isNullOrBlank()) continue
            ids.split(",").forEach {
                val id = it.trim()
                if (id.isNotEmpty()) {
                    set.add("*.R.id.${id}")
                }
            }
        }
        return set
    }
}



