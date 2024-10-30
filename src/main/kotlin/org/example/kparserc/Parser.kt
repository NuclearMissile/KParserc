@file:Suppress("NOTHING_TO_INLINE")

package org.example.kparserc

fun interface Parser<out R> {
    fun parse(s: String, index: Int): ParseResult<R>
}

inline fun <R> Parser<R>.eval(s: String): R = parse(s, 0).result

inline fun <R, R2> Parser<R>.and(rhs: Parser<R2>): Parser<Pair<R, R2>> = Parser { s, index ->
    val r1 = parse(s, index)
    val r2 = rhs.parse(s, r1.index)
    ParseResult(Pair(r1.result, r2.result), r2.index)
}

inline fun <R> Parser<R>.and(s: String): Parser<Pair<R, String>> = and(Str(s))

inline fun <R> Parser<R>.and(c: Char): Parser<Pair<R, Char>> = and(Ch(c))

inline fun <R> Parser<R>.or(rhs: Parser<R>): Parser<R> = Parser { s, index ->
    try {
        parse(s, index)
    } catch (_: InternalParseException) {
        rhs.parse(s, index)
    }
}

inline fun <R, R2> Parser<R>.map(crossinline mapper: (R) -> R2) = Parser { s, index ->
    val r = parse(s, index)
    ParseResult(mapper(r.result), r.index)
}

inline fun <R, R2> Parser<R>.value(r2: R2): Parser<R2> = map { r2 }

inline fun <R, R2> Parser<R>.cast(clazz: Class<R2>): Parser<R2> = map { clazz.cast(it) }

inline fun <R> Parser<R>.many(min: Int, max: Int? = null): Parser<List<R>> = Parser { s, index ->
    val _max = max ?: min
    var count = 0
    var currIndex = index
    val result = ArrayList<R>(min)

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
        } catch (_: InternalParseException) {
            break
        }
    }

    ParseResult(result, currIndex)
}

inline fun <R> Parser<R>.many1(): Parser<List<R>> = many(1, -1)

inline fun <R> Parser<R>.many0(): Parser<List<R>> = many(0, -1)

inline fun <R> Parser<R>.trim(): Parser<R> = surround(WhiteSpace().many0())

inline fun <R, R2> Parser<R>.skip(rhs: Parser<R2>): Parser<R> = and(rhs).map { it.first }

inline fun <R> Parser<R>.skipAll(vararg parsers: Parser<Any>): Parser<R> = and(Seq(*parsers)).map { it.first }

inline fun <R> Parser<R>.surround(prefix: Parser<Any>, suffix: Parser<Any>? = null): Parser<R> =
    Skip(prefix).and(this).skip(suffix ?: prefix)

inline fun <R> Parser<R>.opt(default: R): Parser<R> = Parser { s, index ->
    try {
        parse(s, index)
    } catch (_: InternalParseException) {
        ParseResult(default, index)
    }
}

inline fun <R> Parser<R>.opt(): Parser<Any> = Parser { s, index ->
    try {
        parse(s, index)
    } catch (_: InternalParseException) {
        ParseResult(PlaceHolder, index)
    } as ParseResult<Any>
}

inline fun <R, R2> Parser<R>.flatMap(crossinline mapper: (ParseResult<R>) -> Parser<R2>): Parser<Pair<R, R2>> =
    Parser { s, index ->
        val r1 = parse(s, index)
        val r2 = mapper(r1).parse(s, r1.index)
        ParseResult(Pair(r1.result, r2.result), r2.index)
    }

inline fun <R> Parser<R>.fatal(crossinline exceptionMapper: (String, Int) -> RuntimeException): Parser<R> =
    Parser { s, index ->
        try {
            parse(s, index)
        } catch (_: InternalParseException) {
            throw exceptionMapper(s, index)
        }
    }

inline fun <R> Parser<R>.end(): Parser<R> = skip(End())

inline fun <R> Parser<R>.notFollow(test: Parser<Any>): Parser<R> = skip(Not(test))

inline fun <R> Parser<R>.withExpect(msg: String) = fatal { s, i -> ParseException(msg, s, i) }
