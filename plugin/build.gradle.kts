import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

val jdkVersion = providers.gradleProperty("jdk_version").get()
val pluginVersion = providers.gradleProperty("plugin_version").get()

val copyJavaTemplates by tasks.registering(Copy::class) {
    from("src/main/java-templates")
    into(layout.buildDirectory.dir("generated/sources/java-templates/java/main"))
    expand("projectVersion" to pluginVersion)
    filteringCharset = "UTF-8"
}

sourceSets.named("main") {
    java.srcDir(layout.buildDirectory.dir("generated/sources/java-templates/java/main"))
}

tasks.named("compileKotlin") {
    dependsOn(copyJavaTemplates)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(jdkVersion))
        freeCompilerArgs.add("-Xallow-result-return-type")
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(jdkVersion)
    targetCompatibility = JavaVersion.toVersion(jdkVersion)
}

dependencies {
    compileOnly(gradleApi())
    // Match the Groovy runtime bundled with the consuming Gradle distribution.
    compileOnly(localGroovy())
    compileOnly(libs.asm)
    compileOnly(libs.agp.api)
    compileOnly(libs.aabresguard)
    compileOnly(libs.andresguard)
    testImplementation(libs.junit)
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            groupId = "com.github.liujingxing"
            artifactId = "XmlClassGuard"
            version = pluginVersion
        }
    }
}
