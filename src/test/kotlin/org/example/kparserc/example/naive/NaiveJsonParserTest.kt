package org.example.kparserc.example.naive

import org.junit.jupiter.api.Test

class NaiveJsonParser {
    companion object {
        fun toJSON(obj: Any?): String {
            return when (obj) {
                is String -> {
                    "\"$obj\""
                }

                null, is Number, is Boolean -> {
                    obj.toString()
                }

                is Iterable<*> -> {
                    val list = obj.joinToString(", ") { toJSON(it) }
                    "[$list]"
                }

                is Map<*, *> -> {
                    val pairs = obj.entries.joinToString(", ") { (key, value) ->
                        "\"$key\": ${toJSON(value)}"
                    }
                    "{$pairs}"
                }

                else -> {
                    val pairs = obj.javaClass.declaredFields.joinToString(", ") { field ->
                        field.isAccessible = true
                        "\"${field.name}\": ${toJSON(field.get(obj))}"
                    }
                    "{$pairs}"
                }
            }
        }
    }

    private lateinit var jsonString: String
    private var currentIndex = 0

    fun parse(json: String): Any? {
        jsonString = json
        currentIndex = 0
        skipWhiteSpace()
        return parseValue()
    }

    private fun parseValue(): Any? {
        return when (val nextChar = peek()) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't', 'f' -> parseBoolean()
            'n' -> parseNull()
            in '0'..'9', '-', '.' -> parseNumber()
            else -> throw IllegalStateException("Unexpected character: $nextChar")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        val obj = mutableMapOf<String, Any?>()
        match('{')
        skipWhiteSpace()
        while (peek() != '}') {
            val key = parseString()
            skipWhiteSpace()
            match(':')
            skipWhiteSpace()
            val value = parseValue()
            obj[key] = value
            skipWhiteSpace()
            if (peek() == ',') {
                match(',')
                skipWhiteSpace()
            }
        }
        match('}')
        return obj
    }

    private fun parseArray(): List<Any?> {
        val array = mutableListOf<Any?>()
        match('[')
        skipWhiteSpace()
        while (peek() != ']') {
            val value = parseValue()
            array.add(value)
            skipWhiteSpace()
            if (peek() == ',') {
                match(',')
                skipWhiteSpace()
            }
        }
        match(']')
        return array
    }

    private fun parseString(): String {
        match('"')
        val stringBuilder = StringBuilder()
        while (peek() != '"') {
            val char = next()
            if (char == '\\') {
                when (val escape = next()) {
                    '"' -> stringBuilder.append('"')
                    '\\' -> stringBuilder.append('\\')
                    '/' -> stringBuilder.append('/')
                    'b' -> stringBuilder.append('\b')
                    'f' -> stringBuilder.append('\u000C')
                    'n' -> stringBuilder.append('\n')
                    'r' -> stringBuilder.append('\r')
                    't' -> stringBuilder.append('\t')
                    'u' -> {
                        val unicode = jsonString.substring(currentIndex..currentIndex + 3)
                        currentIndex += 4
                        stringBuilder.append(Integer.parseInt(unicode, 16).toChar())
                    }

                    else -> throw IllegalStateException("Invalid escape character: $escape")
                }
            } else {
                stringBuilder.append(char)
            }
        }
        match('"')
        return stringBuilder.toString()
    }

    private fun parseBoolean(): Boolean {
        return when (val nextChar = peek()) {
            't' -> {
                match("true")
                true
            }

            'f' -> {
                match("false")
                false
            }

            else -> throw IllegalStateException("Expected 't' or 'f', but found  '$nextChar'")
        }
    }

    private fun parseNull(): Any? {
        match("null")
        return null
    }

    private fun parseNumber(): Number {
        val start = currentIndex
        while ((peek() ?: '$') in "1234567890+-.eE") {
            next()
        }
        val numberString = jsonString.substring(start, currentIndex)
        return when {
            numberString.contains('.') -> numberString.toDouble()
            else -> numberString.toLong()
        }
    }

    private fun peek(): Char? = jsonString.getOrNull(currentIndex)

    private fun next(): Char {
        val nextChar = peek() ?: throw IllegalStateException("Unexpected end of JSON")
        currentIndex++
        return nextChar
    }

    private fun match(expected: String) {
        for (char in expected) match(char)
    }

    private fun match(expected: Char) {
        val nextChar = next()
        if (nextChar != expected) {
            throw IllegalStateException("Expected '$expected' but found '$nextChar'")
        }
    }

    private fun skipWhiteSpace() {
        while (peek()?.isWhitespace() == true) {
            next()
        }
    }
}

class NaiveJsonParserTest {
    data class TestObj(val message: String, val payload: Any? = null)

    @Test
    fun testParse() {
        val json = """
        {
            "name": "John",
            "age": -.30e16,
            "isStudent": false,
            "isTeacher": true,
            "grades": [90, 85, 95],
            "address": {
                "city": "New York",
                "zip": "10001"
            },
            "languages": ["English", "Spanish", "French"],
            "contact": null
        }
        """.trimIndent()
        val obj = NaiveJsonParser().parse(json)
        println(obj)
        println(NaiveJsonParser.toJSON(obj))
        println(NaiveJsonParser.toJSON(TestObj("test", TestObj("inner"))))
    }
}
