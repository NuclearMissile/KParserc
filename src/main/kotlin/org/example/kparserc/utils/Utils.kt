package org.example.kparserc.utils

import org.example.kparserc.Parser

class ParseInternalException private constructor() : RuntimeException(null, null, false, false) {
    companion object {
        val INSTANCE = ParseInternalException()
    }
}

class ParseException(
    private val msg: String, private val input: String? = null, private val index: Int? = null,
) : RuntimeException() {
    override val message: String
        get() = if (rowAndCol == null) msg else "at (${rowAndCol!!.first}, ${rowAndCol!!.second}), msg: $msg"

    private val rowAndCol: Pair<Int, Int>?
        get() {
            if (input == null || index == null) return null

            var row = 1
            var col = 0
            var i = 0
            while (i <= index && index < input.length) {
                if (input[i] == '\n') {
                    row++
                    col = 0
                }
                col++
                i++
            }
            return Pair(row, col)
        }
}

data class ParseResult<R>(val result: R, val index: Int)

fun ch(predicate: (Char) -> Boolean): Parser<Char> = Parser { s, index ->
    if (index >= s.length || !predicate(s[index])) throw ParseInternalException.INSTANCE
    ParseResult(s[index], index + 1)
}

fun ch(c: Char): Parser<Char> = ch { ch -> ch == c }

fun any(): Parser<Char> = ch { _ -> true }

fun range(c1: Char, c2: Char): Parser<Char> = ch { c -> (c - c1) * (c - c2) <= 0 }

fun chs(vararg chs: Char): Parser<Char> = ch { c ->
    if (chs.size <= 8) chs.contains(c) else chs.toSet().contains(c)
}

fun not(vararg chs: Char): Parser<Char> = ch { c ->
    if (chs.size <= 8) !chs.contains(c) else !chs.toSet().contains(c)
}

fun str(str: String): Parser<String> = Parser { s, index ->
    if (!s.startsWith(str, index)) throw ParseInternalException.INSTANCE
    ParseResult(str, index + str.length)
}

fun strs(vararg strs: String): Parser<String> = Parser { s, index ->
    for (str in strs) try {
        return@Parser str(str).parse(s, index)
    } catch (_: ParseInternalException) {
    }
    throw ParseInternalException.INSTANCE
}

fun seq(vararg parsers: Parser<out Any>): Parser<List<Any>> = Parser { s, index ->
    var currIndex = index
    val result = parsers.map {
        val r = it.parse(s, currIndex)
        currIndex = r.index
        r.result
    }
    ParseResult(result, currIndex)
}

@SafeVarargs
@Suppress("UNCHECKED_CAST")
fun <R> oneOf(vararg parsers: Parser<out R>): Parser<R> = Parser { s, index ->
    for (parser in parsers) try {
        return@Parser parser.parse(s, index) as ParseResult<R>
    } catch (_: ParseInternalException) {
    }
    throw ParseInternalException.INSTANCE
}

fun <R> lazy(parserSupplier: () -> Parser<R>): Parser<R> = Parser { s, index ->
    parserSupplier().parse(s, index)
}

fun expect(test: Parser<out Any>): Parser<Boolean> = Parser { s, index ->
    test.parse(s, index)
    ParseResult(true, index)
}

fun not(test: Parser<out Any>): Parser<Boolean> = Parser { s, index ->
    try {
        test.parse(s, index)
    } catch (_: ParseInternalException) {
        return@Parser ParseResult(true, index)
    }
    throw ParseInternalException.INSTANCE
}

class SkipWrapper<R>(private val lhs: Parser<R>) {
    fun <R2> and(rhs: Parser<R2>): Parser<R2> = lhs.and(rhs).map { p -> p.second }
}

fun <R> skip(lhs: Parser<R>): SkipWrapper<R> = SkipWrapper(lhs)