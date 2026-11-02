require 'fileutils'
require 'tmpdir'
require 'open3'

repo = File.expand_path('../../../../..', __dir__)
jar = File.join(repo, 'plugin/build/libs/plugin.jar')
raise 'Run :plugin:jar first' unless File.file?(jar)
repositories = "repositories { google(); mavenCentral(); maven { url 'https://raw.githubusercontent.com/martinloren/AabResGuard/mvn-repo' }; maven { url 'https://raw.githubusercontent.com/iQuick/maven/master/' }; maven { url 'https://maven.aliyun.com/repository/public' } }"
plugins = {
  'and' => ['com.tencent.mm:AndResGuard-gradle-plugin:1.2.21', 'AndResGuard'],
  'aab' => ['com.bytedance.android:aabresguard-plugin:0.1.10', 'com.bytedance.android.aabResGuard']
}
plugins.each do |name, (coordinate, id)|
  next if ENV['GUARD_RESOURCE_PLUGIN'] && ENV['GUARD_RESOURCE_PLUGIN'] != name
  %w[new-dsl legacy-dsl].each do |mode|
    next if ENV['GUARD_RESOURCE_MODE'] && ENV['GUARD_RESOURCE_MODE'] != mode
    Dir.mktmpdir("xmlguard-#{name}-#{mode}-") do |dir|
      Dir.children(__dir__).reject { |n| ['build', '.gradle', 'run-regression.rb', 'run-resource-compatibility.rb'].include?(n) }.each do |n|
        FileUtils.cp_r(File.join(__dir__, n), dir)
      end
      root = File.join(dir, 'build.gradle')
      content = File.read(root).sub('repositories { google(); mavenCentral() }', repositories)
      content = content.sub("classpath 'com.android.tools.build:gradle:9.5.0'", "classpath 'com.android.tools.build:gradle:9.5.0'\n        classpath '#{coordinate}'")
      File.write(root, content)
      if mode == 'legacy-dsl'
        File.write(File.join(dir, 'gradle.properties'), "android.newDsl=false\nandroid.builtInKotlin=false\n")
      end
      File.write(File.join(dir, 'first/build.gradle'), <<~GRADLE)
        evaluationDependsOn(':lib')
        apply plugin: 'com.android.application'
        android {
            namespace = 'fixture.first'
            compileSdk = 37
            defaultConfig { minSdk = 24; targetSdk = 37; versionCode = 1; versionName = "1.0" }
            sourceSets.main.manifest.srcFile('custom/main.xml')
        }
        apply plugin: '#{id}'
        apply plugin: 'xml-class-guard'
      GRADLE
      if name == 'aab'
        File.open(File.join(dir, 'first/build.gradle'), 'a') { |f| f.puts("aabResGuard { obfuscatedBundleFileName = 'fixture.aab' }") }
      end
      out, status = Open3.capture2e(File.join(repo, 'gradlew'), '-p', dir, 'help',
        "-PguardPluginJar=#{jar}", '--offline', '--no-daemon')
      log = "/tmp/xmlguard-real-#{name}-#{mode}.log"
      File.write(log, out)
      puts "#{name} #{mode}: #{status.success? ? 'PASS configuration' : 'FAIL configuration'}; #{log}"
      STDOUT.flush
      if status.success?
        packagingTask = name == 'and' ? ':first:resguardDebug' : ':first:bundleDebug'
        output, packaging = Open3.capture2e(File.join(repo, 'gradlew'), '-p', dir, packagingTask,
          "-PguardPluginJar=#{jar}", '--offline', '--no-daemon')
        packagingLog = "/tmp/xmlguard-real-#{name}-#{mode}-packaging.log"
        File.write(packagingLog, output)
        puts "#{name} #{mode}: #{packaging.success? ? 'PASS packaging' : 'FAIL packaging'}; #{packagingLog}"
        STDOUT.flush
      end

    end
  end
end
