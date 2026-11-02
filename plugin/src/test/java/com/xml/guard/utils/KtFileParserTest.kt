package com.xml.guard.utils

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class KtFileParserTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun getTopClassOrFunOrFieldNames() {
        val source = temporaryFolder.newFile("Example.kt")
        source.writeText("""
            package sample
            class Example { fun member() {} }
            class Helper {}
            fun topLevel() {}
            val answer: Int = 42
        """.trimIndent())
        val parser = KtFileParser(source, "Example")
        assertEquals(setOf("Helper", "topLevel", "answer"), parser.getTopClassOrFunOrFieldNames().toSet())
    }
}
