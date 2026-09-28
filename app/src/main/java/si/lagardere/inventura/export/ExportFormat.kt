package si.lagardere.inventura.export

import si.lagardere.inventura.data.Record

/**
 * ==========================================================================
 *  EXPORT FORMAT  --  THE ONE THING TO CONFIRM AGAINST A REAL CE FILE.
 * ==========================================================================
 * The MAOP back-office import ("Štetje inventure") reads the text file the
 * old Windows CE reader produced. To stay 100% compatible, the line format
 * below must match that file exactly. We do NOT have a sample yet, so this
 * is a best guess: one line per scan, semicolon-separated, EAN;KOLICINA;POPISOVALEC.
 *
 * When you have a real file from the old reader (e.g. 06042017_101633742.txt),
 * adjust the four knobs below to match it. Nothing else in the app needs to change.
 */
object ExportFormat {

    /** Field order on each line. Reorder to match the CE file. */
    enum class Field { EAN, KOLICINA, POPISOVALEC }
    val order: List<Field> = listOf(Field.EAN, Field.KOLICINA, Field.POPISOVALEC)

    /** Column separator. Common options: ";"  "\t"  "," */
    const val DELIMITER: String = ";"

    /** Line terminator. Windows tools usually expect CRLF. */
    const val LINE_ENDING: String = "\r\n"

    /** Whether to write a trailing line ending after the last record. */
    const val TRAILING_NEWLINE: Boolean = true

    /** Quantity is an integer count on the reader; change here if decimals are needed. */
    fun formatQuantity(q: Int): String = q.toString()

    fun line(r: Record): String {
        val parts = order.map { field ->
            when (field) {
                Field.EAN -> r.ean
                Field.KOLICINA -> formatQuantity(r.kolicina)
                Field.POPISOVALEC -> r.popisovalec
            }
        }
        return parts.joinToString(DELIMITER)
    }

    fun build(records: List<Record>): String {
        val body = records.joinToString(LINE_ENDING) { line(it) }
        return if (TRAILING_NEWLINE && records.isNotEmpty()) body + LINE_ENDING else body
    }
}
