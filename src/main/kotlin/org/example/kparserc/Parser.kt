package org.example.kparserc

import org.example.kparserc.utils.*

fun interface Parser<R> {

    fun parse(s: String, index: Int): ParseResult<R>

    fun parse(s: String): R {
        val r = parse(s, 0)
        if (r.index != s.length) {
            throw ParseInternalException.INSTANCE
        }
        return r.result
    }

    fun <R2> and(rhs: Parser<R2>): Parser<Pair<R, R2>> = Parser { s, index ->
        val r1 = parse(s, index)
        val r2 = rhs.parse(s, r1.index)
        ParseResult(Pair(r1.result, r2.result), r2.index)
    }

    fun and(c: Char): Parser<Pair<R, Char>> = and(ch(c))

    fun and(s: String): Parser<Pair<R, String>> = and(str(s))

    fun or(rhs: Parser<R>): Parser<R> = Parser { s, index ->
        try {
            parse(s, index)
        } catch (_: ParseInternalException) {
            rhs.parse(s, index)
        }
    }

    fun <R2> map(mapper: (R) -> R2) = Parser { s, index ->
        val r = parse(s, index)
        ParseResult(mapper(r.result), r.index)
    }

    fun <R2> value(r2: R2): Parser<R2> = map { _ -> r2 }

    fun repeat(min: Int, max: Int? = null): Parser<List<R>> = Parser { s, index ->
        val _max = max ?: min
        var count = 0
        var currIndex = index
        val result = mutableListOf<R>()

        while (count < min) {
            val r = parse(s, currIndex)
            result += r.result
            currIndex = r.index
            count++
        }

        while (count < _max || _max < 0) {
            try {
                val r = parse(s, currIndex)
                result += r.result
                currIndex = r.index
                count++
            } catch (_: ParseInternalException) {
                break
            }
        }

        ParseResult(result, currIndex)
    }

    fun many0(): Parser<List<R>> = repeat(0, -1)

    fun many1(): Parser<List<R>> = repeat(1, -1)

    fun <R2> skip(rhs: Parser<R2>): Parser<R> = and(rhs).map { p -> p.first }

    fun surround(prefix: Parser<*>, suffix: Parser<*>? = null): Parser<R> =
        org.example.kparserc.utils.skip(prefix).and(this).skip(suffix ?: prefix)

    fun trim(): Parser<R> = surround(chs(' ', '\t', '\r', '\n').many0())

    fun optional(default: R): Parser<R> = Parser { s, index ->
        try {
            parse(s, index)
        } catch (_: ParseInternalException) {
            ParseResult(default, index)
        }
    }

    fun <R2> flatMap(mapper: (ParseResult<R>) -> Parser<R2>): Parser<Pair<R, R2>> = Parser { s, index ->
        val r1 = parse(s, index)
        val r2 = mapper(r1).parse(s, r1.index)
        ParseResult(Pair(r1.result, r2.result), r2.index)
    }

    fun fetal(exceptionMapper: (String, Int) -> RuntimeException): Parser<R> = Parser { s, index ->
        try {
            parse(s, index)
        } catch (_: ParseInternalException) {
            throw exceptionMapper(s, index)
        }
    }
}