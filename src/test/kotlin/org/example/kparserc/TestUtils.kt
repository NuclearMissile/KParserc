package org.example.kparserc

import kotlin.jvm.javaClass
import kotlin.text.decodeToString

object TestUtils {
    fun getResourceAsString(path: String): String {
        return javaClass.classLoader.getResourceAsStream(path)!!.readAllBytes().decodeToString()
    }
}

sealed class XMLNode {
    data class Element(val name: String, val attributes: Map<String, String>, val children: List<XMLNode>) : XMLNode()
    data class Text(val content: String) : XMLNode()
}

fun printXMLNode(node: XMLNode, sb: StringBuilder = StringBuilder(), indent: String = ""): String {
    when (node) {
        is XMLNode.Element -> {
            sb.append("$indent<${node.name}")
            if (node.attributes.isNotEmpty()) {
                node.attributes.forEach { (key, value) ->
                    sb.append(" $key=\"$value\"")
                }
            }
            sb.appendLine(">")
            node.children.forEach { printXMLNode(it, sb, "$indent\t") }
            sb.appendLine("$indent</${node.name}>")
        }

        is XMLNode.Text -> sb.appendLine("$indent${node.content}")
    }
    return sb.toString()
}
