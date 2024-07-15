import org.example.kparserc.ParseInternalException
import org.example.kparserc.Parsers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ParserTest {
    @Test
    fun testCh() {
        val p1 = Parsers.ch { c -> c == 'a' }
        assertEquals('a', p1.parse("a"))
        assertThrows<ParseInternalException> { p1.parse("b") }
        assertThrows<ParseInternalException> { p1.parse("") }

        val p2 = Parsers.ch('a')
        assertEquals('a', p2.parse("a"))
        assertThrows<ParseInternalException> { p2.parse("b") }
        assertThrows<ParseInternalException> { p2.parse("") }
    }

    @Test
    fun testAny() {
        val p = Parsers.any()
        assertEquals('a', p.parse("a"))
        assertEquals('b', p.parse("b"))
        assertThrows<ParseInternalException> { p.parse("") }
    }

    @Test
    fun testRange() {
        val p1 = Parsers.range('d', 'f')
        assertEquals('d', p1.parse("d"))
        assertEquals('e', p1.parse("e"))
        assertEquals('f', p1.parse("f"))
        assertThrows<ParseInternalException> { p1.parse("") }
        assertThrows<ParseInternalException> { p1.parse("c") }
        assertThrows<ParseInternalException> { p1.parse("g") }

        val p2 = Parsers.range('f', 'd')
        assertEquals('d', p2.parse("d"))
        assertEquals('e', p2.parse("e"))
        assertEquals('f', p2.parse("f"))
        assertThrows<ParseInternalException> { p2.parse("") }
        assertThrows<ParseInternalException> { p2.parse("c") }
        assertThrows<ParseInternalException> { p2.parse("g") }
    }

    @Test
    fun testChs() {
        val p1 = Parsers.chs('f')
        assertEquals('f', p1.parse("f"))
        assertThrows<ParseInternalException> { p1.parse("") }
        assertThrows<ParseInternalException> { p1.parse("a") }

        val p2 = Parsers.chs('a', 'c', 'e')
        assertEquals('a', p2.parse("a"))
        assertEquals('c', p2.parse("c"))
        assertEquals('e', p2.parse("e"))
        assertThrows<ParseInternalException> { p2.parse("") }
        assertThrows<ParseInternalException> { p2.parse("b") }
        assertThrows<ParseInternalException> { p2.parse("d") }

        val p3 = Parsers.chs()
        assertThrows<ParseInternalException> { p3.parse("") }
        assertThrows<ParseInternalException> { p3.parse("a") }
    }

    @Test
    fun testNot() {
        val p1 = Parsers.not('a', 'c', 'e')
        assertEquals('b', p1.parse("b"))
        assertEquals('d', p1.parse("d"))
        assertThrows<ParseInternalException> { p1.parse("") }
        assertThrows<ParseInternalException> { p1.parse("a") }
        assertThrows<ParseInternalException> { p1.parse("c") }
        assertThrows<ParseInternalException> { p1.parse("e") }

        val p2 = Parsers.not('a')
        assertEquals('b', p2.parse("b"))
        assertThrows<ParseInternalException> { p2.parse("") }
        assertThrows<ParseInternalException> { p2.parse("a") }

        val p3 = Parsers.not()
        assertEquals('a', p3.parse("a"))
        assertEquals('b', p3.parse("b"))
        assertEquals('c', p3.parse("c"))
        assertThrows<ParseInternalException> { p3.parse("") }
    }

    @Test
    fun testStr() {
        val p = Parsers.str("abc")
        assertEquals("abc", p.parse("abc"))
        assertThrows<ParseInternalException> { p.parse("ab") }
        assertThrows<ParseInternalException> { p.parse("abcd") }
        assertThrows<ParseInternalException> { p.parse("def") }
        assertThrows<ParseInternalException> { p.parse("") }
    }

}