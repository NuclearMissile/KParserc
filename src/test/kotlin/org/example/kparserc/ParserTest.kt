package org.example.kparserc

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun testCh() {
        val p1 = Ch { c -> c == 'a' }
        assertEquals('a', p1.eval("a"))
        assertThrows<InternalParseException> { p1.eval("b") }
        assertThrows<InternalParseException> { p1.eval("") }

        val p2 = Ch('a')
        assertEquals('a', p2.eval("a"))
        assertThrows<InternalParseException> { p2.eval("b") }
        assertThrows<InternalParseException> { p2.eval("") }
    }

    @Test
    fun testAny() {
        val p = AnyCh()
        assertEquals('a', p.eval("a"))
        assertEquals('b', p.eval("b"))
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testRange() {
        val p1 = Range('d', 'f')
        assertEquals('d', p1.eval("d"))
        assertEquals('e', p1.eval("e"))
        assertEquals('f', p1.eval("f"))
        assertThrows<InternalParseException> { p1.eval("") }
        assertThrows<InternalParseException> { p1.eval("c") }
        assertThrows<InternalParseException> { p1.eval("g") }

        val p2 = Range('f', 'd')
        assertEquals('d', p2.eval("d"))
        assertEquals('e', p2.eval("e"))
        assertEquals('f', p2.eval("f"))
        assertThrows<InternalParseException> { p2.eval("") }
        assertThrows<InternalParseException> { p2.eval("c") }
        assertThrows<InternalParseException> { p2.eval("g") }
    }

    @Test
    fun testChs() {
        val p1 = Chs('f')
        assertEquals('f', p1.eval("f"))
        assertThrows<InternalParseException> { p1.eval("") }
        assertThrows<InternalParseException> { p1.eval("a") }

        val p2 = Chs('a', 'c', 'e')
        assertEquals('a', p2.eval("a"))
        assertEquals('c', p2.eval("c"))
        assertEquals('e', p2.eval("e"))
        assertThrows<InternalParseException> { p2.eval("") }
        assertThrows<InternalParseException> { p2.eval("b") }
        assertThrows<InternalParseException> { p2.eval("d") }

        val p3 = Chs()
        assertThrows<InternalParseException> { p3.eval("") }
        assertThrows<InternalParseException> { p3.eval("a") }
    }

    @Test
    fun testNotChs() {
        val p1 = NotChs('a', 'c', 'e')
        assertEquals('b', p1.eval("b"))
        assertEquals('d', p1.eval("d"))
        assertThrows<InternalParseException> { p1.eval("") }
        assertThrows<InternalParseException> { p1.eval("a") }
        assertThrows<InternalParseException> { p1.eval("c") }
        assertThrows<InternalParseException> { p1.eval("e") }

        val p2 = NotChs('a')
        assertEquals('b', p2.eval("b"))
        assertThrows<InternalParseException> { p2.eval("") }
        assertThrows<InternalParseException> { p2.eval("a") }

        val p3 = NotChs()
        assertEquals('a', p3.eval("a"))
        assertEquals('b', p3.eval("b"))
        assertEquals('c', p3.eval("c"))
        assertThrows<InternalParseException> { p3.eval("") }
    }

    @Test
    fun testStr() {
        val p1 = Str("abc")
        assertEquals("abc", p1.eval("abc"))
        assertEquals("abc", p1.eval("abcd"))
        assertThrows<InternalParseException> { p1.eval("ab") }
        assertThrows<InternalParseException> { p1.eval("def") }
        assertThrows<InternalParseException> { p1.eval("") }
    }

    @Test
    fun testStrs() {
        val p1 = Strs("abc")
        assertEquals("abc", p1.eval("abc"))
        assertEquals("abc", p1.eval("abcd"))
        assertThrows<InternalParseException> { p1.eval("ab") }
        assertThrows<InternalParseException> { p1.eval("def") }
        assertThrows<InternalParseException> { p1.eval("") }

        val p2 = Strs("abc", "def")
        assertEquals("abc", p2.eval("abc"))
        assertEquals("def", p2.eval("def"))
        assertEquals("abc", p2.eval("abcd"))
        assertThrows<InternalParseException> { p2.eval("ghi") }
        assertThrows<InternalParseException> { p2.eval("") }

        val p3 = Strs()
        assertThrows<InternalParseException> { p3.eval("") }
        assertThrows<InternalParseException> { p3.eval("abc") }
    }

    @Test
    fun testAnd() {
        val p1 = Str("hello").and('a')
        assertEquals(Pair("hello", 'a'), p1.eval("helloa"))
        assertThrows<InternalParseException> { p1.eval("hellob") }
        assertThrows<InternalParseException> { p1.eval("hello") }
        assertThrows<InternalParseException> { p1.eval("") }

        val p2 = Str("hello").and(" world")
        assertEquals(Pair("hello", " world"), p2.eval("hello world"))
        assertThrows<InternalParseException> { p2.eval("hello a") }
        assertThrows<InternalParseException> { p2.eval("hello ") }
        assertThrows<InternalParseException> { p2.eval("") }
    }

    @Test
    fun testSeq() {
        val p1 = Seq(Ch('a'), Str("bcd"), Ch('e'))
        assertEquals(listOf('a', "bcd", 'e'), p1.eval("abcde"))
        assertEquals(listOf('a', "bcd", 'e'), p1.eval("abcdef"))
        assertThrows<InternalParseException> { p1.eval("fghij") }
        assertThrows<InternalParseException> { p1.eval("hello") }
        assertThrows<InternalParseException> { p1.eval("abc") }
        assertThrows<InternalParseException> { p1.eval("") }

        val p2 = Seq(Str("abc"))
        assertEquals(listOf("abc"), p2.eval("abc"))
        assertEquals(listOf("abc"), p2.eval("abcd"))
        assertThrows<InternalParseException> { p2.eval("def") }
        assertThrows<InternalParseException> { p2.eval("") }

        val p3 = Seq()
        assertEquals(emptyList(), p3.eval(""))
        assertEquals(emptyList(), p3.eval("abc"))
    }

    @Test
    fun testOr() {
        val p1 = Ch('a').or(Chs('b'))
        assertEquals('a', p1.eval("a"))
        assertEquals('b', p1.eval("b"))
        assertThrows<InternalParseException> { p1.eval("c") }
        assertThrows<InternalParseException> { p1.eval("") }
    }


    @Test
    fun testOneOf() {
        val p1 = OneOf(Ch('a'), Ch('b'), Ch('c'))
        assertEquals('a', p1.eval("a"))
        assertEquals('b', p1.eval("b"))
        assertEquals('c', p1.eval("c"))
        assertThrows<InternalParseException> { p1.eval("d") }
        assertThrows<InternalParseException> { p1.eval("") }

        val p2 = OneOf<Any>()
        assertThrows<InternalParseException> { p2.eval("a") }
        assertThrows<InternalParseException> { p2.eval("") }

        val p3 = OneOf(Str("hello"), Str("world"))
        assertEquals("hello", p3.eval("hello"))
        assertEquals("world", p3.eval("world"))
        assertThrows<InternalParseException> { p3.eval("abc") }
        assertThrows<InternalParseException> { p3.eval("") }

        val p4 = OneOf(Str("hello"), Ch('a'))
        assertEquals("hello", p4.eval("hello"))
        assertEquals('a', p4.eval("a"))
        assertEquals('a', p4.eval("abc"))
        assertThrows<InternalParseException> { p4.eval("") }
    }

    @Test
    fun testMap() {
        val p1 = Str("hello").map(String::length)
        assertEquals(5, p1.eval("hello"))
        assertThrows<InternalParseException> { p1.eval("") }
    }

    @Test
    fun testValue() {
        val p1 = OneOf(Ch('t').value(true), Ch('f').value(false))
        assertEquals(true, p1.eval("t"))
        assertEquals(false, p1.eval("f"))
        assertThrows<InternalParseException> { p1.eval("abc") }
    }

    @Test
    fun testMany0() {
        val p1 = Ch('a').many0()
        assertEquals(emptyList(), p1.eval(""))
        assertEquals(listOf('a'), p1.eval("a"))
        assertEquals(listOf('a', 'a'), p1.eval("aa"))
        assertEquals(listOf('a', 'a', 'a'), p1.eval("aaa"))
        assertEquals(listOf('a'), p1.eval("abc"))
        assertEquals(emptyList(), p1.eval("bcd"))
    }

    @Test
    fun testMany1() {
        val p1 = Ch('a').many1()
        assertEquals(listOf('a'), p1.eval("a"))
        assertEquals(listOf('a', 'a'), p1.eval("aa"))
        assertEquals(listOf('a', 'a', 'a'), p1.eval("aaa"))
        assertEquals(listOf('a'), p1.eval("abc"))
        assertThrows<InternalParseException> { p1.eval("") }
    }

    @Test
    fun testRepeat() {
        val p1 = Ch('a').many(1, 3)
        assertThrows<InternalParseException> { p1.eval("") }
        assertEquals(listOf('a'), p1.eval("a"))
        assertEquals(listOf('a', 'a'), p1.eval("aa"))
        assertEquals(listOf('a', 'a', 'a'), p1.eval("aaa"))
        assertEquals(listOf('a', 'a', 'a'), p1.eval("aaaa"))
        assertEquals(listOf('a'), p1.eval("abcd"))

        val p2 = Ch('a').many(3)
        assertThrows<InternalParseException> { p2.eval("") }
        assertThrows<InternalParseException> { p2.eval("a") }
        assertThrows<InternalParseException> { p2.eval("aa") }
        assertThrows<InternalParseException> { p2.eval("abcd") }
        assertEquals(listOf('a', 'a', 'a'), p2.eval("aaa"))
        assertEquals(listOf('a', 'a', 'a'), p2.eval("aaaa"))
    }

    @Test
    fun testOpt() {
        val p1 = Ch('a').opt('z')
        assertEquals('a', p1.eval("a"))
        assertEquals('z', p1.eval(""))
        assertEquals('z', p1.eval("b"))
        assertEquals('z', p1.eval("z"))
    }

    @Test
    fun testLazy() {
        var flg = false
        val p1 = Lazy { flg = true; Ch('a') }
        assertFalse(flg)
        assertEquals('a', p1.eval("a"))
        assertTrue(flg)
    }

    @Test
    fun testSurround() {
        val p1 = Ch('a').surround(Ch('('), Ch(')'))
        assertEquals('a', p1.eval("(a)"))
        assertThrows<InternalParseException> { p1.eval("a") }
        assertThrows<InternalParseException> { p1.eval("(a") }
        assertThrows<InternalParseException> { p1.eval("a)") }
        assertThrows<InternalParseException> { p1.eval("(b)") }
        assertThrows<InternalParseException> { p1.eval("") }

        val p2 = Ch('a').surround(Strs("===", "---"))
        assertEquals('a', p2.eval("===a==="))
        assertEquals('a', p2.eval("---a---"))
        assertEquals('a', p2.eval("===a---"))
        assertThrows<InternalParseException> { p2.eval("a") }
        assertThrows<InternalParseException> { p2.eval("===b===") }
        assertThrows<InternalParseException> { p2.eval("") }
    }

    @Test
    fun testTrim() {
        val p1 = Ch('a').trim()
        assertEquals('a', p1.eval("a"))
        assertEquals('a', p1.eval("  a  "))
        assertEquals('a', p1.eval("      a"))
        assertEquals('a', p1.eval("a  "))
        assertEquals('a', p1.eval("\ta\n"))
        assertThrows<InternalParseException> { p1.eval("   b    ") }
        assertThrows<InternalParseException> { p1.eval("") }
    }

    @Test
    fun testSkip() {
        val p1 = Skip(Ch('a')).and(Str("bc"))
        assertEquals("bc", p1.eval("abc"))
        assertEquals("bc", p1.eval("abcd"))
        assertThrows<InternalParseException> { p1.eval("") }

        val p2 = Ch('a').skip(Str("bc"))
        assertEquals('a', p2.eval("abc"))
        assertEquals('a', p2.eval("abcd"))
        assertThrows<InternalParseException> { p2.eval("") }
    }

    @Test
    fun testFlatMap() {
        val alpha = Range('a', 'z').or(Range('A', 'Z'))
        val tagName = alpha.many1().map { it.joinToString("") }
        val tagContent = NotChs('<').many1().map { it.joinToString("") }
        val tag = Skip(Ch('<')).and(tagName).skip(Ch('>')).flatMap {
            tagContent.skip(Str("</").and(Str(it.result)).and(">"))
        }.end()

        assertEquals(Pair("body", "content"), tag.eval("<body>content</body>"))
        assertThrows<InternalParseException>{ tag.eval("<aaa>bbb</ccc>") }
        assertThrows<InternalParseException>{ tag.eval("<body>content</body>aa") }
    }

    @Test
    fun testFetal() {
        val p1 = Seq(Ch('a'), Ch('b').fatal { _, _ -> ParseException("b expected") })
        val p2 = Seq(
            Ch('d'),
            Ch('e').fatal { _, _ -> ParseException("e expected") },
            Ch('f').fatal { _, _ -> ParseException("f expected") },
        )
        val p3 = Seq(Ch('g'), Ch('h').fatal { _, _ -> ParseException("h expected") })

        val p = OneOf(p1, p2, p3).fatal { s, i -> ParseException("parse error", s, i) }
        val e1 = assertThrows<ParseException> { p.eval("az") }
        assertTrue(e1.message.contains("b expected"))
        val e2 = assertThrows<ParseException> { p.eval("dez") }
        assertTrue(e2.message.contains("f expected"))
        val e3 = assertThrows<ParseException> { p.eval("gz") }
        assertTrue(e3.message.contains("h expected"))
        val e4 = assertThrows<ParseException> { p.eval("xxx") }
        assertTrue(e4.message.contains("parse error"))
        assertEquals(listOf('a', 'b'), p.eval("ab"))
    }

    @Test
    fun testNot1() {
        val p1 = Skip(Not(Str("abc"))).and(Str("abxyz"))
        assertEquals("abxyz", p1.eval("abxyz"))
        assertThrows<InternalParseException> { p1.eval("abcde") }
    }
}