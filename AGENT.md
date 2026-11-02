# 项目协作指南

## 项目用途与结构

XmlClassGuard 是 Android Gradle 插件，在打包前修改 XML 引用的类名、包名及相关源码，并生成混淆映射。使用方式和限制见 `README.md`。

- `plugin/`：插件实现，入口为 `com.xml.guard.XmlClassGuardPlugin`。
- `plugin/src/main/java/com/xml/guard/tasks/`：类混淆、包名修改、目录移动与资源 ID 查找任务。
- `plugin/src/main/java/com/xml/guard/utils/`：源码解析、项目依赖遍历和字符串工具。
- `plugin/src/main/java-templates/`：版本模板，由 `copyJavaTemplates` 生成源码，不直接编辑生成目录。
- `app/`：Kotlin DSL 示例应用；`base/`：示例依赖库；`kts_test/`：Kotlin DSL 示例模块。
- `plugin/src/test/`：JUnit 4 测试与解析样例。

## 修改约定

- 使用中文说明变更，Kotlin 遵循官方代码风格，保持周边文件现有格式。
- 开始前查看 `git status` 和 `git diff`，保留用户已有修改，不执行清理或回滚。
- 优先使用 Gradle 公开 API，避免新增对 Gradle 内部实现类的依赖。
- 修改替换或解析逻辑时，关注 Kotlin、Java、Manifest、layout、navigation、xml 与映射文件之间的一致性。
- 修改插件配置时，同时考虑 Groovy 与 Kotlin DSL 示例；不要无关升级依赖或改写发布配置。
- 不提交 `local.properties`、本机路径、凭据、构建产物或生成的类文件。

## 构建与验证

- 使用仓库 Gradle Wrapper：`./gradlew :plugin:compileKotlin` 验证插件编译。
- 需要单元测试时运行 `./gradlew :plugin:test`；现有解析测试含绝对路径，ASM 测试会写出类文件，执行前检查测试行为。
- 涉及示例集成时运行 `./gradlew :app:assembleDebug :base:assembleDebug :kts_test:assembleDebug`，需具备相应 Android SDK 和 JDK。
- 版本以 `build.gradle.kts`、`gradle.properties` 与 Wrapper 配置为准；JVM 字节码目标与运行 Gradle 的 JDK 是不同配置。
- 当前使用 AGP 9.5.0 新 DSL 与 Variant API，插件编译目标为 Java 17；AGP 构建插件和插件编译依赖共用版本目录中的 agp。
- 无法完成验证时，准确报告执行命令、失败原因及未验证范围。

## 会修改源码的任务

`xmlClassGuard`、`moveDir`、`packageChange` 会直接修改项目源码和目录。常规验证不执行这些任务；需要验证其行为时，在临时副本或专用测试工程中进行，并检查源码与映射文件的最终差异。

## 发布

插件发布定义位于 `plugin/build.gradle.kts`，版本来自 `gradle.properties` 的 `plugin_version`，保留该属性作为插件自身版本的来源。仅在用户要求发布时执行发布任务，发布前检查示例工程的插件依赖配置。
