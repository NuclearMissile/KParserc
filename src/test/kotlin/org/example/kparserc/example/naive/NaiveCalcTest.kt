package org.example.kparserc.example.naive

import kotlin.math.ln
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals

class NaiveCalc {
    private lateinit var input: String
    private var currentPosition = 0
    private val constants = mutableMapOf<String, Double>()
    private val functions = mutableMapOf<String, (List<Double>) -> Double>()

    fun registerConstant(name: String, value: Double): NaiveCalc {
        constants[name] = value
        return this
    }

    fun registerFunction(name: String, function: (List<Double>) -> Double): NaiveCalc {
        functions[name] = function
        return this
    }

    private fun peek(): Char? = input.getOrNull(currentPosition)

    private fun next(): Char? = input.getOrNull(currentPosition++)

    private fun match(expected: Char): Boolean {
        if (peek() == expected) {
            next()
            return true
        }
        return false
    }

    private fun skipWhitespace() {
        while (peek()?.isWhitespace() == true) {
            next()
        }
    }

    // Parse a function call or a constant
    private fun parseFunctionOrConstant(): Double {
        val start = currentPosition
        while (peek()?.isLetterOrDigit() == true) {
            next()
        }
        val identifier = input.substring(start, currentPosition)

        if (match('(')) {
            // It's a function call
            val arguments = mutableListOf<Double>()
            if (!match(')')) {
                do {
                    arguments.add(parse0())
                } while (match(','))
                if (!match(')')) throw IllegalArgumentException("Expected ')' after function arguments")
            }
            val function = functions[identifier] ?: throw IllegalArgumentException("Undefined function: '$identifier'")
            return function(arguments)
        } else {
            // It's a constant
            return constants[identifier] ?: throw IllegalArgumentException("Undefined constant: '$identifier'")
        }
    }

    // Parse a number
    private fun parseNumber(): Double {
        val start = currentPosition
        while (peek()?.isDigit() == true || peek() == '.') {
            next()
        }
        return input.substring(start, currentPosition).toDouble()
    }

    // Parse a number, function call, constant, or parenthesized expression
    private fun parse3(): Double {
        skipWhitespace()
        val result: Double = when {
            match('(') -> {
                val result = parse0()
                if (!match(')')) throw IllegalArgumentException("Expected ')'")
                result
            }

            match('-') -> {
                if (peek()?.isDigit() == true || peek() == '.') -parseNumber()
                else if (peek()?.isLetter() == true) -parseFunctionOrConstant()
                else throw IllegalArgumentException("Unexpected character: '${peek()}'")
            }

            peek()?.isDigit() == true || peek() == '.' -> parseNumber()
            peek()?.isLetter() == true -> parseFunctionOrConstant()
            else -> throw IllegalArgumentException("Unexpected character: '${peek()}'")
        }
        skipWhitespace()
        return result
    }

    private fun parse2(): Double {
        var result = parse3()
        skipWhitespace()
        while (true) {
            when {
                match('^') -> result = result.pow(parse3())
                else -> return result
            }
        }
    }

    private fun parse1(): Double {
        var result = parse2()
        skipWhitespace()
        while (true) {
            when {
                match('*') -> result *= parse2()
                match('/') -> result /= parse2()
                else -> return result
            }
        }
    }

    private fun parse0(): Double {
        var result = parse1()
        skipWhitespace()
        while (true) {
            when {
                match('+') -> result += parse1()
                match('-') -> result -= parse1()
                else -> return result
            }
        }
    }


    fun calculate(expression: String): Double {
        input = expression
        currentPosition = 0
        val ret = parse0()
        return if (currentPosition == input.length) ret
        else throw IllegalArgumentException("Unexpected character: ${peek()}")
    }
}

class NaiveCalcTest {
    @Test
    fun testCalculate() {
        val calculator = NaiveCalc()
            .registerConstant("PI", 3.14)
            .registerFunction("pow") { args ->
                if (args.size != 2) throw IllegalArgumentException("pow expects 2 arguments")
                args[0].pow(args[1])
            }.registerFunction("log") { args ->
                if (args.size != 2) throw IllegalArgumentException("log expects 2 arguments")
                ln(args[0]) / ln(args[1])
            }
        assertEquals(16.0, calculator.calculate("2 ^3 + pow(2,3)"))
    }
}