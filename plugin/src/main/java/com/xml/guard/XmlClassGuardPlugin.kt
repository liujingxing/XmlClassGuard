package com.xml.guard

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
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
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import kotlin.reflect.KClass

/**
 * User: ljx
 * Date: 2022/2/25
 * Time: 19:03
 */
class XmlClassGuardPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        checkApplicationPlugin(project)
        println("XmlClassGuard version is $version, agpVersion=${AgpVersion.agpVersion}")
        val guardExt = project.extensions.create("xmlClassGuard", GuardExtension::class.java)

        val androidComponents = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
        androidComponents.onVariants(androidComponents.selector().all()) { variant ->
            project.createTasks(guardExt, variant)
        }
    }

    private fun Project.createTasks(guardExt: GuardExtension, variant: ApplicationVariant) {
        val variantName = variant.name.replaceFirstChar { it.uppercaseChar() }
        createTask("xmlClassGuard$variantName", XmlClassGuardTask::class, guardExt, variantName)
        createTask("packageChange$variantName", PackageChangeTask::class, guardExt, variantName)
        createTask("moveDir$variantName", MoveDirTask::class, guardExt, variantName)
        if (guardExt.findAndConstraintReferencedIds) {
            createAndFindConstraintReferencedIdsTask(variantName)
        }
        if (guardExt.findAabConstraintReferencedIds) {
            createAabFindConstraintReferencedIdsTask(variantName)
        }
    }

    private fun Project.createAndFindConstraintReferencedIdsTask(variantName: String) {
        val andResGuardTaskName = "resguard$variantName"
        val taskName = "andFindConstraintReferencedIds$variantName"
        val task =
            createTask(taskName, FindConstraintReferencedIdsTask::class, andResGuard, variantName)
        tasks.configureEach { externalTask ->
            if (externalTask.name == andResGuardTaskName) externalTask.dependsOn(task)
        }
        gradle.projectsEvaluated {
            if (andResGuardTaskName !in tasks.names) {
                throw GradleException("Resource guard task $andResGuardTaskName missing in $path; apply the corresponding resource plugin before XmlClassGuard")
            }
        }
    }

    private fun Project.createAabFindConstraintReferencedIdsTask(variantName: String) {
        val aabResGuardTaskName = "aabresguard$variantName"
        val taskName = "aabFindConstraintReferencedIds$variantName"
        val task =
            createTask(taskName, FindConstraintReferencedIdsTask::class, aabResGuard, variantName)
        tasks.configureEach { externalTask ->
            if (externalTask.name == aabResGuardTaskName) externalTask.dependsOn(task)
        }
        gradle.projectsEvaluated {
            if (aabResGuardTaskName !in tasks.names) {
                throw GradleException("Resource guard task $aabResGuardTaskName missing in $path; apply the corresponding resource plugin before XmlClassGuard")
            }
        }
    }

    private fun checkApplicationPlugin(project: Project) {
        if (!project.plugins.hasPlugin("com.android.application")) {
            throw GradleException("Android Application plugin required")
        }
    }

    private fun <T : Task> Project.createTask(
        taskName: String,
        taskClass: KClass<T>,
        vararg params: Any
    ): TaskProvider<T> = if (taskName in tasks.names) {
        tasks.named(taskName, taskClass.java)
    } else {
        tasks.register(taskName, taskClass.java, *params)
    }
}
