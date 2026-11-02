pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        if (providers.gradleProperty("useMavenLocal").orNull == "true") {
            mavenLocal()
        }
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo") }
        maven { url = uri("https://raw.githubusercontent.com/iQuick/maven/master/") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://jcenter.bintray.com") }
    }
}
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "XmlClassGuard"
include(":app")
include(":plugin")
include(":base")
include(":kts_test")

