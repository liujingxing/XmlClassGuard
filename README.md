# XmlClassGuard 简介

`XmlClassGuard`是一个可混淆任意类的Gradle插件，Android 4大组件、自定义View、任意类，只要你想，都可以将其混淆

# 有什么用？

- 增加aab、apk反编译的难度

- 极大降低aab包查重率，避免上架Google Play时，因查重率过高，导致下架或封号

# 原理

`XmlClassGuard`不同于`AndResGuard(apk资源混淆)、AadResGuard(aab资源混淆)`侵入打包流程的方案，`XmlClassGuard`
需要在打包前执行`xmlClassGuard`任务，该任务会检索`AndroidManifest.xml`及`navigation、layout`
文件夹下的xml，找出xml文件中引用的类，如4大组件及自定义View等，更改其`包名+类名`，并将更改后的内容同步到其他文件中，此过程暂时不可逆，所以，在执行`xmlClassGuard`
任务前，请务必做好代码备份

# 警告警告⚠️

**任务执行是不可逆的，执行前，务必做好代码备份，否则代码将很难还原**

# 上手

1、在`build.gradle(root project)`中配置

```gradle
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath "com.github.liujingxing:xml-class-guard-plugin:1.0.0"
    }
}
```
2、在 `build.gradle(application)` 中配置

```gradle
apply plugin: "xml-class-guard"

//以下均为非必须
xmlClassGuard {
    //用于增量混淆的 mapping 文件
    mappingFile = file("xml-class-mapping.txt")
    //更改manifest文件的package属性，即包名
    packageChange = ["com.ljx.example": "ab.cd"]
    //移动目录
    moveDir = ["com.ljx.example": "ef.gh"]
}
```
此时就可以在`Gradle`栏中，找到以下3个任务

![guard.jpg](https://github.com/liujingxing/xml-class-guard-plugin/blob/master/image/guard.jpg)













