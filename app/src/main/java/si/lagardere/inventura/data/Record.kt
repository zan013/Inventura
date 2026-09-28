package si.lagardere.inventura.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One inventory scan line, mirroring the Windows CE reader record:
 * Popisovalec (referent code), EAN, Količina.
 * The back-office import (MAOP "Štetje inventure") sums lines by EAN,
 * so we store one row per scan and export them raw.
 */
@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val popisovalec: String,
    val ean: String,
    val kolicina: Int,
    val createdAt: Long = System.currentTimeMillis()
)
