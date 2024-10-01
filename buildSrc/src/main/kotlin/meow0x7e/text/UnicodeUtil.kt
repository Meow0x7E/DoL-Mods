package meow0x7e.text

class UnicodeUtil {
    companion object {
        fun isFullWidthCharacter(char: Char): Boolean {
            val unicodeBlock = Character.UnicodeBlock.of(char)
            return unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || unicodeBlock == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                    || unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || unicodeBlock == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                    || unicodeBlock == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
                    || unicodeBlock == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
        }

        fun getTerminalDisplayLength(c: Char): Int = if (isFullWidthCharacter(c)) 2 else 1

        fun getTerminalDisplayLength(s: String): Int {
            var len = 0
            s.forEach {
                len += getTerminalDisplayLength(it)
            }
            return len
        }
    }
}

fun Char.isFullWidthCharacter() = UnicodeUtil.isFullWidthCharacter(this)
fun Char.getTerminalDisplayLength() = UnicodeUtil.getTerminalDisplayLength(this)
fun String.getTerminalDisplayLength() = UnicodeUtil.getTerminalDisplayLength(this)