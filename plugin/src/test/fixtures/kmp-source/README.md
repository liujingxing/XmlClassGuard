# KMP Android 回归

适配目标为 `org.jetbrains.kotlin.multiplatform` 与 `com.android.kotlin.multiplatform.library`，当前验证 AGP 9.5.0、Kotlin 2.4.21、Gradle 9.8.2。

从仓库根目录运行，先设置本机 `ANDROID_HOME`：

```sh
./gradlew :plugin:test :plugin:jar
ruby plugin/src/test/fixtures/kmp-source/run-regression.rb
```

脚本在临时副本中执行，自动清理副本，日志位于 `/tmp/xmlguard-kmp-*.log`。正常、混淆、目录移动、包名修改四种场景都检查结果并构建 Android APK、编译 JVM target 的生产和测试源码。

覆盖：

- app 通过类型安全项目引用依赖 shared，shared 从 commonMain api 继续依赖另一个 KMP Android 模块。
- Android compilation 的 commonMain、手动 dependsOn 中间层、自定义 Kotlin 源码目录；排除 JVM 专属源码和测试源码作为 XML 类检索的来源；修改关联生产包后会同步这些源码中的声明和引用。
- 默认 Android res、namespace、Manifest；无 Manifest、未开启资源的 KMP 模块。
- Android XML 引用类的实际混淆、commonMain 类映射，以及 Android/JVM expect/actual 声明与其他 target、测试源码引用同步。
- app 与 shared 使用相同源码包名时，所有模块的类都正确处理。
- 修改 commonMain 包名、移动目录后，Android 与 JVM 的生产及测试源码均能编译。

边界：

- 插件仍应用于 Android app，自动检索其依赖的 KMP Android 模块；不直接应用于 shared。
- 新版 KMP 的 Android 资源和 Manifest 使用 AGP 默认的 `src/<Android主sourceSet>/res` 与 `AndroidManifest.xml`。通过 Variant API 额外注册的资源、Java、Manifest 路径尚未适配。
- Compose Multiplatform 的 composeResources 不属于 Android res，不扫描。
- 尚未验证旧 `com.android.library + kotlin.multiplatform` 集成、iOS/Native 编译及运行、KMP 发布、配置缓存。
