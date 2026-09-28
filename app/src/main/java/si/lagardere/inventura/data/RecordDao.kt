package si.lagardere.inventura.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RecordDao {

    @Insert
    suspend fun insert(record: Record): Long

    @Delete
    suspend fun delete(record: Record)

    @Query("DELETE FROM records")
    suspend fun clear()

    @Query("SELECT * FROM records ORDER BY id ASC")
    suspend fun all(): List<Record>

    @Query("SELECT COUNT(*) FROM records")
    suspend fun count(): Int

    @Query("SELECT * FROM records ORDER BY id DESC LIMIT 1")
    suspend fun last(): Record?
}
