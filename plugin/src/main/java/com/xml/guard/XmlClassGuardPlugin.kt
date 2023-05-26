package com.xml.guard

import com.android.build.gradle.AppExtension
import com.xml.guard.entensions.GuardExtension
import com.xml.guard.model.aabResGuard
import com.xml.guard.model.andResGuard
import com.xml.guard.tasks.FindConstraintReferencedIdsTask
import com.xml.guard.tasks.MoveDirTask
import com.xml.guard.tasks.PackageChangeTask
import com.xml.guard.tasks.XmlClassGuardTask
import com.xml.guard.utils.AgpVersion
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:03
 */
class XmlClassGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        println("XmlClassGuard version is $version, agpVersion=${AgpVersion.agpVersion}")
        val guardExtension = project.extensions.create("xmlClassGuard", GuardExtension::class.java)
        project.tasks.create("xmlClassGuard", XmlClassGuardTask::class.java, guardExtension)
        project.tasks.create("packageChange", PackageChangeTask::class.java, guardExtension)
        project.tasks.create("moveDir", MoveDirTask::class.java, guardExtension)

        val android = project.extensions.getByName("android") as AppExtension
        project.afterEvaluate {
            android.applicationVariants.all { variant ->
                val variantName = variant.name.capitalize()

                val minifyTaskName = "minify${variant.name.capitalize()}WithR8"

                if (project.tasks.findByName(minifyTaskName) == null) {
                    return@all
                }

                if (guardExtension.findAndConstraintReferencedIds) {
                    createAndFindConstraintReferencedIds(project, variantName)
                }
                if (guardExtension.findAabConstraintReferencedIds) {
                    createAabFindConstraintReferencedIds(project, variantName)
                }
            }
        }
    }

    private fun createAndFindConstraintReferencedIds(
        project: Project,
        variantName: String
    ) {
        val andResGuardTaskName = "resguard$variantName"
        val andResGuardTask = project.tasks.findByName(andResGuardTaskName)
            ?: throw GradleException("AndResGuard plugin required")
        val findConstraintReferencedIdsTaskName = "andFind${variantName}ConstraintReferencedIds"
        val findConstraintReferencedIdsTask =
            project.tasks.findByName(findConstraintReferencedIdsTaskName)
                ?: project.tasks.create(
                    findConstraintReferencedIdsTaskName,
                    FindConstraintReferencedIdsTask::class.java,
                    andResGuard,
                    variantName
                )
        andResGuardTask.dependsOn(findConstraintReferencedIdsTask)
    }

    private fun createAabFindConstraintReferencedIds(
        project: Project,
        variantName: String
    ) {
        val aabResGuardTaskName = "aabresguard$variantName"
        val aabResGuardTask = project.tasks.findByName(aabResGuardTaskName)
            ?: throw GradleException("AabResGuard plugin required")
        val findConstraintReferencedIdsTaskName = "aabFindConstraintReferencedIds"
        val findConstraintReferencedIdsTask =
            project.tasks.findByName(findConstraintReferencedIdsTaskName)
                ?: project.tasks.create(
                    findConstraintReferencedIdsTaskName,
                    FindConstraintReferencedIdsTask::class.java,
                    aabResGuard,
                    variantName
                )
        aabResGuardTask.dependsOn(findConstraintReferencedIdsTask)
    }

    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Android Application plugin required")
        }
    }
}