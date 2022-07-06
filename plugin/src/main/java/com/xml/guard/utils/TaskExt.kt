package com.xml.guard.utils

import org.gradle.api.Project
import org.gradle.api.Task

/**
 * User: ljx
 * Date: 2022/7/6
 * Time: 22:49
 */
//返回主module依赖的所有Android子module，包含间接依赖的
fun Task.allDependencyAndroidProjects(): List<Project> {
    val dependencyProjects = mutableListOf<Project>()
    project.findDependencyAndroidProject(dependencyProjects)
    val androidProjects = mutableListOf<Project>()
    androidProjects.add(project)
    androidProjects.addAll(dependencyProjects)
    return androidProjects
}