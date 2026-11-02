require 'fileutils'
require 'tmpdir'
require 'open3'

repo = File.expand_path('../../../../..', __dir__)
jar = File.join(repo, 'plugin/build/libs/plugin.jar')
raise 'Run :plugin:jar first' unless File.file?(jar)
cases = %w[normal missing-and missing-aab obfuscate move package]
cases.each do |scenario|
  Dir.mktmpdir("xmlguard-#{scenario}-") do |dir|
    Dir.children(__dir__).reject { |name| ['build', '.gradle', 'run-regression.rb'].include?(name) }.each do |name|
      FileUtils.cp_r(File.join(__dir__, name), dir)
    end
    second = File.join(dir, 'second/build.gradle')
    manifest = File.join(dir, 'second/custom/main.xml')
    extra = ''
    task = 'verifyManifestSources'
    case scenario
    when 'normal'
      task = 'verifyManifestSources verifyResourceTaskDependencies'
    when 'missing-and', 'missing-aab'
      option = scenario == 'missing-and' ? 'findAndConstraintReferencedIds' : 'findAabConstraintReferencedIds'
      extra = "\nxmlClassGuard { #{option} = true }\n"
      task = 'help'
    else
      manifest = File.join(dir, 'second/custom/xml/main.xml')
      FileUtils.mkdir_p(File.dirname(manifest))
      File.write(manifest, '<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application android:name=".FixtureApplication" /></manifest>')
      path = File.join(dir, 'second/src/main/kotlin/fixture/second')
      FileUtils.mkdir_p(path)
      File.write(File.join(path, 'FixtureApplication.kt'), "package fixture.second\nclass FixtureApplication : android.app.Application()\n")
      task = { 'obfuscate' => ':second:xmlClassGuardDebug', 'move' => ':second:moveDirDebug', 'package' => ':second:packageChangeDebug' }.fetch(scenario)
      extra = "\nandroid { namespace = 'fixture.second'; sourceSets.main.manifest.srcFile('custom/xml/main.xml') }\n"
      extra += "xmlClassGuard { moveDir = ['fixture.second': 'moved.second'] }\n" if scenario == 'move'
      extra += "xmlClassGuard { packageChange = ['fixture.second': 'fixture.updated'] }\n" if scenario == 'package'
    end
    File.open(second, 'a') { |file| file.write(extra) }
    command = [File.join(repo, 'gradlew'), '-p', dir, *task.split, "-PguardPluginJar=#{jar}", '--offline', '--no-daemon']
    output, status = Open3.capture2e(*command)
    File.write(File.join("/tmp", "xmlguard-#{scenario}.log"), output)
    if scenario.start_with?('missing-')
      expected = scenario == 'missing-and' ? 'resguardDebug' : 'aabresguardDebug'
      raise "#{scenario}: missing validation" if status.success? || !output.include?("Resource guard task #{expected} missing")
    else
      raise "#{scenario}: Gradle failed, see /tmp/xmlguard-#{scenario}.log" unless status.success?
      text = File.read(manifest)
      case scenario
      when 'obfuscate'
        raise 'Manifest class not obfuscated' if text.include?('.FixtureApplication')
        raise 'Missing mapping' unless File.read(File.join(dir, 'second/xml-class-mapping.txt')).include?('fixture.second.FixtureApplication')
      when 'move'
        raise 'Manifest not rewritten' unless text.include?('moved.second.FixtureApplication')
        raise 'Source not moved' unless File.file?(File.join(dir, 'second/src/main/kotlin/moved/second/FixtureApplication.kt'))
      when 'package'
        raise 'Relative manifest class not expanded' unless text.include?('fixture.second.FixtureApplication')
        raise 'Namespace not rewritten' unless File.read(second).include?('namespace = "fixture.updated"')
      end
    end
    if ENV['GUARD_VERIFY_ASSEMBLE'] == 'true' && %w[obfuscate move package].include?(scenario)
      output, status = Open3.capture2e(File.join(repo, 'gradlew'), '-p', dir,
        ':second:assembleDebug', "-PguardPluginJar=#{jar}", '--offline', '--no-daemon')
      File.write(File.join("/tmp", "xmlguard-#{scenario}-assemble.log"), output)
      raise "#{scenario}: transformed application failed to build" unless status.success?
    end
    puts "PASS #{scenario}"

    STDOUT.flush
  end
end
