# KParserc

### Yet another parser combinator library in Kotlin. 

- *Example: expr calculator*

```kotlin
object ExprCalc {
    private val add = Ch('+').trim()
    private val sub = Ch('-').trim()
    private val mul = Ch('*').trim()
    private val div = Ch('/').trim()
    private val lp = Ch('(').trim()
    private val rp = Ch(')').trim()
    private val comma = Ch(',').trim()

    // const definition example: PI
    private val PI: Parser<Double> = Str("PI").map { Math.PI }.trim()

    // function definition example: pow and log
    private val POW: Parser<Double> = SkipAll(Str("pow"), lp)
        .and(Lazy { fact })
        .skip(comma)
        .and(Lazy { fact })
        .skip(rp)
        .map { it.first.pow(it.second) }
    private val LOG: Parser<Double> = SkipAll(Str("log"), lp)
        .and(Lazy { fact })
        .skip(comma)
        .and(Lazy { fact })
        .skip(rp)
        .map { log(it.first, it.second) }

    private val integer = Match("\\d+").map { it.toDouble() }
    private val decimal = Match("\\d*\\.\\d+").map { it.toDouble() }
    private val number = decimal.or(integer).trim()
    private val bracketExpr: Parser<Double> = Skip(lp).and(Lazy { expr }).skip(rp)
    private val negFact: Parser<Double> = Skip(sub).and(Lazy { fact }).map { -it }
    private val fact = OneOf(number, bracketExpr, negFact, PI, POW, LOG)
    private val term = fact.and(mul.or(div).and(fact).many0()).map(::calc)
    private val expr = term.and(add.or(sub).and(term).many0()).map(::calc)

    private fun calc(p: Pair<Double, List<Pair<Char, Double>>>): Double = p.second.fold(p.first) { acc, pair ->
        when (pair.first) {
            '+' -> acc + pair.second
            '-' -> acc - pair.second
            '*' -> acc * pair.second
            '/' -> acc / pair.second
            else -> throw IllegalArgumentException("Unexpected symbol: ${pair.first}")
        }
    }

    fun eval(s: String) = expr.end()
        .fatal { input, index -> ParseException("Unexpect character found", input, index) }
        .eval(s)
}
```

- *Example: JSON parser*

```kotlin
object JsonParser {
    private val objStart = Ch('{').trim()
    private val objEnd = Ch('}').trim()
    private val arrStart = Ch('[').trim()
    private val arrEnd = Ch(']').trim()
    private val colon = Ch(':').trim()
    private val comma = Ch(',').trim()
    private val jsonObj = OneOf(
        Lazy { decimal },
        Lazy { integer },
        Lazy { string },
        Lazy { boolean },
        Lazy { _null },
        Lazy { arr },
        Lazy { obj },
    )

    private val integer = Match("[+\\-]?\\d+").map { it.toInt() }.trim()
    private val decimal = Match("[+\\-]?\\d*\\.\\d+([eE][+-]?[0-9]+)?").map { it.toDouble() }.trim()
    private val string = Match("\"[^\"\\\\]*(\\\\.[^\"\\\\]*)*\"").map { s ->
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
    private val boolean = Strs("true", "false").map { it.toBoolean() }.trim()
    private val _null = Str("null").map { null }.trim()
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
```

- *Example: XML parser*

```kotlin
object XMLParser {
    // Parser combinators for XML
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
```