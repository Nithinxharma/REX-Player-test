package xyz.mpv.rex.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import xyz.mpv.rex.database.entities.MediaPlayCountEntity

@Dao
abstract class MediaPlayCountDao {
  @Query("SELECT * FROM media_play_counts WHERE filePath = :filePath LIMIT 1")
  abstract suspend fun getMediaPlayCountEntity(filePath: String): MediaPlayCountEntity?

  @Upsert
  abstract suspend fun upsertPlayCount(entity: MediaPlayCountEntity)

  @Transaction
  open suspend fun incrementPlayCount(filePath: String, timestamp: Long) {
    val existing = getMediaPlayCountEntity(filePath)
    val updated = if (existing != null) {
      existing.copy(playCount = existing.playCount + 1, lastPlayedAt = timestamp)
    } else {
      MediaPlayCountEntity(filePath = filePath, playCount = 1, lastPlayedAt = timestamp)
    }
    upsertPlayCount(updated)
  }

  @Query("SELECT * FROM media_play_counts WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
  abstract suspend fun getMostPlayed(limit: Int = 50): List<MediaPlayCountEntity>

  @Query("SELECT * FROM media_play_counts WHERE playCount > 0 ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
  abstract fun observeMostPlayed(limit: Int = 50): Flow<List<MediaPlayCountEntity>>

  @Query("SELECT playCount FROM media_play_counts WHERE filePath = :filePath LIMIT 1")
  abstract suspend fun getPlayCount(filePath: String): Int?

  @Query("UPDATE media_play_counts SET filePath = :newPath WHERE filePath = :oldPath")
  abstract suspend fun updateFilePath(oldPath: String, newPath: String)

  @Query("DELETE FROM media_play_counts WHERE filePath = :filePath")
  abstract suspend fun deleteByFilePath(filePath: String)

  @Query("DELETE FROM media_play_counts")
  abstract suspend fun clearAll()
}
