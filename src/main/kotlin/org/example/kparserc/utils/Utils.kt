package org.example.kparserc.utils

import org.example.kparserc.Parser

class InternalException private constructor() : RuntimeException(null, null, false, false) {
    companion object {
        val INSTANCE = InternalException()
    }
}

class ParseException(msg: String, input: String? = null, index: Int? = null) : RuntimeException() {
    override val message = if (input == null || index == null) msg else {
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
        "$col,$row: $msg"
    }
}

data class ParseResult<out R>(val result: R, val index: Int)

inline fun ch(crossinline predicate: (Char) -> Boolean): Parser<Char> = Parser { s, index ->
    if (index >= s.length || !predicate(s[index])) throw InternalException.INSTANCE
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
    if (!s.startsWith(str, index)) throw InternalException.INSTANCE
    ParseResult(str, index + str.length)
}

fun strs(vararg strs: String): Parser<String> = Parser { s, index ->
    for (str in strs) try {
        return@Parser str(str).parse(s, index)
    } catch (_: InternalException) {
    }
    throw InternalException.INSTANCE
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

fun <R> oneOf(vararg parsers: Parser<out R>): Parser<R> = Parser { s, index ->
    for (parser in parsers) try {
        return@Parser parser.parse(s, index)
    } catch (_: InternalException) {
    }
    throw InternalException.INSTANCE
}

inline fun <R> lazy(crossinline parserSupplier: () -> Parser<R>): Parser<R> = Parser { s, index ->
    parserSupplier().parse(s, index)
}

fun expect(test: Parser<out Any>): Parser<Boolean> = Parser { s, index ->
    test.parse(s, index)
    ParseResult(true, index)
}

fun not(test: Parser<out Any>): Parser<Boolean> = Parser { s, index ->
    try {
        test.parse(s, index)
    } catch (_: InternalException) {
        return@Parser ParseResult(true, index)
    }
    throw InternalException.INSTANCE
}

class SkipWrapper<R>(private val lhs: Parser<R>) {
    fun <R2> and(rhs: Parser<R2>): Parser<R2> = lhs.and(rhs).map { p -> p.second }
}

fun <R> skip(lhs: Parser<R>): SkipWrapper<R> = SkipWrapper(lhs)