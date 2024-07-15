import org.example.kparserc.utils.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun testCh() {
        val p1 = ch { c -> c == 'a' }
        assertEquals('a', p1.parse("a"))
        assertThrows<ParseInternalException> { p1.parse("b") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = ch('a')
        assertEquals('a', p2.parse("a"))
        assertThrows<ParseInternalException> { p2.parse("b") }
        assertThrows<ParseInternalException> { p2.parse("") }
    }

    @Test
    fun testAny() {
        val p = any()
        assertEquals('a', p.parse("a"))
        assertEquals('b', p.parse("b"))
        assertThrows<ParseInternalException> { p.parse("") }
    }

    @Test
    fun testRange() {
        val p1 = range('d', 'f')
        assertEquals('d', p1.parse("d"))
        assertEquals('e', p1.parse("e"))
        assertEquals('f', p1.parse("f"))
        assertThrows<ParseInternalException> { p1.parse("") }
        assertThrows<ParseInternalException> { p1.parse("c") }
        assertThrows<ParseInternalException> { p1.parse("g") }

        val p2 = range('f', 'd')
        assertEquals('d', p2.parse("d"))
        assertEquals('e', p2.parse("e"))
        assertEquals('f', p2.parse("f"))
        assertThrows<ParseInternalException> { p2.parse("") }
        assertThrows<ParseInternalException> { p2.parse("c") }
        assertThrows<ParseInternalException> { p2.parse("g") }
    }

    @Test
    fun testChs() {
        val p1 = chs('f')
        assertEquals('f', p1.parse("f"))
        assertThrows<ParseInternalException> { p1.parse("") }
        assertThrows<ParseInternalException> { p1.parse("a") }

        val p2 = chs('a', 'c', 'e')
        assertEquals('a', p2.parse("a"))
        assertEquals('c', p2.parse("c"))
        assertEquals('e', p2.parse("e"))
        assertThrows<ParseInternalException> { p2.parse("") }
        assertThrows<ParseInternalException> { p2.parse("b") }
        assertThrows<ParseInternalException> { p2.parse("d") }

        val p3 = chs()
        assertThrows<ParseInternalException> { p3.parse("") }
        assertThrows<ParseInternalException> { p3.parse("a") }
    }

    @Test
    fun testNot() {
        val p1 = not('a', 'c', 'e')
        assertEquals('b', p1.parse("b"))
        assertEquals('d', p1.parse("d"))
        assertThrows<ParseInternalException> { p1.parse("") }
        assertThrows<ParseInternalException> { p1.parse("a") }
        assertThrows<ParseInternalException> { p1.parse("c") }
        assertThrows<ParseInternalException> { p1.parse("e") }

        val p2 = not('a')
        assertEquals('b', p2.parse("b"))
        assertThrows<ParseInternalException> { p2.parse("") }
        assertThrows<ParseInternalException> { p2.parse("a") }

        val p3 = not()
        assertEquals('a', p3.parse("a"))
        assertEquals('b', p3.parse("b"))
        assertEquals('c', p3.parse("c"))
        assertThrows<ParseInternalException> { p3.parse("") }
    }

    @Test
    fun testStr() {
        val p1 = str("abc")
        assertEquals("abc", p1.parse("abc"))
        assertThrows<ParseInternalException> { p1.parse("ab") }
        assertThrows<ParseInternalException> { p1.parse("abcd") }
        assertThrows<ParseInternalException> { p1.parse("def") }
        assertThrows<ParseInternalException> { p1.parse("") }
    }

    @Test
    fun testStrs() {
        val p1 = strs("abc")
        assertEquals("abc", p1.parse("abc"))
        assertThrows<ParseInternalException> { p1.parse("ab") }
        assertThrows<ParseInternalException> { p1.parse("abcd") }
        assertThrows<ParseInternalException> { p1.parse("def") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = strs("abc", "def")
        assertEquals("abc", p2.parse("abc"))
        assertEquals("def", p2.parse("def"))
        assertThrows<ParseInternalException> { p2.parse("abcd") }
        assertThrows<ParseInternalException> { p2.parse("ghi") }
        assertThrows<ParseInternalException> { p2.parse("") }

        val p3 = strs()
        assertThrows<ParseInternalException> { p3.parse("") }
        assertThrows<ParseInternalException> { p3.parse("abc") }
    }

    @Test
    fun testAnd() {
        val p1 = str("hello").and('a')
        assertEquals(Pair("hello", 'a'), p1.parse("helloa"))
        assertThrows<ParseInternalException> { p1.parse("hellob") }
        assertThrows<ParseInternalException> { p1.parse("hello") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = str("hello").and(" world")
        assertEquals(Pair("hello", " world"), p2.parse("hello world"))
        assertThrows<ParseInternalException> { p2.parse("hello a") }
        assertThrows<ParseInternalException> { p2.parse("hello ") }
        assertThrows<ParseInternalException> { p2.parse("") }
    }

    @Test
    fun testSeq() {
        val p1 = seq(ch('a'), str("bcd"), ch('e'))
        assertEquals(listOf('a', "bcd", 'e'), p1.parse("abcde"))
        assertThrows<ParseInternalException> { p1.parse("abcdef") }
        assertThrows<ParseInternalException> { p1.parse("fghij") }
        assertThrows<ParseInternalException> { p1.parse("hello") }
        assertThrows<ParseInternalException> { p1.parse("abc") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = seq(str("abc"))
        assertEquals(listOf("abc"), p2.parse("abc"))
        assertThrows<ParseInternalException> { p2.parse("abcd") }
        assertThrows<ParseInternalException> { p2.parse("def") }
        assertThrows<ParseInternalException> { p2.parse("") }

        val p3 = seq()
        assertEquals(emptyList(), p3.parse(""))
        assertThrows<ParseInternalException> { p3.parse("abc") }
    }

    @Test
    fun testOr() {
        val p1 = ch('a').or(chs('b'))
        assertEquals('a', p1.parse("a"))
        assertEquals('b', p1.parse("b"))
        assertThrows<ParseInternalException> { p1.parse("c") }
        assertThrows<ParseInternalException> { p1.parse("") }
    }


    @Test
    fun testOneOf() {
        val p1 = oneOf(ch('a'), ch('b'), ch('c'))
        assertEquals('a', p1.parse("a"))
        assertEquals('b', p1.parse("b"))
        assertEquals('c', p1.parse("c"))
        assertThrows<ParseInternalException> { p1.parse("d") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = oneOf<Any>()
        assertThrows<ParseInternalException> { p2.parse("a") }
        assertThrows<ParseInternalException> { p2.parse("") }

        val p3 = oneOf(str("hello"), str("world"))
        assertEquals("hello", p3.parse("hello"))
        assertEquals("world", p3.parse("world"))
        assertThrows<ParseInternalException> { p3.parse("abc") }
        assertThrows<ParseInternalException> { p3.parse("") }

        val p4 = oneOf(str("hello"), ch('a'))
        assertEquals("hello", p4.parse("hello"))
        assertEquals('a', p4.parse("a"))
        assertThrows<ParseInternalException> { p4.parse("abc") }
        assertThrows<ParseInternalException> { p4.parse("") }
    }

    @Test
    fun testMap() {
        val p1 = str("hello").map(String::length)
        assertEquals(5, p1.parse("hello"))
        assertThrows<ParseInternalException> { p1.parse("") }
    }

    @Test
    fun testValue() {
        val p1 = oneOf(ch('t').value(true), ch('f').value(false))
        assertEquals(true, p1.parse("t"))
        assertEquals(false, p1.parse("f"))
        assertThrows<ParseInternalException> { p1.parse("abc") }
    }

    @Test
    fun testMany0() {
        val p1 = ch('a').many0()
        assertEquals(emptyList(), p1.parse(""))
        assertEquals(listOf('a'), p1.parse("a"))
        assertEquals(listOf('a', 'a'), p1.parse("aa"))
        assertEquals(listOf('a', 'a', 'a'), p1.parse("aaa"))
        assertThrows<ParseInternalException> { p1.parse("abc") }
    }

    @Test
    fun testMany1() {
        val p1 = ch('a').many1()
        assertEquals(listOf('a'), p1.parse("a"))
        assertEquals(listOf('a', 'a'), p1.parse("aa"))
        assertEquals(listOf('a', 'a', 'a'), p1.parse("aaa"))
        assertThrows<ParseInternalException> { p1.parse("") }
        assertThrows<ParseInternalException> { p1.parse("abc") }
    }

    @Test
    fun testRepeat() {
        val p1 = ch('a').repeat(1, 3)
        assertThrows<ParseInternalException> { p1.parse("") }
        assertEquals(listOf('a'), p1.parse("a"))
        assertEquals(listOf('a', 'a'), p1.parse("aa"))
        assertEquals(listOf('a', 'a', 'a'), p1.parse("aaa"))
        assertThrows<ParseInternalException> { p1.parse("aaaa") }
        assertThrows<ParseInternalException> { p1.parse("abcd") }

        val p2 = ch('a').repeat(3)
        assertThrows<ParseInternalException> { p2.parse("") }
        assertThrows<ParseInternalException> { p2.parse("a") }
        assertThrows<ParseInternalException> { p2.parse("aa") }
        assertEquals(listOf('a', 'a', 'a'), p2.parse("aaa"))
        assertThrows<ParseInternalException> { p2.parse("aaaa") }
        assertThrows<ParseInternalException> { p2.parse("abcd") }
    }

    @Test
    fun testOptional() {
        val p1 = ch('a').optional('z')
        assertEquals('a', p1.parse("a"))
        assertEquals('z', p1.parse(""))
        assertThrows<ParseInternalException> { p1.parse("b") }
        assertThrows<ParseInternalException> { p1.parse("z") }
    }

    @Test
    fun testLazy() {
        var flg = false
        val p1 = lazy { flg = true; ch('a') }
        assertFalse(flg)
        assertEquals('a', p1.parse("a"))
        assertTrue(flg)
    }

    @Test
    fun testSurround() {
        val p1 = ch('a').surround(ch('('), ch(')'))
        assertEquals('a', p1.parse("(a)"))
        assertThrows<ParseInternalException> { p1.parse("a") }
        assertThrows<ParseInternalException> { p1.parse("(a") }
        assertThrows<ParseInternalException> { p1.parse("a)") }
        assertThrows<ParseInternalException> { p1.parse("(b)") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = ch('a').surround(strs("===", "---"))
        assertEquals('a', p2.parse("===a==="))
        assertEquals('a', p2.parse("---a---"))
        assertEquals('a', p2.parse("===a---"))
        assertThrows<ParseInternalException> { p2.parse("a") }
        assertThrows<ParseInternalException> { p2.parse("===b===") }
        assertThrows<ParseInternalException> { p2.parse("") }
    }

    @Test
    fun testTrim() {
        val p1 = ch('a').trim()
        assertEquals('a', p1.parse("a"))
        assertEquals('a', p1.parse("  a  "))
        assertEquals('a', p1.parse("      a"))
        assertEquals('a', p1.parse("a  "))
        assertEquals('a', p1.parse("\ta\n"))
        assertThrows<ParseInternalException> { p1.parse("   b    ") }
        assertThrows<ParseInternalException> { p1.parse("") }
    }

    @Test
    fun testSkip() {
        val p1 = skip(ch('a')).and(str("bc"))
        assertEquals("bc", p1.parse("abc"))
        assertThrows<ParseInternalException> { p1.parse("abcd") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = ch('a').skip(str("bc"))
        assertEquals('a', p2.parse("abc"))
        assertThrows<ParseInternalException> { p2.parse("abcd") }
        assertThrows<ParseInternalException> { p2.parse("") }
    }

    @Test
    fun testFlatMap() {
        val alphaBeta = range('a', 'z').or(range('A', 'Z'))
        val tagName = alphaBeta.many1().map { it.joinToString("") }
        val tagContent = not('<').many1().map { it.joinToString("") }
        val tag = skip(ch('<')).and(tagName).skip(ch('>')).flatMap {
            tagContent.skip(str("</").and(str(it.result)).and(">"))
        }
        assertEquals(Pair("body", "content"), tag.parse("<body>content</body>"))
        assertEquals(Pair("aaa", "bbb"), tag.parse("<aaa>bbb</aaa>"))
        assertThrows<ParseInternalException> { tag.parse("<aaa>bbb</ccc>") }
    }

    @Test
    fun testFetal() {
        val p1 = seq(ch('a'), ch('b').fetal { _, _ -> ParseException("b expected") })
        val p2 = seq(
            ch('d'),
            ch('e').fetal { _, _ -> ParseException("e expected") },
            ch('f').fetal { _, _ -> ParseException("f expected") },
        )
        val p3 = seq(ch('g'), ch('h').fetal { _, _ -> ParseException("h expected") })

        val p = oneOf(p1, p2, p3).fetal { s, i -> ParseException("parse error", s, i) }
        val e1 = assertThrows<ParseException> { p.parse("az") }
        assertTrue(e1.message.contains("b expected"))
        val e2 = assertThrows<ParseException> { p.parse("dez") }
        assertTrue(e2.message.contains("f expected"))
        val e3 = assertThrows<ParseException> { p.parse("gz") }
        assertTrue(e3.message.contains("h expected"))
        val e4 = assertThrows<ParseException> { p.parse("xxx") }
        assertTrue(e4.message.contains("parse error"))
        assertEquals(listOf('a', 'b'), p.parse("ab"))
    }

    @Test
    fun testExpect() {
        val p1 = skip(expect(str("abc"))).and(str("abcde"))
        assertEquals("abcde", p1.parse("abcde"))
        assertThrows<ParseInternalException> { p1.parse("xyz") }
    }

    @Test
    fun testNot_() {
        val p1 = skip(not(str("abc"))).and(str("abxyz"))
        assertEquals("abxyz", p1.parse("abxyz"))
        assertThrows<ParseInternalException> { p1.parse("abcde") }
    }
}