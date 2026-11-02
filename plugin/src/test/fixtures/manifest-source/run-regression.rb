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
      activity = File.join(dir, 'second/src/main/kotlin/fixture/second/shortcuts/ShortcutOnlyActivity.kt')
      FileUtils.mkdir_p(File.dirname(activity))
      File.write(activity, "package fixture.second.shortcuts\nclass ShortcutOnlyActivity : android.app.Activity()\n")
      %w[xml xml-v25].each do |folder|
        res = File.join(dir, 'second/src/main/res', folder)
        FileUtils.mkdir_p(res)
        File.write(File.join(res, 'shortcuts.xml'), <<~XML)
          <shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
            <shortcut android:shortcutId="launch" android:shortcutShortLabel="@string/shortcut_label">
              <intent android:action="android.intent.action.VIEW" android:targetPackage="fixture.second" android:targetClass="fixture.second.shortcuts.ShortcutOnlyActivity" />
              <intent android:action="android.intent.action.VIEW" android:targetPackage="external.app" android:targetClass="external.app.ExternalActivity" />
            </shortcut>
          </shortcuts>
        XML
      end
      values = File.join(dir, 'second/src/main/res/values')
      FileUtils.mkdir_p(values)
      File.write(File.join(values, 'strings.xml'), '<resources><string name="shortcut_label">Launch</string></resources>')
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
    unless scenario.start_with?('missing-') || scenario == 'normal'
      %w[xml xml-v25].each do |folder|
        shortcut = File.read(File.join(dir, 'second/src/main/res', folder, 'shortcuts.xml'))
        raise 'Application ID changed' unless shortcut.include?('android:targetPackage="fixture.second"')
        raise 'External intent changed' unless shortcut.include?('android:targetPackage="external.app" android:targetClass="external.app.ExternalActivity"')
        case scenario
        when 'obfuscate'
          mapping = File.read(File.join(dir, 'second/xml-class-mapping.txt'))
          match = mapping.match(/fixture\.second\.shortcuts\.ShortcutOnlyActivity -> (\S+)/)
          raise 'Shortcut-only activity not discovered' unless match
          raise 'Shortcut target class not updated' unless shortcut.include?("android:targetClass=\"#{match[1]}\"")
        when 'move'
          raise 'Shortcut class package not moved' unless shortcut.include?('android:targetClass="moved.second.shortcuts.ShortcutOnlyActivity"')
        when 'package'
          raise 'Shortcut target changed with namespace' unless shortcut.include?('android:targetClass="fixture.second.shortcuts.ShortcutOnlyActivity"')
        end
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
