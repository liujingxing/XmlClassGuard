package com.xml.guard.utils

import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import java.io.File

/**
 * User: ljx
 * Date: 2022/3/6
 * Time: 22:54
 */

val whiteList = arrayListOf(
    "layout", "data", "variable", "import", "merge", "ViewStub", "include",
    "LinearLayout", "RelativeLayout", "FrameLayout", "AbsoluteLayout",
    "Button", "TextView", "View", "ImageView", "EditText", "ProgressBar",
    "androidx.constraintlayout.widget.ConstraintLayout",
    "androidx.core.widget.NestedScrollView",
    "androidx.constraintlayout.widget.Group",
    "androidx.constraintlayout.widget.Guideline",
    "androidx.appcompat.widget.Toolbar",
    "com.google.android.material.button.MaterialButton",
    "GridLayout", "GridView",
)

fun Project.javaDir(path: String = ""): File = file("src/main/java/$path")

fun Project.resDir(path: String = ""): File = file("src/main/res/$path")

fun Project.manifestFile(): File = file("src/main/AndroidManifest.xml")

fun Project.javaTree(path: String = ""): ConfigurableFileTree = fileTree("src/main/java/$path")

fun Project.resTree(path: String = ""): ConfigurableFileTree = fileTree("src/main/res/$path")


//查找依赖的Android Project，也就是子 module，包括间接依赖的子 module
fun Project.findDependencyAndroidProject(
    projects: MutableList<Project>,
    names: List<String> = mutableListOf("api", "implementation")
) {
    names.forEach { name ->
        val dependencyProjects = configurations.getByName(name).dependencies
            .filterIsInstance<DefaultProjectDependency>()
            .filter { it.dependencyProject.isAndroidProject() }
            .map { it.dependencyProject }
        projects.addAll(dependencyProjects)
        dependencyProjects.forEach {
            it.findDependencyAndroidProject(projects, mutableListOf("api"))
        }
    }
}

fun Project.isAndroidProject() =
    plugins.hasPlugin("com.android.application")
            || plugins.hasPlugin("com.android.library")

//查找dir所在的Project，dir不存在，返回null
fun Project.findLocationProject(dir: String): Project? {
    val packageName = dir.replace(".", File.separator)
    val absoluteDir = javaDir(packageName)
    if (absoluteDir.exists()) {
        return this
    }
    val dependencyProjects = mutableListOf<Project>()
    findDependencyAndroidProject(dependencyProjects)
    dependencyProjects.forEach {
        val project = it.findLocationProject(dir)
        if (project != null) return project
    }
    return null
}

fun findClassByLayoutXml(text: String, classPaths: MutableList<String>) {
    val childrenList = XmlParser(false, false).parseText(text).breadthFirst()
    for (children in childrenList) {
        val childNode = children as? Node ?: continue
        val classPath = childNode.name().toString()
        if (classPath !in whiteList) {
            classPaths.add(classPath)
            val layoutManager = childNode.attribute("app:layoutManager")?.toString()
            if (layoutManager != null && !layoutManager.startsWith("androidx.recyclerview.widget.")) {
                classPaths.add(layoutManager)
            }
        }
    }
}

fun findClassByNavigationXml(text: String, classPaths: MutableList<String>) {
    val rootNode = XmlParser(false, false).parseText(text)
    for (children in rootNode.children()) {
        val childNode = children as? Node ?: continue
        val childName = childNode.name()
        if ("fragment" == childName) {
            val classPath = childNode.attribute("android:name").toString()
            classPaths.add(classPath)
        }
    }
}

//在manifest文件里，查找四大组件及Application，返回文件的package属性，即包名
fun findClassByManifest(text: String, classPaths: MutableList<String>, namespace: String?): String {
    val rootNode = XmlParser(false, false).parseText(text)
    val packageName = namespace ?: rootNode.attribute("package").toString()
    val nodeList = rootNode.get("application") as? NodeList ?: return packageName
    val applicationNode = nodeList.firstOrNull() as? Node ?: return packageName
    val application = applicationNode.attribute("android:name")?.toString()
    if (application != null) {
        val classPath = if (application.startsWith(".")) packageName + application else application
        classPaths.add(classPath)
    }
    for (children in applicationNode.children()) {
        val childNode = children as? Node ?: continue
        val childName = childNode.name()
        if ("activity" == childName || "service" == childName ||
            "receiver" == childName || "provider" == childName
        ) {
            val name = childNode.attribute("android:name").toString()
            val classPath = if (name.startsWith(".")) packageName + name else name
            classPaths.add(classPath)
        }
    }
    return packageName
}