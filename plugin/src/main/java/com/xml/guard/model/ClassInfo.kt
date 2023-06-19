package com.xml.guard.model

/**
 * User: ljx
 * Date: 2023/6/16
 * Time: 11:28
 */
class ClassInfo(
    val classPath: String,
    val hasAction: Boolean = false,
    val fromImportNode: Boolean = false
)