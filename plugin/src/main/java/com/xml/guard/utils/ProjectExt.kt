package com.xml.guard.utils

import com.android.build.gradle.BaseExtension
import com.xml.guard.model.ClassInfo
import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import java.io.File

/**
 * User: ljx
 * Date: 2022/3/6
 * Time: 22:54
 */

val whiteList = arrayListOf(
    "layout", "data", "merge", "ViewStub", "include",
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

fun Project.findPackage(): String {
    if (AgpVersion.versionCompare("4.2.0") >= 0) {
        val namespace = (extensions.getByName("android") as BaseExtension).namespace
        if (namespace != null) {
            return namespace
        }
    }
    val rootNode = XmlParser(false, false).parse(manifestFile())
    return rootNode.attribute("package").toString()
}

//返回java/kotlin代码目录,可能有多个
fun Project.javaDirs(variantName: String): List<File> {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    val nameSet = mutableSetOf<String>()
    nameSet.add("main")
    if (isAndroidProject()) {
        nameSet.addAll(variantName.splitWords())
    }
    val javaDirs = mutableListOf<File>()
    sourceSet.names.forEach { name ->
        if (nameSet.contains(name)) {
            sourceSet.getByName(name).java.srcDirs.mapNotNullTo(javaDirs) {
                if (it.exists()) it else null
            }
        }
    }
    return javaDirs
}

fun Project.findLayoutDirs(variantName: String) = findXmlDirs(variantName, "layout")
fun Project.findXmlDirs(variantName: String, vararg dirName: String): ArrayList<File> {
    return resDirs(variantName).flatMapTo(ArrayList()) { dir ->
        dir.listFiles { file, name ->
            //过滤res目录下xxx目录
            file.isDirectory && dirName.any { name.startsWith(it) }
        }?.toList() ?: emptyList()
    }
}

//返回res目录,可能有多个
fun Project.resDirs(variantName: String): List<File> {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    val nameSet = mutableSetOf<String>()
    nameSet.add("main")
    if (isAndroidProject()) {
        nameSet.addAll(variantName.splitWords())
    }
    val resDirs = mutableListOf<File>()
    sourceSet.names.forEach { name ->
        if (nameSet.contains(name)) {
            sourceSet.getByName(name).res.srcDirs.mapNotNullTo(resDirs) {
                if (it.exists()) it else null
            }
        }
    }
    return resDirs
}

//返回manifest文件目录,有且仅有一个
fun Project.manifestFile(): File {
    val sourceSet = (extensions.getByName("android") as BaseExtension).sourceSets
    return sourceSet.getByName("main").manifest.srcFile
}


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
fun Project.findLocationProject(dir: String, variantName: String): Project? {
    val packageName = dir.replace(".", File.separator)
    val javaDirs = javaDirs(variantName)
    if (javaDirs.any { File(it, packageName).exists() }) {
        return this
    }
    val dependencyProjects = mutableListOf<Project>()
    findDependencyAndroidProject(dependencyProjects)
    dependencyProjects.forEach {
        val project = it.findLocationProject(dir, variantName)
        if (project != null) return project
    }
    return null
}

fun findClassByLayoutXml(text: String, packageName: String): List<ClassInfo> {
    val classInfoList = mutableListOf<ClassInfo>()
    val childrenList = XmlParser(false, false).parseText(text).breadthFirst()
    val destAttributes =
        mutableListOf("tools:context", "app:layout_behavior", "app:layoutManager", "android:name")
    for (children in childrenList) {
        val childNode = children as? Node ?: continue
        destAttributes.forEach { attributeName ->
            val attributeValue = childNode.attribute(attributeName)?.toString()
            if (!attributeValue.isNullOrBlank()) {
                val classname =
                    if (attributeValue.startsWith(".")) "$packageName$attributeValue" else attributeValue
                classInfoList.add(ClassInfo(classname))
            }
        }
        val nodeName = childNode.name().toString()
        if (nodeName !in whiteList) {
            if (nodeName == "variable" || nodeName == "import") {
                val typeValue = childNode.attribute("type").toString()
                classInfoList.add(ClassInfo(typeValue, fromImportNode = nodeName == "import"))
            } else {
                classInfoList.add(ClassInfo(nodeName))
            }
        }
    }
    return classInfoList
}

fun findFragmentInfoList(text: String): List<ClassInfo> {
    val classInfoList = mutableListOf<ClassInfo>()
    val rootNode = XmlParser(false, false).parseText(text)
    for (children in rootNode.children()) {
        val childNode = children as? Node ?: continue
        val childName = childNode.name()
        if ("fragment" == childName) {
            val classPath = childNode.attribute("android:name").toString()
            classInfoList.add(ClassInfo(classPath, childNode.children().isNotEmpty()))
        }
    }
    return classInfoList
}

//在manifest文件里，查找四大组件及Application，返回文件的package属性，即包名
fun findClassByManifest(text: String, packageName: String): List<ClassInfo> {
    val classInfoList = mutableListOf<ClassInfo>()
    val rootNode = XmlParser(false, false).parseText(text)
    val nodeList = rootNode.get("application") as? NodeList ?: return classInfoList
    val applicationNode = nodeList.firstOrNull() as? Node ?: return classInfoList
    val application = applicationNode.attribute("android:name")?.toString()
    if (application != null) {
        val classPath = if (application.startsWith(".")) "$packageName$application" else application
        classInfoList.add(ClassInfo(classPath))
    }
    for (children in applicationNode.children()) {
        val childNode = children as? Node ?: continue
        val childName = childNode.name()
        if ("activity" == childName || "service" == childName ||
            "receiver" == childName || "provider" == childName
        ) {
            val name = childNode.attribute("android:name").toString()
            val classPath = if (name.startsWith(".")) "$packageName$name" else name
            classInfoList.add(ClassInfo(classPath))
        }
    }
    return classInfoList
}