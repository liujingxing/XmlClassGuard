require 'tmpdir'
require 'fileutils'
require 'open3'

agp, gradle = ARGV
abort 'Usage: ruby kmp-probe.rb AGP_VERSION GRADLE_EXECUTABLE' unless agp && gradle
distribution = File.basename(File.dirname(File.dirname(gradle)))
fixture = File.expand_path('../kmp-source', __dir__)
repositories = 'maven { url "https://maven.aliyun.com/repository/google" }; maven { url "https://maven.aliyun.com/repository/central" }; google(); mavenCentral(); maven { url "https://jitpack.io" }'
%w[normal obfuscate move package].each do |scenario|
  Dir.mktmpdir('xmlguard-kmp-compat-') do |dir|
    Dir.children(fixture).reject { |name| %w[build .gradle run-regression.rb].include?(name) }.each do |name|
      FileUtils.cp_r(File.join(fixture, name), dir)
    end
    Dir.glob(File.join(dir, '**/*.gradle')).each do |path|
      source = File.read(path)
        .gsub('google(); mavenCentral()', repositories)
        .gsub('com.android.tools.build:gradle:9.5.0', "com.android.tools.build:gradle:#{agp}")
        .gsub("classpath files(providers.gradleProperty('guardPluginJar').get())", "classpath 'com.github.liujingxing:XmlClassGuard:1.2.8-beta1'")
        .gsub('compileSdk = 37', 'compileSdk = 35')
        .gsub('targetSdk = 37', 'targetSdk = 35')
      source = source.gsub(/android \{/, 'androidLibrary {') if (path.include?('/shared/') || path.include?('/transitive/')) && Gem::Version.new(agp) < Gem::Version.new('8.12.0')
      source = source.sub('assert deps*.path', 'println("KMP target diagnostics: " + shared.kotlin.targets.collect { t -> [t.name, t.platformType, t.compilations.collect { c -> [c.name, c.allKotlinSourceSets*.name] }] }); assert deps*.path') if path == File.join(dir, 'build.gradle')
      # AGP 8.10 has no resource opt-in DSL. Let the plugin's own scan expose
      # the missing API rather than failing while evaluating the test script.
      source = source.gsub('androidResources.enable = true', '') if agp.start_with?('8.10.')
      source = source.gsub('compileSdk = 35', 'compileSdk = 35; buildToolsVersion = "35.0.0"') if path.include?('/app/')
      File.write(path, source)
    end
    File.write(File.join(dir, 'gradle.properties'), "org.gradle.jvmargs=-Xmx2g\n")
    if scenario == 'obfuscate'
      File.write(File.join(dir, 'app/xml-class-mapping.txt'), "dir mapping:\n    sample.common -> q.common\n\nclass mapping:\n")
    end
    tasks = {
      'normal' => 'verifyKmpSources', 'obfuscate' => ':app:xmlClassGuardDebug',
      'move' => ':app:moveDirDebug', 'package' => ':app:packageChangeDebug'
    }
    command = [gradle, '-p', dir, '--no-daemon', '--stacktrace', '-Dorg.gradle.internal.http.connectionTimeout=15000', '-Dorg.gradle.internal.http.socketTimeout=15000']
    command << '--offline' unless ENV['COMPAT_ONLINE'] == 'true'
    [[tasks.fetch(scenario)], %w[:app:assembleDebug :shared:compileKotlinJvm :shared:compileTestKotlinJvm]].each_with_index do |targets, index|
      output, status = Open3.capture2e(*command, *targets)
      log = "/tmp/xmlguard-kmp-compat-#{agp}-#{distribution}-#{scenario}-#{index}.log"
      File.write(log, output)
      puts "AGP #{agp} KMP #{scenario} #{targets.join(' ')}: #{status.success? ? 'PASS' : 'FAIL'} #{log}"
      STDOUT.flush
      exit 1 unless status.success?
      next unless index == 0
      shared = File.join(dir, 'shared')
      reference = File.read(File.join(shared, 'src/jvmMain/kotlin/sample/jvm/JvmReference.kt'))
      layout = File.read(File.join(shared, 'src/androidMain/res/layout/shared_view.xml'))
      case scenario
      when 'obfuscate'
        raise 'View XML not rewritten' if layout.include?('sample.shared.AndroidView')
        raise 'JVM reference stale' if reference.include?('sample.common.SharedModel')
      when 'move'
        raise 'View XML not moved' unless layout.include?('moved.shared.AndroidView')
        raise 'JVM reference stale' unless reference.include?('moved.common.SharedModel')
      when 'package'
        raise 'KMP namespace not changed' unless File.read(File.join(shared, 'build.gradle')).include?('namespace = "updated.shared"')
      end
    end
  end
end
