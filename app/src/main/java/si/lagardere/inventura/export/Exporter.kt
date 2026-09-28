package si.lagardere.inventura.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import si.lagardere.inventura.data.Record
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes the inventory to a text file whose name mirrors the old CE reader,
 * e.g. 28092026_093045123.txt  (ddMMyyyy_HHmmssSSS), with an optional
 * warehouse suffix like _63. Saved into the public Downloads/Inventura folder
 * so it is visible over USB (MTP) to copy into C:\temp on the PC.
 */
object Exporter {

    /** Sub-folder inside Downloads where files are written. */
    private const val SUBDIR = "Inventura"

    /** File extension. Real CE exports are ";"-separated CSV (e.g. voklo_1.csv).
     *  The MAOP import dialog uses "All Files", so .txt works too — change here. */
    private const val FILE_EXT = ".csv"

    /** File content charset. CE tooling is Windows-based; content is digits only,
     *  so ASCII/UTF-8 is safe. Change here if the import needs Windows-1250. */
    private val CHARSET = Charsets.UTF_8

    data class Result(val displayPath: String, val fileName: String)

    fun buildFileName(skladisce: String?): String {
        val stamp = SimpleDateFormat("ddMMyyyy_HHmmssSSS", Locale.US).format(Date())
        val sk = skladisce?.trim().orEmpty()
        val suffix = if (sk.isNotEmpty()) "_" + sk.filter { it.isLetterOrDigit() } else ""
        return "$stamp$suffix$FILE_EXT"
    }

    suspend fun export(
        context: Context,
        records: List<Record>,
        skladisce: String?
    ): Result = withContext(Dispatchers.IO) {
        val fileName = buildFileName(skladisce)
        val content = ExportFormat.build(records)
        val bytes = content.toByteArray(CHARSET)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeViaMediaStore(context, fileName, bytes)
        } else {
            writeLegacy(fileName, bytes)
        }
    }

    private fun writeViaMediaStore(context: Context, fileName: String, bytes: ByteArray): Result {
        val resolver = context.contentResolver
        val relative = Environment.DIRECTORY_DOWNLOADS + "/" + SUBDIR
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, relative)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: error("MediaStore ni vrnil poti za shranjevanje.")
        resolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: error("Ni mogoče odpreti izhodnega toka.")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return Result("Download/$SUBDIR/$fileName", fileName)
    }

    @Suppress("DEPRECATION")
    private fun writeLegacy(fileName: String, bytes: ByteArray): Result {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloads, SUBDIR).apply { if (!exists()) mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { it.write(bytes) }
        return Result(file.absolutePath, fileName)
    }
}
