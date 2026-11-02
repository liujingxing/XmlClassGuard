import org.gradle.kotlin.dsl.withGroovyBuilder
import org.gradle.api.artifacts.VersionCatalogsExtension

apply(plugin = "AndResGuard")
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")


extensions.getByName("andResGuard").withGroovyBuilder {
    // mappingFile = file("./resource_mapping.txt")
//    mappingFile = null
    setProperty("use7zip", true)
    setProperty("useSign", true)
    // 打开这个开关，会keep住所有资源的原始路径，只混淆资源的名字
    setProperty("keepRoot", false)
    // 设置这个值，会把arsc name列混淆成相同的名字，减少string常量池的大小
    setProperty("fixedResName", "abcd")
    // 打开这个开关会合并所有哈希值相同的资源，但请不要过度依赖这个功能去除去冗余资源
    setProperty("mergeDuplicatedRes", true)
    setProperty("whiteList", listOf(
            "R.drawable.vol*",
            "R.drawable.hd_record_animate_*"
    ))


    setProperty("compressFilePattern", listOf(
            "*.webp",
            "*.png",
            "*.jpg",
            "*.jpeg",
            "*.gif",
    ))
    "sevenzip" {
        setProperty("artifact", catalog.findLibrary("sevenzip").get().get().toString())
        //path = "/usr/local/bin/7za"
    }

    /**
     * 可选： 如果不设置则会默认覆盖assemble输出的apk
     **/
    // finalApkBackupPath = "${project.rootDir}/final.apk"

    /**
     * 可选: 指定v1签名时生成jar文件的摘要算法
     * 默认值为“SHA-1”
     **/
    // digestalg = "SHA-256"
}
