package org.example.kparserc.example.naive

import org.example.kparserc.TestUtils
import org.example.kparserc.XMLNode
import org.example.kparserc.printXMLNode
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test

class NaiveXMLParser {
    private var currIndex = 0
    private lateinit var xmlString: String

    fun parse(input: String): XMLNode {
        xmlString = input
        currIndex = 0
        return parseElement()
    }

    private fun parseElement(): XMLNode.Element {
        skipWhiteSpace()
        match('<')
        val tagName = parseName()
        val attributes = parseAttributes()
        skipWhiteSpace()

        if (peek() == '/') {
            // Self-closing tag
            match("/>")
            return XMLNode.Element(tagName, attributes, emptyList())
        }

        match('>')

        val children = mutableListOf<XMLNode>()
        while (true) {
            skipWhiteSpace()
            if (xmlString.startsWith("</", currIndex)) break

            when {
                xmlString.startsWith("<!--", currIndex) -> skipComment()
                xmlString[currIndex] == '<' -> children.add(parseElement())
                else -> children.add(parseText())
            }
        }

        // Closing tag
        match("</")
        val closingTagName = parseName()
        if (closingTagName != tagName) {
            throw IllegalArgumentException("Mismatched closing tag: expected $tagName, found $closingTagName")
        }
        match('>')

        return XMLNode.Element(tagName, attributes, children)
    }

    private fun parseAttributes(): Map<String, String> {
        val attributes = mutableMapOf<String, String>()
        while (true) {
            skipWhiteSpace()
            if (peek() == '>' || peek() == '/') break
            val name = parseName()
            if (attributes.containsKey(name)) {
                throw IllegalArgumentException("Duplicate attribute key: $name")
            }
            match('=')
            val value = parseAttributeValue()
            attributes[name] = value
        }
        return attributes
    }

    private fun parseAttributeValue(): String {
        val quote = next()
        if (quote != '"' && quote != '\'') {
            throw IllegalArgumentException("Expected quote for attribute value")
        }
        val value = StringBuilder()
        while (peek() != quote) {
            value.append(next())
        }
        match(quote)
        return value.toString()
    }

    private fun skipComment() {
        match("<!--")
        while (!xmlString.startsWith("-->", currIndex)) {
            next()
        }
        match("-->")
    }

    private fun parseText(): XMLNode.Text {
        val text = StringBuilder()
        while (peek() != '<' || xmlString.startsWith("<!--", currIndex)) {
            if (xmlString.startsWith("<!--", currIndex)) {
                skipComment()
                continue
            }
            text.append(next())
        }
        return XMLNode.Text(text.toString().trim())
    }

    private fun parseName(): String {
        var peek = peek()
        if (!peek.isLetterOrDigit() && peek !in "_:")
            throw IllegalArgumentException("Unexpected character \"$peek\"")
        val name = StringBuilder()
        while (peek.isLetterOrDigit() || peek in "-_:") {
            name.append(next())
            peek = peek()
        }
        return name.toString()
    }

    private fun skipWhiteSpace() {
        while (peek().isWhitespace()) {
            next()
        }
    }

    private fun peek(): Char = xmlString.getOrNull(currIndex) ?: '\u0000'

    private fun next(): Char {
        val nextChar = peek()
        if (nextChar == '\u0000') throw IllegalArgumentException("Unexpected end of XML")
        currIndex++
        return nextChar
    }

    private fun match(char: Char) {
        if (next() != char) throw IllegalArgumentException("Expected '$char'")
    }

    private fun match(s: String) = s.forEach { match(it) }
}

class NaiveXMLParserTest {
    @Test
    fun test() {
        val xmlString = """
        <root attr1="value1" attr2="value2">
            <!-- This is a comment -->
            <child1>
                Text content
            </child1>
            <child2 attr3="value3"/>
            <child3>
                <grand-child>
                    Nested content
                </grand-child>
            </child3>
        </root>
        """.trimIndent()

        val parser = NaiveXMLParser()
        val rootNode = parser.parse(xmlString)
        printXMLNode(rootNode)

        // This should throw an exception
        val invalidXmlString = """
        <root attr1="value1" attr1="value2">
            <child1>Text content</child1>
        </root>
        """.trimIndent()

        assertThrows<IllegalArgumentException> { parser.parse(invalidXmlString) }
    }

    @Test
    fun test2() {
        val testXML = TestUtils.getResourceAsString("test.xml")
        val parser = NaiveXMLParser()
        val rootNode = parser.parse(testXML)
        printXMLNode(rootNode)
    }
}