package org.example.kparserc

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParserTest2 {
    @Test
    fun testEmpty1() {
        val p = Empty(123)
        assertEquals(123, p.eval("abc"))
        assertEquals(123, p.eval(""))
    }

    @Test
    fun testFail1() {
        val p = Fail<Any>()
        assertThrows<InternalParseException> { p.eval("abc") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testEnd1() {
        val p = End("xyz")
        val r = p.eval("")
        assertEquals("xyz", r)
        assertThrows<InternalParseException> { p.eval("abc") }
    }

    @Test
    fun testEnd2() {
        val p = End()
        val r = p.eval("")
        assertEquals(PlaceHolder, r)
        assertThrows<InternalParseException> { p.eval("abc") }
    }

    @Test
    fun testMatch() {
        val p1 = Match("\\d*\\.\\d+").end()
        assertEquals(".123", p1.eval(".123"))
        assertEquals("0.123", p1.eval("0.123"))
        assertThrows<InternalParseException> { p1.eval("abc") }
        assertThrows<InternalParseException> { p1.eval("3.") }

        val p2 = Match("('[^']+')|(\"[^\"]+\")").map { it.trim('"', '\'') }.end()
        assertEquals("abc", p2.eval("'abc'"))
        assertEquals("abc", p2.eval("\"abc\""))
        assertThrows<InternalParseException> { p2.eval("'abc\"") }
        assertThrows<InternalParseException> { p2.eval("abc") }
    }

    @Test
    fun testCh1() {
        val p = Ch { c -> c == 'a' }
        val r = p.eval("abc")
        assertEquals('a', r)
        assertThrows<InternalParseException> { p.eval("def") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testCh2() {
        val p = Ch('a')
        val r = p.eval("abc")
        assertEquals('a', r)
        assertThrows<InternalParseException> { p.eval("def") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testAny() {
        val p = AnyCh()
        val r1 = p.eval("abc")
        assertEquals('a', r1)
        val r = p.eval("bcd")
        assertEquals('b', r)
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testRange1() {
        val p = Range('d', 'f')
        val r = p.eval("dog")
        assertEquals('d', r)
        assertEquals('e', p.eval("egg"))
        assertEquals('f', p.eval("father"))
        assertThrows<InternalParseException> { p.eval("apple") }
        assertThrows<InternalParseException> { p.eval("high") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testRange2() {
        val p = Range('f', 'd')
        val r = p.eval("dog")
        assertEquals('d', r)
        assertEquals('e', p.eval("egg"))
        assertEquals('f', p.eval("father"))
        assertThrows<InternalParseException> { p.eval("apple") }
        assertThrows<InternalParseException> { p.eval("high") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testChs1() {
        val p = Chs('f', 'o', 'h')
        val r = p.eval("far")
        assertEquals('f', r)
        assertEquals('o', p.eval("ohh"))
        assertEquals('h', p.eval("high"))
        assertThrows<InternalParseException> { p.eval("xyz") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testChs2() {
        val p = Chs('f')
        val r = p.eval("far")
        assertEquals('f', r)
        assertThrows<InternalParseException> { p.eval("xyz") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testChs3() {
        val p = Chs()
        assertThrows<InternalParseException> { p.eval("xyz") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testNotChs1() {
        val p = NotChs('f', 'o', 'h')
        val r = p.eval("xyz")
        assertEquals('x', r)
        assertThrows<InternalParseException> { p.eval("fog") }
        assertThrows<InternalParseException> { p.eval("ohhh") }
        assertThrows<InternalParseException> { p.eval("high") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testNotChs2() {
        val p = NotChs('f')
        val r = p.eval("xyz")
        assertEquals('x', r)
        assertThrows<InternalParseException> { p.eval("fog") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testNotChs3() {
        val p = NotChs()
        val r = p.eval("xyz")
        assertEquals('x', r)
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testStr() {
        val p = Str("xyz")
        val r1 = p.eval("xyzabcd")
        assertEquals("xyz", r1)
        val r = p.eval("xyz")
        assertEquals("xyz", r)
        assertThrows<InternalParseException> { p.eval("by") }
        assertThrows<InternalParseException> { p.eval("bytb") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testStrs1() {
        val p = Strs("apple", "amend", "xyz")
        val r1 = p.eval("applemen")
        assertEquals("apple", r1)
        val r = p.eval("xyzm")
        assertEquals("xyz", r)
        assertThrows<InternalParseException> { p.eval("app") }
        assertThrows<InternalParseException> { p.eval("bycd") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testStrs2() {
        val p = Strs("xyz")
        val r1 = p.eval("xyzabcd")
        assertEquals("xyz", r1)
        val r = p.eval("xyz")
        assertEquals("xyz", r)
        assertThrows<InternalParseException> { p.eval("by") }
        assertThrows<InternalParseException> { p.eval("bytb") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testStrs3() {
        val p = Strs()
        assertThrows<InternalParseException> { p.eval("abc") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testAnd() {
        val p = Str("hello").and(Ch('a'))
        val r = p.eval("helloabc")
        assertEquals(Pair("hello", 'a'), r)
        assertThrows<InternalParseException> { p.eval("hello world") }
        assertThrows<InternalParseException> { p.eval("xyz") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testSeq1() {
        val p = Seq(Ch('a'), Str("bcd"), Ch('e'))
        val r = p.eval("abcdefgh")
        assertEquals(listOf('a', "bcd", 'e'), r)
        assertThrows<InternalParseException> { p.eval("abcdk") }
        assertThrows<InternalParseException> { p.eval("amnpuk") }
        assertThrows<InternalParseException> { p.eval("xyz") }
        assertThrows<InternalParseException> { p.eval("a") }
        assertThrows<InternalParseException> { p.eval("abc") }
        assertThrows<InternalParseException> { p.eval("abcd") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testSeq2() {
        val p = Seq(Str("abc"))
        val r = p.eval("abcde")
        assertEquals(listOf("abc"), r)
        assertThrows<InternalParseException> { p.eval("axy") }
        assertThrows<InternalParseException> { p.eval("ab") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testSeq3() {
        val p = Seq()
        val r = p.eval("abcde")
        assertEquals(emptyList(), r)
        assertEquals(emptyList(), p.eval(""))
    }

    @Test
    fun testOr() {
        val p = Ch('a').or(Ch('b'))
        val r1 = p.eval("a")
        assertEquals('a', r1)
        val r = p.eval("b")
        assertEquals('b', r)
        assertThrows<InternalParseException> { p.eval("x") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testOneOf1() {
        val p = OneOf(Ch('a'), Ch('b'), Ch('c'))
        val r2 = p.eval("a")
        assertEquals('a', r2)
        val r1 = p.eval("b")
        assertEquals('b', r1)
        val r = p.eval("c")
        assertEquals('c', r)
        assertThrows<InternalParseException> { p.eval("d") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testOneOf2() {
        val p = OneOf(Ch('a'))
        val r = p.eval("a")
        assertEquals('a', r)
        assertThrows<InternalParseException> { p.eval("d") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testOneOf3() {
        val p = OneOf<Any>()
        assertThrows<InternalParseException> { p.eval("a") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testMap() {
        val p = Str("hello").map { it.length }
        val r = p.eval("hello")
        assertEquals(5, r)
        assertThrows<InternalParseException> { p.eval("hi") }
    }

    @Test
    fun testMany() {
        val p = Ch('a').many(1, 3)
        assertThrows<InternalParseException> { p.eval("") }
        assertThrows<InternalParseException> { p.eval("b") }
        assertEquals(listOf('a'), p.eval("a"))
        assertEquals(listOf('a', 'a'), p.eval("aa"))
        assertEquals(listOf('a', 'a', 'a'), p.eval("aaa"))
        assertEquals(listOf('a', 'a', 'a'), p.eval("aaab"))

        val p2 = Ch('a').many(1)
        assertThrows<InternalParseException> { p2.eval("") }
        assertEquals(listOf('a'), p2.eval("a"))
        assertEquals(listOf('a'), p2.eval("aa"))
    }

    @Test
    fun testMany0() {
        val p = Ch('a').many0()
        val r3 = p.eval("")
        assertEquals(emptyList(), r3)
        val r2 = p.eval("bbb")
        assertEquals(emptyList(), r2)
        val r1 = p.eval("a")
        assertEquals(listOf('a'), r1)
        val r = p.eval("aaa")
        assertEquals(listOf('a', 'a', 'a'), r)
    }

    @Test
    fun testMany1() {
        val p = Ch('a').many1()
        val r1 = p.eval("a")
        assertEquals(listOf('a'), r1)
        val r = p.eval("aaa")
        assertEquals(listOf('a', 'a', 'a'), r)
        assertThrows<InternalParseException> { p.eval("bbb") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testOpt1() {
        val p = Ch('a').opt('x')
        val r2 = p.eval("a")
        assertEquals('a', r2)
        val r1 = p.eval("xyz")
        assertEquals('x', r1)
        val r = p.eval("")
        assertEquals('x', r)
    }

    @Test
    fun testOpt2() {
        val p = Ch('a').opt()
        val r2 = p.eval("a")
        assertEquals('a', r2)
        val r1 = p.eval("xyz")
        assertEquals(PlaceHolder, r1)
        val r = p.eval("")
        assertEquals(PlaceHolder, r)
    }

    @Test
    fun testLazy() {
        var flg = false
        val p = Lazy { flg = true; Ch('a') }
        assertFalse(flg)
        val r = p.eval("a")
        assertEquals('a', r)
        assertTrue(flg)
    }

    @Test
    fun testSurround1() {
        val p = Ch('a').surround(Ch('('), Ch(')'))
        val r = p.eval("(a)")
        assertEquals('a', r)
        assertThrows<InternalParseException> { p.eval("(a") }
        assertThrows<InternalParseException> { p.eval("a)") }
        assertThrows<InternalParseException> { p.eval("a") }
        assertThrows<InternalParseException> { p.eval("(b)") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testSurround2() {
        val p = Ch('a').surround(Str("***"))
        val r = p.eval("***a***")
        assertEquals('a', r)
        assertThrows<InternalParseException> { p.eval("***a**") }
        assertThrows<InternalParseException> { p.eval("*a***") }
        assertThrows<InternalParseException> { p.eval("*a**") }
        assertThrows<InternalParseException> { p.eval("***b***") }
        assertThrows<InternalParseException> { p.eval("") }
    }

    @Test
    fun testSkipFirst() {
        val p = Skip(Ch('a')).and(Str("bc"))
        val r = p.eval("abc")
        assertEquals("bc", r)
    }

    @Test
    fun testSkipSecond() {
        val p = Ch('a').skip(Str("bc"))
        val r = p.eval("abc")
        assertEquals('a', r)
    }

    @Test
    fun testSkip() {
        val p = Skip(Ch('a')).and(Str("bc"))
        val r = p.eval("abc")
        assertEquals("bc", r)
    }

    @Test
    fun testSkipAll() {
        val p = Ch('a').skipAll(Ch('b'), Ch('c'))
        assertEquals('a', p.eval("abc"))
        assertThrows<InternalParseException> { p.eval("cba") }
    }

    @Test
    fun testExpect() {
        val p = Expect(Str("xy"), 123)
        val r = p.eval("xyz")
        assertEquals(123, r)
        assertThrows<InternalParseException> { p.eval("xz") }
    }

    @Test
    fun testNot() {
        val p = Not(Str("xy"))
        val r = p.eval("abc")
        assertEquals(PlaceHolder, r)
        assertThrows<InternalParseException> { p.eval("xyz") }
    }

    @Test
    fun testNotFollow() {
        val p = Str("abc").notFollow(Ch('d'))
        val r1 = p.eval("abcx")
        assertEquals("abc", r1)
        val r = p.eval("abc")
        assertEquals("abc", r)
        assertThrows<InternalParseException> { p.eval("abcd") }
    }

    @Test
    fun testFatal() {
        val p = Ch('a').fatal { s, index -> ParseException("msg_fatal", s, index) }
        val e = assertThrows<ParseException> { p.eval("bcd") }
        assertContains(e.message, "msg_fatal")
    }

    @Test
    fun testWithExpect() {
        val p = Ch('a').withExpect("'a' expected.")
        assertEquals('a', p.eval("ab"))
        val e = assertThrows<ParseException> { p.eval("ba") }
        assertContains(e.message, "'a' expected.")
    }
}
