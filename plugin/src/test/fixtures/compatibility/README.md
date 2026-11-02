# 已发布版本的兼容性边界

验证对象：`com.github.liujingxing:XmlClassGuard:1.2.8-beta1`，从 Maven/JitPack 解析，未使用本地 `plugin.jar`。

已验证 JAR SHA-256：`38e84962f50e9777d4da98fe4eb38d2b358ab9c0acb4c0409221fc4ecf9e8cbc`。

## 普通 Android 项目

最低支持组合为 **AGP 8.5.0、Gradle 8.11、JDK 17**。Gradle 版本还必须满足所选 AGP 的要求；升级 AGP 后不能一直固定使用 Gradle 8.11。

| AGP | Gradle | 已发布插件实测结果 |
| --- | --- | --- |
| 8.4.2 | 8.13 | 源码扫描失败：`AndroidSourceDirectorySet.getDirectories()` 不存在 |
| 8.5.0 | 8.10.2 | 项目依赖扫描失败：`ProjectDependency.getPath()` 不存在 |
| 8.5.0 | 8.11 | Kotlin DSL 类型安全配置、源码与项目依赖扫描、实际混淆、Debug APK 构建通过 |
| 8.13.2 | 8.13 | 同上，通过 |

AGP 8.5 自身要求的 Gradle 8.7 不满足本插件使用的 Gradle API；`ProjectDependency.getPath()` 从 Gradle 8.11 才提供。参考 [Gradle API](https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.artifacts/-project-dependency/get-path.html) 和 [AGP／Gradle 对照表](https://developer.android.com/build/releases/about-agp)。

复现（从仓库根目录运行，参数为 AGP 版本及 Gradle 可执行文件）：

```sh
ANDROID_HOME=/path/to/sdk COMPAT_ONLINE=true ruby plugin/src/test/fixtures/compatibility/probe.rb 8.5.0 /path/to/gradle-8.11/bin/gradle
```

默认离线；首次运行设置 `COMPAT_ONLINE=true`。脚本只在临时工程修改源码，自动清理，日志保存到 `/tmp/xmlguard-compat-*.log`。工程使用 Java app/library、compileSdk 34、Build Tools 35，需预先安装对应 SDK。测试仓库使用阿里云 Google/Central 镜像并保留官方仓库和 JitPack，不改变主工程仓库。

## KMP Android 项目

最低支持组合为 **AGP 9.0.0、Gradle 9.1.0、JDK 17**，不能沿用普通 Android 的最低组合。这里特指 app 依赖使用 `com.android.kotlin.multiplatform.library` 的 shared，插件应用在 app。

| AGP | Gradle | KGP | 结果 |
| --- | --- | --- | --- |
| 8.10.0 | 8.11.1 | 2.4.21 | 未通过：shared 的传递模块没有检索到；该 AGP 还缺少插件使用的 `androidResources` API |
| 8.11.0 | 8.13 | 2.4.21 | 未通过：shared 的传递模块没有检索到 |
| 8.13.2 | 8.13 | 2.4.21 | 未通过：Android target 的 `platformType` 为 `jvm`，被发布版的 `androidJvm` 筛选排除 |
| 9.0.0 | 9.1.0 | 2.4.21 | 正常、混淆、移动目录、修改包名四场景的扫描／改写及 Android APK、JVM 生产和测试源码编译均通过 |
| 9.0.0 | 9.5.0 | 2.4.21 | 同上，通过 |

AGP 9.0.0 的 target 为 `androidJvm`，扫描断言通过。扫描断言覆盖 commonMain、中间 sourceSet、自定义源码目录、Android XML／Manifest、未启用资源的模块及传递依赖。构建测试覆盖 Android APK、JVM 生产和测试源码；混淆、移动目录、修改包名均在临时工程执行。AGP 8.x 的问题可能表现为漏扫而非直接报错，不能以 `help` 或插件加载成功作为兼容的判断标准。

复现：

```sh
ANDROID_HOME=/path/to/sdk COMPAT_ONLINE=true ruby plugin/src/test/fixtures/compatibility/kmp-probe.rb 9.0.0 /path/to/gradle-9.1.0/bin/gradle
```

KMP 工程使用 compileSdk 35、KGP 2.4.21，需安装 SDK 35 和所选 AGP 要求的 Build Tools；日志保存到 `/tmp/xmlguard-kmp-compat-*.log`。未下探 KGP 最低版本。Android-KMP 官方最低 AGP 8.10.0 是该 Android 插件的要求，不能据此推断 XmlClassGuard 的兼容性，参考 [Android-KMP 文档](https://developer.android.com/kotlin/multiplatform/plugin)。

## 范围

- 以上为插件本身的最低版本，不强制使用者的 Kotlin Gradle Plugin 升到插件编译时版本。项目使用的 KGP、Compose 等仍需满足各自兼容矩阵。
- 普通 Android 测试没有应用 KGP；不据此宣称所有 Kotlin／Compose 版本均兼容。
- 未穷举所有 AGP／Gradle 补丁版本、所有 productFlavor、自定义 sourceSet、Release／R8 和第三方资源混淆插件的组合。
- 未验证旧 `com.android.library + kotlin.multiplatform` 集成、iOS／Native 编译与运行；KMP 的其他边界见相邻 `kmp-source/README.md`。
- AndResGuard、AabResGuard 的兼容性独立于本插件的最低版本；这里未重新验证它们。
