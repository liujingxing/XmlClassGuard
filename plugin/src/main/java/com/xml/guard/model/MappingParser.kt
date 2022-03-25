package com.xml.guard.model

import com.xml.guard.utils.toInt
import java.io.File
import java.util.regex.Pattern

/**
 * User: ljx
 * Date: 2022/3/25
 * Time: 11:15
 */
object MappingParser {

    private val MAPPING_PATTERN: Pattern = Pattern.compile("\\s+(.*)->(.*)")

    fun parse(mappingFile: File): Mapping {
        val mapping = Mapping()
        var isDir = true
        if (!mappingFile.exists()) return mapping
        var classIndex = -1
        mappingFile.forEachLine { line ->
            if (line.isNotEmpty()) {
                val mat = MAPPING_PATTERN.matcher(line)
                if (mat.find()) {
                    val rawName = mat.group(1).trim()
                    val obfuscateName = mat.group(2).trim()
                    if (isDir) {
                        mapping.dirMapping[rawName] = obfuscateName
                    } else {
                        val num =
                            obfuscateName.substring(obfuscateName.lastIndexOf(".") + 1).toInt()
                        if (num > classIndex) classIndex = num
                        mapping.classMapping[rawName] = obfuscateName
                    }
                } else {
                    isDir = line == Mapping.DIR_MAPPING
                }
            }
        }
        mapping.classIndex = classIndex
        return mapping
    }
}