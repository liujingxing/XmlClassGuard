package com.xml.guard

import com.xml.guard.entensions.GuardExtension
import com.xml.guard.tasks.MoveDirTask
import com.xml.guard.tasks.PackageChangeTask
import com.xml.guard.tasks.XmlClassGuardTask
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:03
 */
class XmlClassGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val guardExtension = project.extensions.create("xmlClassGuard", GuardExtension::class.java)
        project.tasks.create("xmlClassGuard", XmlClassGuardTask::class.java, guardExtension)
        project.tasks.create("packageChange", PackageChangeTask::class.java, guardExtension)
        project.tasks.create("moveDir", MoveDirTask::class.java, guardExtension)
    }
}