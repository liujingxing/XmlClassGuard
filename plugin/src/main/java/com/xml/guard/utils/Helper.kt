package com.xml.guard.utils

import java.io.File
import java.util.regex.Pattern

/**
 * User: ljx
 * Date: 2022/3/22
 * Time: 10:31
 */

private val packageNameBlackList = hashSetOf(
    "in", "is", "as", "if", "do", "by", "new", "try", "int", "for", "out", "var", "val", "fun",
    "byte", "void", "this", "else", "case", "open", "enum", "true", "false", "inner", "unit",
    "null", "char", "long", "super", "while", "break", "float", "final", "short", "const",
    "throw", "class", "catch", "return", "static", "import", "assert", "inline", "reified",
    "object", "sealed", "vararg", "suspend",
    "double", "native", "extends", "switch", "public", "package", "throws", "continue",
    "noinline", "lateinit", "internal", "companion",
    "default", "finally", "abstract", "private", "protected", "implements", "interface",
    "strictfp", "transient", "boolean", "volatile", "instanceof", "synchronized", "constructor"
)

private val classNameBlackList = hashSetOf("R", "BR")


fun String.inClassNameBlackList() = this in classNameBlackList

fun String.inPackageNameBlackList() = this in packageNameBlackList


private val packagePattern = Pattern.compile("\\s*package\\s+(.*)")

//插入 import xx.xx.xx.R  import xx.xx.xx.BuildConfig    语句，
fun File.insertImportXxxIfAbsent(newPackage: String) {
    val text = readText()
    val importR = "import $newPackage.R"
    val importBuildConfig = "import $newPackage.BuildConfig"
    //如果使用引用了R类且没有导入，则需要导入
    val needImportR = text.findWord("R.") != -1 && text.findWord(importR) == -1
    //如果使用引用了BuildConfig类且没有导入，则需要导入
    val needImportBuildConfig = text.findWord("BuildConfig.") != -1 &&
            text.findWord(importBuildConfig) == -1
    if (!needImportR && !needImportBuildConfig) return
    val builder = StringBuilder()
    val matcher = packagePattern.matcher(text)
    if (matcher.find()) {
        val packageStatement = matcher.group()
        val packageIndex = text.indexOf(packageStatement)
        if (packageIndex > 0) {
            builder.append(text.substring(0, packageIndex))
        }
        builder.append(text.substring(packageIndex, packageIndex + packageStatement.length))
        builder.append("\n\n")
        if (needImportR) {
            //import xx.xx.xx.R
            builder.append(importR)
            if (name.endsWith(".java")) {
                builder.append(";")
            }
            builder.append("\n")
        }

        if (needImportBuildConfig) {
            //import xx.xx.xx.BuildConfig
            builder.append(importBuildConfig)
            if (name.endsWith(".java")) {
                builder.append(";")
            }
            builder.append("\n")
        }
        builder.append(text.substring(packageIndex + packageStatement.length))
    }
    writeText(builder.toString())
}