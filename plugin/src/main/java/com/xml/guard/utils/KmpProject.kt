package com.xml.guard.utils

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import java.io.File

internal const val KMP_ANDROID_PLUGIN = "com.android.kotlin.multiplatform.library"

// Keep Kotlin Gradle API references in this file so ordinary Android consumers need no KMP plugin.
internal fun Project.kmpAndroidTarget(): KotlinMultiplatformAndroidLibraryTarget =
    extensions.getByType(KotlinMultiplatformExtension::class.java).targets
        .filterIsInstance<KotlinMultiplatformAndroidLibraryTarget>().single()

internal fun Project.kmpAndroidSourceSets(variantName: String): Set<KotlinSourceSet> {
    val kotlin = extensions.getByType(KotlinMultiplatformExtension::class.java)
    return kotlin.targets.filter { it.platformType == KotlinPlatformType.androidJvm }
        .flatMap { target ->
            target.compilations.filter {
                it.name == "main" || it.name.equals(variantName, ignoreCase = true)
            }.flatMap { it.allKotlinSourceSets }
        }.toSet()
}

internal fun Project.kmpJavaDirs(variantName: String): List<File> =
    (kmpAndroidSourceSets(variantName).flatMap { it.kotlin.srcDirs } +
        kmpAndroidSourceSets(variantName).map { file("src/${it.name}/java") }).filter { it.isDirectory }.distinct()

internal fun Project.kmpResourceDirs(): List<File> {
    if (!kmpAndroidTarget().androidResources.enable) return emptyList()
    val sourceSet = kmpAndroidTarget().compilations.getByName("main").defaultSourceSet
    return listOf(file("src/${sourceSet.name}/res")).filter { it.isDirectory }
}

internal fun Project.kmpManifestFile(): File {
    val sourceSet = kmpAndroidTarget().compilations.getByName("main").defaultSourceSet
    return file("src/${sourceSet.name}/AndroidManifest.xml")
}

internal fun Project.kmpDependencyConfigurations(names: List<String>): List<String> =
    kmpAndroidSourceSets("main").flatMap { sourceSet ->
        names.mapNotNull {
            when (it) {
                "api" -> sourceSet.apiConfigurationName
                "implementation" -> sourceSet.implementationConfigurationName
                else -> null
            }
        }
    }.distinct()

internal fun Project.kmpReferenceDirs(): List<File> {
    val sourceSets = extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
    return (sourceSets.flatMap { it.kotlin.srcDirs } +
        sourceSets.map { file("src/${it.name}/java") }).filter { it.isDirectory }.distinct()
}
