package si.lagardere.inventura.export

import si.lagardere.inventura.data.Record

/**
 * ==========================================================================
 *  EXPORT FORMAT  --  confirmed against a real CE export (voklo_1.csv).
 * ==========================================================================
 * The MAOP back-office import ("Štetje inventure") reads the file the old
 * Windows CE reader produced. Confirmed line format:
 *
 *     POPISOVALEC;EAN;KOLIČINA        e.g.  27;3831008245905;48
 *
 * - semicolon separated, no header row
 * - one line per scan (duplicate EANs are kept separate; the BO sums them)
 * - quantity is an integer
 *
 * The knobs below let you tweak the format later without touching the rest.
 */
object ExportFormat {

    /** Field order on each line. Confirmed: POPISOVALEC;EAN;KOLIČINA. */
    enum class Field { EAN, KOLICINA, POPISOVALEC }
    val order: List<Field> = listOf(Field.POPISOVALEC, Field.EAN, Field.KOLICINA)

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
