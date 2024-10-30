package org.example.kparserc

import org.example.kparserc.TestUtils.getResourceAsString
import org.example.kparserc.naive.NaiveXMLParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.text.trim

class XMLParser {
    // Parser combinators for XML
    companion object {
        val comment = Skip(Str("<!--")).and(Not(Str("-->")).and(AnyCh()).many0()).skip(Str("-->"))
        val ignores = Alt(WhiteSpace(), comment).many0()

        val xmlName = Match("[a-zA-Z_][a-zA-Z-_:\\d]*")
        val attrValue = Match("('[^']+')|(\"[^\"]+\")").map { it.trim('\'', '"') }
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
        val openingTag = Skip(Ch('<')).and(xmlName).and(attributes).skip(Ch('>')).surround(ignores)
        val closingTag = Skip(Str("</")).and(xmlName).skip(Ch('>')).surround(ignores)
        val selfClosingTag = Skip(Ch('<')).and(xmlName).and(attributes).skip(Str("/>")).surround(ignores)
        val textContent =
            NotChs('<').surround(comment.many0()).many1().map { XMLNode.Text(it.joinToString("").trim()) }

        val xmlElement: Parser<XMLNode.Element> = OneOf(
            openingTag.flatMap {
                val name = it.result.first
                val attrs = it.result.second
                xmlNode.many0().and(closingTag).map { (content, closeName) ->
                    if (name != closeName) throw ParseException("Mismatched tags: <$name> and </$closeName>")
                    XMLNode.Element(name, attrs, content)
                }
            }.map { it.second },
            selfClosingTag.map { XMLNode.Element(it.first, it.second, emptyList()) }
        )

        val xmlNode: Parser<XMLNode> = OneOf(xmlElement, textContent)

        fun parse(input: String): XMLNode.Element = xmlElement.surround(ignores).end().eval(input)
    }
}

class XMLParserTest {
    @Test
    fun test() {
        println(XMLParser.Companion.xmlName.end().eval("tag1"))
        println(XMLParser.Companion.attrValue.end().eval("'value1'"))
        println(XMLParser.Companion.attribute.end().eval("key1='value1'"))
        println(XMLParser.Companion.parse("<root></root>"))

        val xmlString = """
        <root attr1="value1" attr2='value2'>
            <!-- This is a comment -->
            <child1>Text content</child1> <!-- This is a comment -->
            <child2 attr3='value3'/> <!-- This is a comment -->
            <child3>
                <grandchild><!-- This is a comment -->N<!-- This is a comment -->ested<!-- This is a comment --> <!-- This is a comment -->content<!-- This is a comment --></grandchild>
            </child3>
        </root><!-- This is a comment -->
        """.trimIndent()
        println(printXMLNode(XMLParser.Companion.parse(xmlString)))

        println(printXMLNode(NaiveXMLParser().parse(xmlString)))
    }

    @Test
    fun test1() {
        val xmlString = getResourceAsString("test.xml")
        val naive = NaiveXMLParser()
        val r1 = naive.parse(xmlString)
        val r2 = XMLParser.Companion.parse(xmlString)

        assertEquals(printXMLNode(r1), printXMLNode(r2))

        val start1 = System.currentTimeMillis()
        (0 until 1000).forEach {
            naive.parse(xmlString)
        }
        val end1 = System.currentTimeMillis()
        println("naive parser: ${end1 - start1} ms")

        val start2 = System.currentTimeMillis()
        (0 until 1000).forEach {
            XMLParser.Companion.parse(xmlString)
        }
        val end2 = System.currentTimeMillis()
        println("parserc: ${end2 - start2} ms")
    }
}