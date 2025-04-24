package org.example.kparserc.example

import org.example.kparserc.*
import org.example.kparserc.example.naive.NaiveJsonParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

object JsonParser {
    private val objStart = Ch('{').trim()
    private val objEnd = Ch('}').trim()
    private val arrStart = Ch('[').trim()
    private val arrEnd = Ch(']').trim()
    private val colon = Ch(':').trim()
    private val comma = Ch(',').trim()
    private val jsonObj = OneOf(
        Lazy { decimal }, Lazy { integer }, Lazy { string }, Lazy { boolLiteral }, Lazy { nullLiteral },
        Lazy { arr }, Lazy { obj },
    )

    private val integer = Match("""[+-]?\d+""").map { it.toInt() }.trim()
    private val decimal = Match("""[+-]?\d*\.\d+([eE][+-]?[0-9]+)?""").map { it.toDouble() }.trim()
    private val string = Match(""""([^"\x00-\x1F\x7F\\]|\\[\\"bfnrt]|\\u[a-fA-F0-9]{4})*"""").map { s ->
        val sb = StringBuilder()
        var currIndex = 1
        while (s[currIndex] != '"') {
            val char = s[currIndex++]
            if (char == '\\') {
                when (val escape = s[currIndex++]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    '/' -> sb.append('/')
                    'b' -> sb.append('\b')
                    'f' -> sb.append('\u000C')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    'u' -> {
                        val unicode = s.substring(currIndex..currIndex + 3)
                        currIndex += 4
                        sb.append(Integer.parseInt(unicode, 16).toChar())
                    }

                    else -> throw ParseException("Invalid escape character: $escape")
                }
            } else {
                sb.append(char)
            }
        }
        sb.toString()
    }.trim()
    private val boolLiteral = Strs("true", "false").map { it.toBoolean() }.trim()
    private val nullLiteral = Str("null").map { null }.trim()
    private val objList = jsonObj.and(Skip(comma).and(jsonObj).many0()).map(::reduceList)
    private val arr: Parser<List<Any?>> = Skip(arrStart).and(objList.opt(emptyList())).skip(arrEnd)
    private val pair = string.skip(colon).and(jsonObj)
    private val pairList = pair.and(Skip(comma).and(pair).many0()).map(::reduceList)
    private val obj: Parser<Map<String, Any?>> =
        Skip(objStart).and(pairList.opt(emptyList())).skip(objEnd).map { it.toMap() }

    private inline fun <reified T> reduceList(p: Pair<T, List<T>>): List<T> =
        listOf(p.first, *p.second.toTypedArray())

    fun parse(s: String) = jsonObj.end().eval(s)
}

class JsonParserTest {
    @Test
    fun test1() {
        assertEquals("", JsonParser.parse("  \"\"  "))
        assertEquals(123, JsonParser.parse("  123  "))
        assertEquals(3.14, JsonParser.parse("  3.14  "))
        assertEquals(true, JsonParser.parse("  true  "))
        assertEquals(false, JsonParser.parse("  false  "))
        assertNull(JsonParser.parse("null"))
        assertEquals("hello!", JsonParser.parse("  \"hello!\"  "))
        assertEquals(emptyList<Any>(), JsonParser.parse(" [] "))
        assertEquals(emptyList<Any>(), JsonParser.parse(" [ ] "))
        assertEquals(emptyMap<Any, Any>(), JsonParser.parse(" {} "))
        assertEquals(emptyMap<Any, Any>(), JsonParser.parse(" { } "))
        assertEquals(listOf(emptyMap<Any, Any>()), JsonParser.parse(" [ { } ] "))

        val json = """
            {
                "escaped": "\ttest\u1234\ntest",
                "null": null,
                "a": +123,
                "b": -3.14e-1,
                "c": "hello",
                "d": {
                    "x": 100,
                    "y": "world!"
                },
                "e": [
                    12,
                    34.56,
                    {
                        "name": "Xiao Ming",
                        "age": 18,
                        "score": [99.8, 87.5, 60.0]
                    },
                    "abc"
                ],
                "f": [],
                "g": {},
                "h": [true, {"m": false}]
            }
            """
        val map = mapOf(
            "escaped" to "\ttest\u1234\ntest",
            "null" to null,
            "a" to 123,
            "b" to -0.314,
            "c" to "hello",
            "d" to mapOf(
                "x" to 100,
                "y" to "world!"
            ),
            "e" to listOf(
                12,
                34.56,
                mapOf(
                    "name" to "Xiao Ming",
                    "age" to 18,
                    "score" to listOf(99.8, 87.5, 60.0)
                ),
                "abc"
            ),
            "f" to emptyList<Any>(),
            "g" to emptyMap<Any, Any>(),
            "h" to listOf(true, mapOf("m" to false))
        )
        assertEquals(map, JsonParser.parse(json))

        assertThrows<InternalParseException> { JsonParser.parse("{") }
        assertThrows<InternalParseException> { JsonParser.parse("{}}") }
        assertThrows<InternalParseException> { JsonParser.parse("[{]}") }
        assertThrows<InternalParseException> { JsonParser.parse("[1 2 3]") }
        assertThrows<InternalParseException> { JsonParser.parse("[1,2,3],4") }
        assertThrows<InternalParseException> { JsonParser.parse("") }
    }

    @Test
    fun test2() {
        val jsonString = TestUtils.getResourceAsString("test.json")
        val naive = NaiveJsonParser()
        val m1 = naive.parse(jsonString)
        val m2 = JsonParser.parse(jsonString)
        assertEquals(m1.toString(), m2.toString())

        val start1 = System.currentTimeMillis()
        (0 until 1000).forEach {
            naive.parse(jsonString)
        }
        val end1 = System.currentTimeMillis()
        println("naive parser: ${end1 - start1} ms")

        val start2 = System.currentTimeMillis()
        (0 until 1000).forEach {
            JsonParser.parse(jsonString)
        }
        val end2 = System.currentTimeMillis()
        println("parserc: ${end2 - start2} ms")
    }
}