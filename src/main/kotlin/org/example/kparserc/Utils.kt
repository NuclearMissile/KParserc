@file:Suppress("NOTHING_TO_INLINE", "FunctionName")

package org.example.kparserc

import java.util.concurrent.ConcurrentHashMap

val REGEX_CACHE = ConcurrentHashMap<String, Regex>()

class InternalParseException(
    private val input: String, private val index: Int,
) : RuntimeException(null, null, false, false) {
    fun toParseException(msg: String): ParseException = ParseException(msg, input, index)
}

class ParseException(msg: String, input: String? = null, index: Int? = null) : RuntimeException() {
    override val message = if (input == null || index == null) msg else {
        var row = 1
        var col = 1
        var i = 0
        while (i <= index && index < input.length) {
            if (input[i] == '\n') {
                row++
                col = 1
            }
            col++
            i++
        }
        "$row:$col $msg"
    }
}

data class ParseResult<out R>(val result: R, val index: Int)

object PlaceHolder

inline fun <R> Fail(): Parser<R> = Parser { s, index -> throw InternalParseException(s, index) }

inline fun <R> Empty(result: R): Parser<R> = Parser { _, index -> ParseResult(result, index) }

inline fun <R> End(result: R): Parser<R> = Parser { s, index ->
    if (index != s.length) throw InternalParseException(s, index)
    ParseResult(result, index)
}

inline fun End(): Parser<Any> = Parser { s, index ->
    if (index != s.length) throw InternalParseException(s, index)
    ParseResult(PlaceHolder, index)
}

inline fun Match(pattern: String): Parser<String> = Parser { s, index ->
    val m = REGEX_CACHE.getOrPut(pattern) { pattern.toRegex() }.matchAt(s, index)
    if (m != null) {
        return@Parser ParseResult(m.value, index + m.value.length)
    }
    throw InternalParseException(s, index)
}

inline fun Ch(crossinline predicate: (Char) -> Boolean): Parser<Char> = Parser { s, index ->
    if (index >= s.length || !predicate(s[index])) throw InternalParseException(s, index)
    ParseResult(s[index], index + 1)
}

inline fun Ch(c: Char): Parser<Char> = Ch { ch -> ch == c }

inline fun AnyCh(): Parser<Char> = Ch { true }

inline fun Range(c1: Char, c2: Char): Parser<Char> = Ch { c -> (c - c1) * (c - c2) <= 0 }

inline fun WhiteSpace() = Chs(' ', '\t', '\r', '\n')

inline fun Digit() = Range('0', '9')

inline fun Alpha() = Range('a', 'z').or(Range('A', 'Z'))

inline fun Chs(vararg chs: Char): Parser<Char> = Ch { c ->
    if (chs.size <= 8) chs.contains(c) else chs.toSet().contains(c)
}

inline fun NotChs(vararg chs: Char): Parser<Char> = Ch { c ->
    if (chs.size <= 8) !chs.contains(c) else !chs.toSet().contains(c)
}

inline fun Not(test: Parser<Any>): Parser<Any> = Parser { s, index ->
    try {
        test.parse(s, index)
    } catch (_: InternalParseException) {
        return@Parser ParseResult(PlaceHolder, index)
    }
    throw InternalParseException(s, index)
}

inline fun Str(str: String): Parser<String> = Parser { s, index ->
    if (!s.startsWith(str, index)) throw InternalParseException(s, index)
    ParseResult(str, index + str.length)
}

inline fun Strs(vararg strs: String): Parser<String> = strs.fold(Fail()) { p, s -> p.or(Str(s)) }

inline fun Seq(vararg parsers: Parser<Any>): Parser<List<Any>> = Parser { s, index ->
    var currIndex = index
    val result = parsers.map {
        val r = it.parse(s, currIndex)
        currIndex = r.index
        r.result
    }
    ParseResult(result, currIndex)
}

inline fun <R> OneOf(vararg parsers: Parser<R>): Parser<R> = Parser { s, index ->
    for (parser in parsers) try {
        return@Parser parser.parse(s, index)
    } catch (_: InternalParseException) {
    }
    throw InternalParseException(s, index)
}

inline fun Alt(vararg parsers: Parser<Any>): Parser<Any> = Parser { s, index ->
    for (parser in parsers) try {
        return@Parser parser.cast(Any::class.java).parse(s, index)
    } catch (_: InternalParseException) {
    }
    throw InternalParseException(s, index)
}

inline fun <R> Lazy(crossinline parserSupplier: () -> Parser<R>): Parser<R> = Parser { s, index ->
    parserSupplier().parse(s, index)
}

inline fun <R> Expect(test: Parser<Any>, value: R): Parser<R> = Parser { s, index ->
    test.parse(s, index)
    ParseResult(value, index)
}

class SkipWrapper<R>(private val lhs: Parser<R>) {
    fun <R2> and(rhs: Parser<R2>): Parser<R2> = lhs.and(rhs).map { it.second }
}

inline fun <R> Skip(lhs: Parser<R>): SkipWrapper<R> = SkipWrapper(lhs)

inline fun SkipAll(vararg parsers: Parser<Any>): SkipWrapper<out Any> = SkipWrapper(Seq(*parsers))