require 'tmpdir'
require 'fileutils'
require 'open3'
agp = ARGV.fetch(0)
gradle = ARGV.fetch(1)
Dir.mktmpdir('xmlguard-compat-') do |dir|
  File.write(File.join(dir, 'settings.gradle.kts'), <<~KTS)
    pluginManagement {
        repositories { maven("https://maven.aliyun.com/repository/google"); maven("https://maven.aliyun.com/repository/central"); google(); mavenCentral(); gradlePluginPortal(); maven("https://jitpack.io") }
        resolutionStrategy { eachPlugin {
            when (requested.id.id) {
                "xml-class-guard" -> useModule("com.github.liujingxing:XmlClassGuard:1.2.8-beta1")
                "com.android.application", "com.android.library" -> useModule("com.android.tools.build:gradle:#{agp}")
            }
        } }
    }
    dependencyResolutionManagement { repositories { maven("https://maven.aliyun.com/repository/google"); maven("https://maven.aliyun.com/repository/central"); google(); mavenCentral() } }
    rootProject.name = "compatibility"
    include(":app", ":lib")
  KTS
  File.write(File.join(dir, 'build.gradle.kts'), <<~KTS)
    plugins {
        id("com.android.application") version "#{agp}" apply false
        id("com.android.library") version "#{agp}" apply false
        id("xml-class-guard") version "1.2.8-beta1" apply false
    }
  KTS
  FileUtils.mkdir_p(File.join(dir, 'app/src/main/java/sample/app'))
  FileUtils.mkdir_p(File.join(dir, 'lib/src/main/java/sample/lib'))
  File.write(File.join(dir, 'app/build.gradle.kts'), <<~KTS)
    import com.xml.guard.entensions.GuardExtension
    plugins { id("com.android.application"); id("xml-class-guard") }
    android {
        namespace = "sample.app"
        compileSdk = 34
        buildToolsVersion = "35.0.0"
        defaultConfig { applicationId = "sample.app"; minSdk = 21; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    }
    dependencies { implementation(project(":lib")) }
    extensions.configure<GuardExtension> {
        moveDir = hashMapOf("sample.app" to "moved.app")
        packageChange = hashMapOf("sample.app" to "updated.app")
    }
    tasks.register("verifySourceScan") {
        doLast {
            val utils = plugins.getPlugin("xml-class-guard").javaClass.classLoader.loadClass("com.xml.guard.utils.ProjectExtKt")
            val sources = utils.getMethod("javaDirs", Project::class.java, String::class.java).invoke(null, project, "Debug") as List<*>
            check(sources.isNotEmpty())
            val modules = mutableListOf<Project>()
            utils.getMethod("findDependencyAndroidProject", Project::class.java, MutableList::class.java, List::class.java)
                .invoke(null, project, modules, listOf("api", "implementation"))
            check(modules.map { it.path } == listOf(":lib"))
        }
    }
  KTS
  File.write(File.join(dir, 'lib/build.gradle.kts'), 'plugins { id("com.android.library") }; android { namespace = "sample.lib"; compileSdk = 34; buildToolsVersion = "35.0.0"; defaultConfig { minSdk = 21 } }')
  File.write(File.join(dir, 'app/src/main/AndroidManifest.xml'), '<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application android:name="sample.app.DemoApplication" /></manifest>')
  File.write(File.join(dir, 'lib/src/main/AndroidManifest.xml'), '<manifest />')
  File.write(File.join(dir, 'app/src/main/java/sample/app/DemoApplication.java'), 'package sample.app; public class DemoApplication extends android.app.Application {}')
  File.write(File.join(dir, 'lib/src/main/java/sample/lib/Model.java'), 'package sample.lib; public class Model {}')
  command = [gradle, '-p', dir, '--no-daemon', '--stacktrace', '--info', '-Dorg.gradle.internal.http.connectionTimeout=15000', '-Dorg.gradle.internal.http.socketTimeout=15000']
  command << '--offline' unless ENV['COMPAT_ONLINE'] == 'true'
  [ ['help', ':app:verifySourceScan'], [':app:xmlClassGuardDebug'], [':app:assembleDebug'] ].each_with_index do |tasks, index|
    out, status = Open3.capture2e(*command, *tasks)
    log = "/tmp/xmlguard-compat-#{agp}-#{File.basename(File.dirname(File.dirname(gradle)))}-#{index}.log"
    File.write(log, out)
    puts "AGP #{agp} #{tasks.join(' ')}: #{status.success? ? 'PASS' : 'FAIL'} #{log}"
    STDOUT.flush
    exit 1 unless status.success?
    if index == 1
      manifest = File.read(File.join(dir, 'app/src/main/AndroidManifest.xml'))
      raise 'Manifest class was not obfuscated' if manifest.include?('sample.app.DemoApplication')
      raise 'Class mapping missing' unless File.read(File.join(dir, 'app/xml-class-mapping.txt')).include?('DemoApplication')
    end
  end
end
