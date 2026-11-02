import org.gradle.kotlin.dsl.withGroovyBuilder

apply(plugin = "xml-class-guard")


//以下均为非必须，根据自己需要配置即可
extensions.getByName("xmlClassGuard").withGroovyBuilder {
    /*
     * 是否查找约束布局的constraint_referenced_ids属性的值，并添加到AabResGuard的白名单中，
     * 是的话，要求你在XmlClassGuard前依赖AabResGuard插件，默认false
     */
    setProperty("findAabConstraintReferencedIds", true)

    /*
     * 是否查找约束布局的constraint_referenced_ids属性的值，并添加到AndResGuard的白名单中，
     * 是的话，要求你在XmlClassGuard前依赖AabResGuard插件，默认false
     */
    setProperty("findAndConstraintReferencedIds", false)

    //混淆映射文件
    setProperty("mappingFile", file("xml-class-mapping.txt"))
    //更改manifest里package属性
    setProperty("packageChange", hashMapOf(
        "com.ljx.example" to "ab.cd",
        "com.ljx.base" to "ggg.kkk",
        "com.ljx.kts" to "ff.dd"
    ))
    //移动目录，支持同时移动多个目录
    setProperty("moveDir", hashMapOf(
        "com.ljx.example" to "ef.gh",
        "com.ljx.base" to "zzzz.hhhh",
        "com.ljx.kts" to "ddd.xxx"
    ))
}
