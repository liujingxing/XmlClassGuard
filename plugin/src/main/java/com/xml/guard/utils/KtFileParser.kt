package com.xml.guard.utils

import java.io.File
import java.util.regex.Pattern


private val classPattern = Pattern.compile("class\\s+([_a-zA-Z-0-9]+)[\\s(]")
private val funPattern = Pattern.compile("(fun|fun\\s.*)\\s([._a-zA-Z0-9]+)\\s*\\(")
private val fieldPattern = Pattern.compile("va[lr]\\s+([._a-zA-Z0-9]+)[\\s:]+")
private val jvmFileNamePattern = Pattern.compile("@file\\s*:\\s*JvmName\\s*[(]\\s*\"(.+)\\s*\"[)]")
private val docOrCommentPattern = Pattern.compile("(/[*][\\s\\S]*?[*]/)|(//.*\n)")

/**
 * User: ljx
 * Date: 2023/6/24
 * Time: 16:27
 *
 * 注意，如果声明的字符串里出现 //、 /*、 */、{、}、(、)、fun、val、var、class等字符时，极有可能出现未知异常
 */
class KtFileParser(file: File, filename: String) {

    private val topClassNames = mutableListOf<String>()
    val topFunNames = mutableListOf<String>()
    val topFieldNames = mutableListOf<String>()
    var jvmName: String? = null

    init {
        if (file.name.endsWith("kt")) {
            try {
                var content = file.readText()
                val matcher = jvmFileNamePattern.matcher(content)
                if (matcher.find()) {
                    jvmName = matcher.group(1).trim()
                }
                content = content.removeAllDocsAndComments()
                content = content.removeAllBody()

                topClassNames.addAll(content.findClassNames())
                topFunNames.addAll(content.findFunNames())
                topFieldNames.addAll(content.findFieldNames())
                topClassNames.remove(filename)
            } catch (t: Throwable) {
                throw IllegalArgumentException("$file parser fail", t)
            }
        }
    }

    fun getTopClassOrFunOrFieldNames(): List<String> {
        val answers = mutableListOf<String>()
        answers.addAll(topClassNames)
        answers.addAll(topFunNames)
        answers.addAll(topFieldNames)
        return answers;
    }
}


private fun String.findClassNames(): MutableList<String> {
    val classNames = mutableListOf<String>()
    var index = 0
    while (true) {
        val matcher = classPattern.matcher(this)
        if (matcher.find(index)) {
            val classname = matcher.group(1)
            classNames.add(classname)
            index = matcher.end()
        } else {
            break
        }
    }
    return classNames
}

private fun String.findFunNames(): List<String> {
    val funNames = mutableListOf<String>()
    var index = 0
    while (true) {
        val matcher = funPattern.matcher(this)
        if (matcher.find(index)) {
            val names = matcher.group(2).split('.')  //拆分扩展方法
            funNames.add(names.last())
            index = matcher.end()
        } else {
            break
        }
    }
    return funNames
}

private fun String.findFieldNames(): List<String> {
    val fieldNames = mutableListOf<String>()
    var index = 0
    while (true) {
        val matcher = fieldPattern.matcher(this)
        if (matcher.find(index)) {
            val names = matcher.group(1).split('.') //拆分扩展属性
            fieldNames.add(names.last())
            index = matcher.end()
        } else {
            break
        }
    }
    return fieldNames
}


//移除所有的注释
private fun String.removeAllDocsAndComments(): String {
    var content = this
    var index = 0
    while (true) {
        val matcher = docOrCommentPattern.matcher(content)
        if (matcher.find(index)) {
            index = matcher.start()
            content = content.removeRange(index, matcher.end())
        } else {
            break
        }
    }
    return content
}

private fun String.removeAllBody(): String {
    var content = this
    var index = 0
    while (true) {
        val newIndex = content.indexOf('{', index)
        if (newIndex == -1) break
        content = content.removeBody(newIndex)
        index = newIndex
    }
    return content
}

private fun String.removeBody(startIndex: Int): String {
    if (startIndex < 0) return this
    var endIndex = -1
    var level = 0
    val charArray = toCharArray()
    for (i in startIndex until charArray.size) {
        val char = charArray[i]
        if (char == '{') {
            level++
        } else if (char == '}') {
            level--
        }
        if (level == 0) {
            endIndex = i
            break
        }
    }
    return removeRange(startIndex, endIndex + 1)
}
