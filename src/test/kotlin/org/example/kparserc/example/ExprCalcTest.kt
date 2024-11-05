package org.example.kparserc.example

import org.example.kparserc.*
import org.junit.jupiter.api.assertThrows
import kotlin.math.log
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals

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
        .and(Lazy { expr })
        .skip(comma)
        .and(Lazy { expr })
        .skip(rp)
        .map { it.first.pow(it.second) }
    private val LOG: Parser<Double> = SkipAll(Str("log"), lp)
        .and(Lazy { expr })
        .skip(comma)
        .and(Lazy { expr })
        .skip(rp)
        .map { log(it.first, it.second) }

    private val number = Match("(\\d*\\.\\d+)|(\\d+)").map { it.toDouble() }.trim()
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

class ExprCalcTest {
    @Test
    fun test() {
        assertEquals(1.0, ExprCalc.eval("1"))
        assertEquals(1.0, ExprCalc.eval(" 1 "))
        assertEquals(1.0, ExprCalc.eval("1.0"))
        assertEquals(3.14, ExprCalc.eval("3.14"))
        assertEquals(-1.0, ExprCalc.eval("-1"))
        assertEquals(-3.14, ExprCalc.eval("-3.14"))
        assertEquals(2.0 + 3.0, ExprCalc.eval("2 + 3"))
        assertEquals(5.2 - 7.56, ExprCalc.eval("5.2-7.56"))
        assertEquals(123.456 * 67.89, ExprCalc.eval("123.456*67.89"))
        assertEquals(0.78 / 10.4, ExprCalc.eval(" .78 / 10.4 "))
        assertEquals((2.0 + 3) * (7 - 4.0), ExprCalc.eval("(2+3)*(7-4)"))
        assertEquals(
            2.4 / 5.774 * (6 / 3.57 + 6.37) - 2 * 7 / 5.2 + 5,
            ExprCalc.eval("2.4 / 5.774 * (6 / 3.57 + 6.37) - 2 * 7 / 5.2 + 5")
        )
        assertEquals(
            77.58 * (6 / 3.14 + 55.2234) - 2 * 6.1 / (1.0 + 2 / (4.0 - 3.8 * 5)),
            ExprCalc.eval("77.58* ( 6 / 3.14+55.2234 ) -2 * 6.1/ ( 1.0+2/ (4.0-3.8*5))  ")
        )
        assertEquals(Math.PI, ExprCalc.eval("PI"))
        assertEquals(
            77.58 * (6 / 3.14 + 55.2234) / (-Math.PI) - 2 * 6.1 / (1.0 + 2 / (4.0 - 3.8 * 5)),
            ExprCalc.eval("77.58* ( 6 / 3.14+55.2234 ) / (-PI) -2 * 6.1/ ( 1.0+2/ (4.0-3.8*5))  ")
        )
        assertEquals(2.0.pow(3.0) * 10 / Math.PI, ExprCalc.eval("pow(2, 3) * 10 / PI"))
        assertEquals(log(3 * (2.0.pow(3.0) + 3.0), 3.0), ExprCalc.eval("log(3 * (pow(2, 3) + 3), 3)"))

        assertThrows<ParseException> { ExprCalc.eval("") }
        assertThrows<ParseException> { ExprCalc.eval("abc") }
        assertThrows<ParseException> { ExprCalc.eval("1.2.3") }
        assertThrows<ParseException> { ExprCalc.eval("2+") }
        assertThrows<ParseException> { ExprCalc.eval("-2-3-") }
        assertThrows<ParseException> { ExprCalc.eval("1*2-/3") }
        assertThrows<ParseException> { ExprCalc.eval("()") }
        assertThrows<ParseException> { ExprCalc.eval("(") }
        assertThrows<ParseException> { ExprCalc.eval(")") }
        assertThrows<ParseException> { ExprCalc.eval("1*(2+(3+4)") }
    }
}
