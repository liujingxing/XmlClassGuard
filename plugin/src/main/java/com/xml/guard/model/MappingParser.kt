package com.xml.guard.model

import com.xml.guard.utils.to26Long
import java.io.File
import java.util.regex.Pattern

/**
 * User: ljx
 * Date: 2022/3/25
 * Time: 11:15
 */
object MappingParser {

    private val MAPPING_PATTERN: Pattern = Pattern.compile("\\s+(.*)->(.*)")
    private val UPPERCASE_PATTERN: Pattern = Pattern.compile("^[A-Z]+$")
    private val LOWER_PATTERN: Pattern = Pattern.compile("^[a-z]+$")

    fun parse(mappingFile: File): Mapping {
        val mapping = Mapping()
        var isDir = true
        if (!mappingFile.exists()) return mapping
        var classIndex = -1L
        var packageNameIndex = -1L
        mappingFile.forEachLine { line ->
            val mat = MAPPING_PATTERN.matcher(line)
            if (mat.find()) {
                val rawName = mat.group(1).trim()
                val obfuscateName = mat.group(2).trim()
                if (isDir) {
                    mapping.dirMapping[rawName] = obfuscateName
                    if (LOWER_PATTERN.matcher(obfuscateName).matches()) {
                        val index = obfuscateName.to26Long()
                        packageNameIndex = packageNameIndex.coerceAtLeast(index)
                    }
                } else {
                    val index = obfuscateName.lastIndexOf(".")
                    if (index == -1) {
                        //混淆路径必须要有包名
                        throw IllegalArgumentException("`$obfuscateName` is illegal, must have a package name")
                    }
                    val obfuscateClassPath = obfuscateName.substring(0, index) //混淆后的包名
                    val obfuscateClassName = obfuscateName.substring(index + 1) //混淆后的类名
                    if ("R" == obfuscateClassName) {
                        throw IllegalArgumentException("`$obfuscateName` is illegal, R cannot be defined as a class name")
                    }
                    if (!UPPERCASE_PATTERN.matcher(obfuscateClassName).find()) {
                        //混淆的类名必须要大写
                        throw IllegalArgumentException("`$obfuscateName` is illegal, Obfuscation class name must be capitalized")
                    }
                    val dirMapping = mapping.dirMapping
                    if (!dirMapping.containsValue(obfuscateClassPath)) {
                        val rawClassPath = rawName.substring(0, rawName.lastIndexOf(".")) //原始包名
                        if (dirMapping.containsKey(rawClassPath)) {
                            //类混淆的真实路径与混淆的目录不匹配
                            throw IllegalArgumentException("$rawName -> $obfuscateName is illegal should be\n$rawName -> ${dirMapping[rawClassPath]}.$obfuscateClassName")
                        }
                        dirMapping[rawClassPath] = obfuscateClassPath
                    }
                    val num = obfuscateClassName.to26Long()
                    classIndex = classIndex.coerceAtLeast(num)
                    mapping.classMapping[rawName] = obfuscateName
                }
            } else {
                isDir = line == Mapping.DIR_MAPPING
            }
        }
        mapping.classIndex = classIndex
        mapping.packageNameIndex = packageNameIndex
        return mapping
    }
}