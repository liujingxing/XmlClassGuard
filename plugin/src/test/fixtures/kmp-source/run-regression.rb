require 'fileutils'
require 'tmpdir'
require 'open3'
repo = File.expand_path('../../../../..', __dir__)
jar = File.join(repo, 'plugin/build/libs/plugin.jar')
%w[normal obfuscate move package].each do |scenario|
  Dir.mktmpdir("xmlguard-kmp-#{scenario}-") do |dir|
    Dir.children(__dir__).reject { |n| ['build', '.gradle', 'run-regression.rb'].include?(n) }.each do |n|
      FileUtils.cp_r(File.join(__dir__, n), dir)
    end
    tasks = ['verifyKmpSources']
    if scenario == 'obfuscate'
      File.write(File.join(dir, 'app/xml-class-mapping.txt'), "dir mapping:\n    sample.common -> q.common\n\nclass mapping:\n")
    end
    tasks = [ { 'obfuscate' => ':app:xmlClassGuardDebug', 'move' => ':app:moveDirDebug', 'package' => ':app:packageChangeDebug' }.fetch(scenario)] unless scenario == 'normal'
    command = [File.join(repo, 'gradlew'), '-p', dir, "-PguardPluginJar=#{jar}", '--offline', '--no-daemon']
    output, status = Open3.capture2e(*command, *tasks)
    File.write("/tmp/xmlguard-kmp-#{scenario}.log", output)
    raise "#{scenario} failed" unless status.success?
    shared = File.join(dir, 'shared')
    reference = File.read(File.join(shared, 'src/jvmMain/kotlin/sample/jvm/JvmReference.kt'))
    layout = File.read(File.join(shared, 'src/androidMain/res/layout/shared_view.xml'))
    case scenario
    when 'obfuscate'
      raise 'View XML not rewritten' if layout.include?('sample.shared.AndroidView')
      raise 'JVM reference stale after common class obfuscation' if reference.include?('sample.common.SharedModel')
    when 'move'
      raise 'View XML not moved' unless layout.include?('moved.shared.AndroidView')
      raise 'JVM reference stale after moving common source' unless reference.include?('moved.common.SharedModel')
    when 'package'
      raise 'KMP namespace not changed' unless File.read(File.join(shared, 'build.gradle')).include?('namespace = "updated.shared"')
    end
    output, status = Open3.capture2e(*command, ':app:assembleDebug', ':shared:compileKotlinJvm', ':shared:compileTestKotlinJvm')
    File.write("/tmp/xmlguard-kmp-#{scenario}-assemble.log", output)
    raise "#{scenario} compilation failed" unless status.success?
    puts "PASS KMP #{scenario}, Android APK, JVM production and test compilation"
    STDOUT.flush
  end
end
