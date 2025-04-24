package org.example.kparserc.example

import org.example.kparserc.*
import org.example.kparserc.example.naive.NaiveXMLParser
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.trim

object XMLParser {
    // Parser combinators for XML
    val comment = Skip(Str("<!--")).and(Not(Str("-->")).and(AnyCh()).many0()).skip(Str("-->"))
    val ignores = Alt(WhiteSpace(), comment).many0()

    val xmlName = Match("""[a-zA-Z_][a-zA-Z-_:\d]*""")
    val attrValue = Match("""('[^']+')|("[^"]+")""").map { it.trim('\'', '"') }
    val attribute = xmlName.trim().skip(Ch('=')).and(attrValue.trim())
    val attributes = attribute.many0().map {
        val ret = mutableMapOf<String, String>()
        for (p in it) {
            if (ret.containsKey(p.first))
                throw ParseException("Attribute key ${p.first} is duplicate.")
            ret[p.first] = p.second
        }
        ret
    }
    val openTag = Skip(Ch('<')).and(xmlName).and(attributes).skip(Ch('>')).surround(ignores)
    val closeTag = Skip(Str("</")).and(xmlName).skip(Ch('>')).surround(ignores)
    val selfCloseTag = Skip(Ch('<')).and(xmlName).and(attributes).skip(Str("/>")).surround(ignores)
    val text = NotChs('<').surround(comment.many0()).many1().map { XMLNode.Text(it.joinToString("").trim()) }

    val xmlElement: Parser<XMLNode.Element> = OneOf(
        openTag.flatMap {
            val name = it.result.first
            val attrs = it.result.second
            xmlNode.many0().and(closeTag).map { (children, closeName) ->
                if (name != closeName) throw ParseException("Mismatched tags: <$name> and </$closeName>")
                XMLNode.Element(name, attrs, children)
            }
        }.map { it.second },
        selfCloseTag.map { XMLNode.Element(it.first, it.second, emptyList()) }
    )

    val xmlNode: Parser<XMLNode> = OneOf(xmlElement, text)

    fun parse(input: String): XMLNode.Element = xmlElement.surround(ignores).end().eval(input)
}

class XMLParserTest {
    @Test
    fun test() {
        println(XMLParser.xmlName.end().eval("tag1"))
        println(XMLParser.attrValue.end().eval("'value1'"))
        println(XMLParser.attribute.end().eval("key1='value1'"))
        println(XMLParser.attribute.end().eval("key2=\"value2\""))
        println(XMLParser.parse("<root></root>"))

        assertThrows<ParseException> { XMLParser.parse("<root attr1='a' attr1='b'></root>") }
        assertThrows<ParseException> { XMLParser.parse("<root></ROOT>") }

        val xmlString = """
        <root attr1="value1" attr2='value2'>
            <!-- This is a comment -->
            <child1>Text content</child1> <!-- This is a comment -->
            <child2 attr3='value3'/> <!-- This is a comment -->
            <child3>
                <grandchild>
                    <!-- This is a comment -->N<!-- This is a comment -->ested<!-- This is a comment --> 
                    <!-- This is a comment -->content<!-- This is a comment -->
                </grandchild>
            </child3>
        </root><!-- This is a comment -->
        """.trimIndent()
        println(printXMLNode(XMLParser.parse(xmlString)))

        println(printXMLNode(NaiveXMLParser().parse(xmlString)))
    }

    @Test
    fun test1() {
        val xmlString = TestUtils.getResourceAsString("test.xml")
        val naive = NaiveXMLParser()
        val r1 = naive.parse(xmlString)
        val r2 = XMLParser.parse(xmlString)

        assertEquals(printXMLNode(r1), printXMLNode(r2))

        val start1 = System.currentTimeMillis()
        (0 until 1000).forEach {
            naive.parse(xmlString)
        }
        val end1 = System.currentTimeMillis()
        println("naive parser: ${end1 - start1} ms")

        val start2 = System.currentTimeMillis()
        (0 until 1000).forEach {
            XMLParser.parse(xmlString)
        }
        val end2 = System.currentTimeMillis()
        println("parserc: ${end2 - start2} ms")
    }
}