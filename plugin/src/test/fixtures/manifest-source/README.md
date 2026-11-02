# AGP 9.5.0 回归工程

先设置 `ANDROID_HOME`，从仓库根目录运行：

```sh
./gradlew :plugin:test :plugin:jar
GUARD_VERIFY_ASSEMBLE=true ruby plugin/src/test/fixtures/manifest-source/run-regression.rb
```

脚本复制测试工程到临时目录，分别运行以下场景，结束后自动删除副本：

- 依赖库先配置、两个 app、自定义主 Manifest、附加 Manifest、类型安全项目依赖。
- AndResGuard/AabResGuard 任务在 `afterEvaluate` 中晚注册，检查 Debug/Release 的扫描依赖。
- 开启集成但未提供对应资源任务，检查配置失败与明确错误信息。
- 在 Kotlin 源码目录中实际执行类混淆、目录移动、包名修改，检查自定义 Manifest 和源码结果；开启 `GUARD_VERIFY_ASSEMBLE=true` 后分别重新生成 Debug APK。
- 检查 `xml/shortcuts.xml` 与 `xml-v25/shortcuts.xml`：识别仅被 `android:targetClass` 引用的类，更新混淆/移动后的类路径，保持 `targetPackage` 和外部应用 Intent 不变。

直接运行只读检查：

```sh
./gradlew -p plugin/src/test/fixtures/manifest-source verifyManifestSources verifyResourceTaskDependencies -PguardPluginJar="$PWD/plugin/build/libs/plugin.jar"
```

资源任务使用模拟任务，不代表第三方资源混淆插件自身兼容 AGP 9.5.0。脚本不对工作区中的应用执行源码修改。诊断日志写入 `/tmp/xmlguard-*.log`。

真实资源插件的兼容性结果见下表；KMP、真机运行、Release 混淆与签名发布不在此回归工程覆盖范围内。

## 本轮验证记录（2026-11-02）

| 检查 | 结果 |
| --- | --- |
| AGP 9.5.0 下插件编译、JAR、3 个有效单元测试 | 通过 |
| 多 app、依赖库先配置、自定义/附加 Manifest、类型安全项目依赖 | 通过 |
| 两种资源任务晚注册及缺失任务错误提示 | 模拟回归通过 |
| 临时工程实际执行混淆、目录移动、包名修改 | 通过 |
| 上述三种改写后分别构建 Debug APK | 通过 |
| 完整示例副本执行跨模块混淆后编译 Kotlin/Safe Args | 通过 |
| 主工程 app/base/kts_test 完整 Debug 构建 | 通过：联网构建和随后常规离线复验均成功；APK 与两个 AAR 已生成 |
| 真实 AndResGuard 1.2.21 | 新 DSL 配置失败（applicationVariants）；旧 DSL 配置也失败（variantDslInfo 内部接口变化） |
| 真实 AabResGuard 0.1.10 | 新 DSL 配置失败（强转 AppExtension）；旧 DSL 配置通过，真实资源混淆打包失败（private @TaskAction execute 被 Gradle 拒绝） |

未验证通过：真实资源混淆打包和白名单写入（第三方插件失败，见上表）。尚未覆盖：KMP、产品风味组合、增量混淆重复执行、配置缓存、旧 AGP/Gradle 版本、设备运行及 Release 混淆/签名发布。上述范围不能由 Debug 编译或模拟任务测试推断为通过。

本轮下载曾使用受限 Google Maven 镜像补齐大型 Lint 依赖并预热缓存；当前 `settings.gradle.kts` 保留项目现有仓库配置，没有固定启用该镜像。空缓存环境仍需联网解析依赖。

真实插件兼容矩阵（需要先完成在线依赖解析；该脚本随后用离线缓存验证）：

```sh
ANDROID_HOME=/path/to/android/sdk ruby plugin/src/test/fixtures/manifest-source/run-resource-compatibility.rb
```

脚本使用真实发布的插件，分别验证新 DSL 和旧 DSL；配置成功后再调用实际资源打包任务。详细错误保存在 `/tmp/xmlguard-real-*.log`。当前发布版本均不能在 AGP 9.5.0 / Gradle 9.8.2 下完成资源混淆，不能将模拟依赖绑定测试视为真实资源混淆通过。
