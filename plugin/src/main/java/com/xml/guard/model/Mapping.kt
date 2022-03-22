package com.xml.guard.model

import com.xml.guard.utils.findLocationProject
import com.xml.guard.utils.getDirPath
import com.xml.guard.utils.getSuffix
import com.xml.guard.utils.insertImportXxxIfAbsent
import com.xml.guard.utils.javaDir
import com.xml.guard.utils.manifestFile
import com.xml.guard.utils.removeSuffix
import com.xml.guard.utils.toInt
import com.xml.guard.utils.toLetterStr
import com.xml.guard.utils.toUpperLetterStr
import groovy.xml.XmlParser
import org.gradle.api.Project
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.regex.Pattern


/**
 * User: ljx
 * Date: 2022/3/16
 * Time: 22:02
 */
class Mapping(private val mappingFile: File) {

    companion object {
        private val MAPPING_PATTERN: Pattern = Pattern.compile("\\s+(.*)->(.*)")
        private const val DIR_MAPPING = "dir mapping:"
        private const val CLASS_MAPPING = "class mapping:"
    }


    private val dirMapping = mutableMapOf<String, String>()
    private val classMapping = mutableMapOf<String, String>()

    //通过index记录类名，防止重复
    private var classIndex = -1

    init {
        var isDir = true
        if (mappingFile.exists())
            mappingFile.forEachLine { line ->
                if (line.isNotEmpty()) {
                    val mat = MAPPING_PATTERN.matcher(line)
                    if (mat.find()) {
                        val rawName = mat.group(1).trim()
                        val obfuscateName = mat.group(2).trim()
                        if (isDir) {
                            dirMapping[rawName] = obfuscateName
                        } else {
                            val num =
                                obfuscateName.substring(obfuscateName.lastIndexOf(".") + 1).toInt()
                            if (num > classIndex) classIndex = num
                            classMapping[rawName] = obfuscateName
                        }
                    } else {
                        isDir = line == DIR_MAPPING
                    }
                }
            }
    }

    //遍历文件夹下的所有直接子类，混淆文件名及移动目录
    fun obfuscateAllClass(project: Project): Map<String, String> {
        val classMapped = mutableMapOf<String, String>()
        val iterator = dirMapping.iterator()
        val manifestPackage = project.manifestFile().findPackage()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val rawDir = entry.key
            val locationProject = project.findLocationProject(rawDir)
            if (locationProject == null) {
                iterator.remove()
                continue
            }
            //去除目录的直接子文件
            val dirPath = rawDir.replace(".", "/")
            val childFiles = locationProject.javaDir(dirPath).listFiles { f -> !f.isDirectory }
            if (childFiles.isNullOrEmpty()) continue
            for (file in childFiles) {
                val rawClassPath = "${rawDir}.${file.name.removeSuffix()}"
                //已经混淆
                if (isObfuscated(rawClassPath)) continue
                if (rawDir == manifestPackage) {
                    file.insertImportXxxIfAbsent(manifestPackage)
                }
                val obfuscatePath = obfuscatePath(rawClassPath)
                val relativePath = obfuscatePath.replace(".", "/") + file.name.getSuffix()
                val newFile = locationProject.javaDir(relativePath)
                if (!newFile.exists()) newFile.parentFile.mkdirs()
                newFile.writeText(file.readText())
                file.delete()
                classMapped[rawClassPath] = obfuscatePath
            }
        }
        return classMapped
    }

    fun isObfuscated(rawClassPath: String) = classMapping.containsValue(rawClassPath)

    fun obfuscatePath(rawClassPath: String): String {
        var obfuscateClassPath = classMapping[rawClassPath]
        if (obfuscateClassPath == null) {
            val rawPackage = rawClassPath.getDirPath()
            val obfuscatePackage = obfuscatePackage(rawPackage)
            obfuscateClassPath = "$obfuscatePackage.${generateObfuscateClassName()}"
            classMapping[rawClassPath] = obfuscateClassPath
        }
        return obfuscateClassPath
    }


    fun writeMappingToFile() {
        val writer: Writer = BufferedWriter(FileWriter(mappingFile, false))

        writer.write("$DIR_MAPPING\n")
        for ((key, value) in dirMapping) {
            writer.write(String.format("\t%s -> %s\n", key, value))
        }
        writer.write("\n")
        writer.flush()

        writer.write("$CLASS_MAPPING\n")
        for ((key, value) in classMapping.entries) {
            writer.write(String.format("\t%s -> %s\n", key, value))
        }
        writer.flush()

        writer.close()
    }

    private fun obfuscatePackage(rawPackage: String): String {
        var obfuscatePackage = dirMapping[rawPackage]
        if (obfuscatePackage == null) {
            val packageHash = hash(rawPackage)
            val index = packageHash and 0xff
            obfuscatePackage = index.toLetterStr()
            dirMapping[rawPackage] = obfuscatePackage
        }
        return obfuscatePackage
    }

    private fun generateObfuscateClassName(): String {
        if (++classIndex == 17) { //跳过字母 R
            classIndex++
        }
        return classIndex.toUpperLetterStr()
    }

    private fun hash(key: Any): Int {
        val h = key.hashCode()
        return h xor (h ushr 16)
    }


    private fun File.findPackage(): String? {
        val rootNode = XmlParser(false, false).parse(this)
        return rootNode.attribute("package")?.toString()
    }

}