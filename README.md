[![](https://jitpack.io/v/liujingxing/XmlClassGuard.svg)](https://jitpack.io/#liujingxing/XmlClassGuard)
[![](https://img.shields.io/badge/change-更新日志-success.svg)](https://github.com/liujingxing/XmlClassGuard/wiki/%E6%9B%B4%E6%96%B0%E6%97%A5%E5%BF%97)

***加我微信 ljx-studio 拉你进微信群(备注来意，否则不通过)***

# XmlClassGuard 简介

- `XmlClassGuard`是一个可混淆Android 4大组件、自定义View等任意类的插件

- `XmlClassGuard`可以看作是`ProGuard`的一个补充，跟`ProGuard`没有任何关系，也不会有任何冲突

- 可快速更改`manifest`文件里的`package`属性，并同步到其他文件中

- 可快速移动n个目录到其他目录中，并同步到其他文件中

- 可查找`constraint_referenced_ids`属性的值，并自动添加到`AabResGuard`的白名单中

- `XmlClassGuard`最主要的功能是混淆xml文件用到的类，故取名为`XmlClassGuard`,与[AndResGuard](https://github.com/shwenzhang/AndResGuard)、[AabResGuard](https://github.com/bytedance/AabResGuard)对应


# 有什么用？

- 弥补`ProGuard`不混淆4大组件等类问题

- 增加aab、apk反编译的难度

- 极大降低aab包查重率，避免上架`Google Play`因查重率过高，导致下架或封号问题

关于第三点，有过上架`Google Play` 商店的同学应该知道，如果之前的包被下架或封号，想要同套代码再次上架，那99%概率是再次封号，很大一部分原因就是以上说到的类未被混淆，很容易被Google断定为包重复，从而导致再次封号，因此，如果想要再次上架，就必须要更改四大组件、自定义View等的`包名+类名`以降低查重率，然而，如果手动去完成这项任务，估计会累死一个程序员，于是乎，就有了`XmlClassGuard`，通过插件去完成手工的活，一个任务便可搞定


# 原理

`XmlClassGuard`不同于`AndResGuard(apk资源混淆)、AadResGuard(aab资源混淆)`侵入打包流程的方案，`XmlClassGuard`
需要在打包前执行`xmlClassGuard`任务，该任务会检索`AndroidManifest.xml`及`navigation、layout`
文件夹下的xml，找出xml文件中引用的类，如4大组件及自定义View等，更改其`包名+类名`，并将更改后的内容同步到其他文件中，说直白点，就是在打包前，在本地更改`包名+类名`

# 警告警告⚠️

**由于是在本地操作，任务执行是不可逆的，故务必做好代码备份，否则代码将很难还原**

# 上手

1、在`build.gradle(root project)`中配置

```gradle
buildscript {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        classpath "com.github.liujingxing:XmlClassGuard:1.0.0"
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

此时就可以在`Gradle`栏中，找到以下4个任务

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/7e98f0285572474182947cdfdc7c064d~tplv-k3u1fbpfcp-watermark.image?)

# 任务介绍

`XmlClassGuard`插件共有4个任务，分别是`findConstraintReferencedIds`、`moveDir`、`packageChange`及`xmlClassGuard`，这4个任务之间没有任何关系，下面将一一介绍

## 1、findConstraintReferencedIds

该任务需要配合[AabResGuard](https://github.com/bytedance/AabResGuard)插件使用，如果你未使用AabResGuard插件，可忽略。

这里简单介绍下，由于约束布局`constraint_referenced_ids`属性的值，内部是通过getIdentifier方法获取具体的id，这就要求我们把`constraint_referenced_ids`属性的值添加进`AabResGurad`的白名单中，否则打包时，id会被混淆，打包后，`constraint_referenced_ids`属性会失效，UI将出现异常。

然而，项目中可能很多地方都用到`constraint_referenced_ids`属性，并且值非常多，要一个个找出来并手动添加到`AabResGuard`的白名单中，无疑是一项繁琐的工作，于是乎，`findConstraintReferencedIds`任务就派上用场了，它是在打包时，自动查找`constraint_referenced_ids`属性并添加进`AabResGuard`的白名单中，非常实用的功能，你仅需要在`XmlClassGurad`的配置`findConstraintReferencedIds`为true即可，如下：

```gradle
//以下均为非必须
xmlClassGuard {
    /*
     * 是否查找约束布局的constraint_referenced_ids属性的值，并添加到AabResGuard的白名单中，
     * true的话，要求你在XmlClassGuard前依赖AabResGuard插件，默认false
     */
    findConstraintReferencedIds = true
}
```
`findConstraintReferencedIds`任务不需要手动执行，打包(aab)时会自动执行

## 2、moveDir

`moveDir`是一个移动目录的任务，它支持同时移动任意个目录，它会将原目录下的所有文件(包括子目录)移动到另外一个文件夹下，并将移动的结果，同步到其他文件中，配置如下：

```gradle
xmlClassGuard {
    //移动目录
    moveDir = ["com.ljx.example": "ef.gh",
               "com.ljx.example.test": "ff.gg"]
}
```

上面代码中`moveDir`是一个Map对象，其中key代表要移动的目录，value代表目标目录； 上面任务会把`com.ljx.example`目录下的所有文件，移动到`ef.gh`
目录下，将`com.ljx.example.test`目录下的所有文件移动到`ff.gg`目录下

## 3、packageChange

`packageChange`是一个更改`manifest`文件里`package`属性的任务，也就是更改app包名的任务(不会更改applicationId)
，改完后，会将更改结果，同步到其他文件中(不会更改项目结构)，配置如下：

```gradle
xmlClassGuard {
    //更改manifest文件的package属性，即包名
    packageChange = ["com.ljx.example": "ab.cd"]
}
```

以上`packageChange`是一个Map对象，key为原始package属性，value为要更改的package属性，原始package属性不匹配，将更改失败

## 4、xmlClassGuard

`xmlClassGuard`是一个混淆类的任务，该任务会检索`AndroidManifest.xml`及`navigation、layout`
文件夹下的xml文件，找出xml文件中引用到的类，如4大组件及自定义View等，更改其`包名+类名`，并将更改的结果，同步到其他文件中，最后会将混淆映射写出到mapping文件中，配置如下：

```gradle
xmlClassGuard {
    //用于增量混淆的 mapping 文件
    mappingFile = file("xml-class-mapping.txt")
}
```

上面配置的`mappingFile`可以是一个不存在的文件，混淆结束后，会将混淆映射写出到该文件中，如下：

```xml
dir mapping:
    com.ljx.example -> e
    com.ljx.example.activity -> dh

class mapping:
    com.ljx.example.AppHolder -> e.B
    com.ljx.example.activity.MainActivity -> dh.C
```

`dir mapping`是混淆的目录列表，`class mapping`是具体类的混淆列表

## 4、混淆任意类

`xmlClassGuard`任务是支持增量混淆的，如果你需要混淆指定的类`com.ljx.example.test.Test`，便可以在`dir mapping`下写入`com.ljx.example.test -> h`,
此时再次执行`xmlClassGuard`任务，便会将`com.ljx.example.test`目录下的所有类(不包含子目录下的类)
移动到`h`文件夹中，并将所有类名混淆，再次混淆的后mapping文件如下：

```xml
dir mapping:
    com.ljx.example -> e
    com.ljx.example.activity -> dh
    com.ljx.example.test -> h

class mapping:
    com.ljx.example.AppHolder -> e.B
    com.ljx.example.activity.MainActivity -> dh.C
    com.ljx.example.test.Test -> h.D
```

手动输入混淆规则，需要注意以下几条规则

![mapping_rule.jpg](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/32f446561004426193dae50537488180~tplv-k3u1fbpfcp-watermark.image?)


## 5、每次混淆产生不一样的结果

默认情况下，每次混淆，都将产生一样的结果，混淆的包名根据哈希算法得出，混淆的类名，从大写字母A开启，依次递增，如：`A B C ... Y Z BA BB .. ZY ZZ BAA...`

如果你需要每次混淆产生不一样的结果，只需做两步

- 对于包名，需要你配置每一个

- 对于类名，可以每一个都去配置，但类太多时，配置每一个，就显得繁琐，此时仅需要配置一个即可

如我们修改一下上面的`mapping`文件，如下

```xml
dir mapping:
    com.ljx.example -> hh
    com.ljx.example.activity -> jk
    com.ljx.example.test -> et

class mapping:
    com.ljx.example.AppHolder -> hh.Z
```

此时执行`xmlClassGuard`任务，就会产生不一样的结果，如下：

```xml
dir mapping:
	com.ljx.example -> hh
	com.ljx.example.activity -> jk
	com.ljx.example.test -> et

class mapping:
	com.ljx.example.AppHolder -> hh.Z
	com.ljx.example.activity.MainActivity -> jk.BA
	com.ljx.example.test.Test -> et.BC
```

可以看到，包名完全是根据自定义生成的结果，而类名便从`Z`开始，依次递增`Z BA BC ...`, 这里可以把包名看成26进制的字符串依次递增


# 注意事项⚠️

- 要混淆的类，要避免与其他类同名，否则类名替换时，会出现误杀情况

- 类混淆后，类的包名(路径)也会被混淆，所以，如果你用到一些三方库，有配置包名的地方，记得手动更改

- `XmlClassGuard`不会更改`proguard-rules.pro`文件的内容，所以，类混淆后，如果该文件内容有混淆前的类或目录，也记得手动更改

- `XmlClassGuard`只会帮你更改`包名+类名`，并同步带其他文件中，不会更改你的任何代码逻辑，如混淆后，出现部分功能不正常问题，需要你自己查找原因，如果是`XmlClassGuard`的问题，欢迎提[issue](https://github.com/liujingxing/XmlClassGuard/issues)或[PR](https://github.com/liujingxing/XmlClassGuard/pulls)

## Donations
如果它对你帮助很大，并且你很想支持库的后续开发和维护，那么你可以扫下方二维码随意打赏我，就当是请我喝杯咖啡或是啤酒，开源不易，感激不尽

![rxhttp_donate.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/aafa7d05cfda4b2ea2a092bba8ebc1a0~tplv-k3u1fbpfcp-watermark.image)

 














