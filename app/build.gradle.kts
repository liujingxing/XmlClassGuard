import com.android.build.api.dsl.ApplicationExtension
import com.xml.guard.entensions.GuardExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.xmlclassguard)
}
// apply(from = "aabresguard.gradle.kts")

extensions.configure<GuardExtension> {
    // Enable resource guard integrations only when the corresponding plugin is applied.
    findAabConstraintReferencedIds = false
    findAndConstraintReferencedIds = false
    mappingFile = file("xml-class-mapping.txt")
    packageChange = hashMapOf(
        "com.ljx.example" to "ab.cd",
        "com.ljx.base" to "ggg.kkk",
        "com.ljx.kts" to "ff.dd"
    )
    moveDir = hashMapOf(
        "com.ljx.example" to "ef.gh",
        "com.ljx.base" to "zzzz.hhhh",
        "com.ljx.kts" to "ddd.xxx"
    )
}

extensions.configure<ApplicationExtension> {
    namespace = "com.ljx.example"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ljx.example"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets {
        named("main") { kotlin.directories.add("src/main/kotlin") }
    }

    buildFeatures {
        dataBinding = true
        aidl = true
    }
}

dependencies {

    implementation(projects.base)
    implementation(projects.ktsTest)
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.espresso)
}
