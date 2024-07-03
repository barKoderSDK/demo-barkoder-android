package com.barkoder.demoscanner.enums

enum class Charset(val title: String, val value: String) {

    NOT_SET("Not set", ""),
    ISO_8859_1("ISO-8859-1", "ISO-8859-1"),
    ISO_8859_2("ISO-8859-2", "ISO-8859-2"),
    ISO_8859_5("ISO-8859-5", "ISO-8859-5"),
    SHIFT_JIS("Shift_JIS", "Shift_JIS"),
    US_ASCII("US-ASCII", "US-ASCII"),
    UTF_8("UTF-8", "UTF-8"),
    UTF_16("UTF-16", "UTF-16"),
    UTF_32("UTF-32", "UTF-32"),
    WINDOWS_1251("windows-1251", "windows-1251"),
    WINDOWS_1256("windows-1256", "windows-1256");

    override fun toString(): String {
        return title
    }
}
