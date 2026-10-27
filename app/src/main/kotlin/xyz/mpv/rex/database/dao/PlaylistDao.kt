package xyz.mpv.rex.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import xyz.mpv.rex.database.entities.PlaylistEntity
import xyz.mpv.rex.database.entities.PlaylistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlaylistDao {
  // Playlist operations
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

  @Update
  abstract suspend fun updatePlaylist(playlist: PlaylistEntity)

  @Delete
  abstract suspend fun deletePlaylist(playlist: PlaylistEntity)

  @Query("SELECT * FROM PlaylistEntity ORDER BY updatedAt DESC")
  abstract fun observeAllPlaylists(): Flow<List<PlaylistEntity>>

  @Query("SELECT * FROM PlaylistEntity ORDER BY updatedAt DESC")
  abstract suspend fun getAllPlaylists(): List<PlaylistEntity>

  @Query("SELECT * FROM PlaylistEntity WHERE id = :playlistId")
  abstract suspend fun getPlaylistById(playlistId: Int): PlaylistEntity?

  @Query("SELECT * FROM PlaylistEntity WHERE id = :playlistId")
  abstract fun observePlaylistById(playlistId: Int): Flow<PlaylistEntity?>

  // Playlist item operations
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertPlaylistItem(item: PlaylistItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract suspend fun insertPlaylistItems(items: List<PlaylistItemEntity>)

  @Update
  abstract suspend fun updatePlaylistItem(item: PlaylistItemEntity)

  @Delete
  abstract suspend fun deletePlaylistItem(item: PlaylistItemEntity)

  @Delete
  abstract suspend fun deletePlaylistItems(items: List<PlaylistItemEntity>)

  @Query("DELETE FROM PlaylistItemEntity WHERE id = :itemId")
  abstract suspend fun deletePlaylistItemById(itemId: Int)

  @Query("DELETE FROM PlaylistItemEntity WHERE id IN (:itemIds)")
  abstract suspend fun deletePlaylistItemsByIds(itemIds: List<Int>)

  @Query("SELECT * FROM PlaylistItemEntity WHERE playlistId = :playlistId ORDER BY position ASC")
  abstract fun observePlaylistItems(playlistId: Int): Flow<List<PlaylistItemEntity>>

  @Query("SELECT * FROM PlaylistItemEntity WHERE playlistId = :playlistId ORDER BY position ASC")
  abstract suspend fun getPlaylistItems(playlistId: Int): List<PlaylistItemEntity>

  @Query("SELECT COUNT(*) FROM PlaylistItemEntity WHERE playlistId = :playlistId")
  abstract suspend fun getPlaylistItemCount(playlistId: Int): Int

  @Query("SELECT COUNT(*) FROM PlaylistItemEntity WHERE playlistId = :playlistId")
  abstract fun observePlaylistItemCount(playlistId: Int): Flow<Int>

  @Query("DELETE FROM PlaylistItemEntity WHERE playlistId = :playlistId")
  abstract suspend fun deleteAllItemsFromPlaylist(playlistId: Int)

  @Query("UPDATE PlaylistItemEntity SET position = :newPosition WHERE id = :itemId")
  abstract suspend fun updateItemPosition(itemId: Int, newPosition: Int)

  @Transaction
  open suspend fun reorderPlaylistItems(playlistId: Int, newOrder: List<Int>) {
    newOrder.forEachIndexed { index, itemId ->
      updateItemPosition(itemId, index)
    }
  }

  @Query("SELECT MAX(position) AS maxPos FROM PlaylistItemEntity WHERE playlistId = :playlistId")
  abstract suspend fun getMaxPosition(playlistId: Int): Int?

  @Query("SELECT filePath FROM PlaylistItemEntity WHERE playlistId = :playlistId ORDER BY position ASC LIMIT 1")
  abstract suspend fun getFirstPlaylistItemPath(playlistId: Int): String?

  @Query("UPDATE PlaylistEntity SET customThumbnailPath = :path WHERE id = :playlistId")
  abstract suspend fun updateCustomThumbnailPath(playlistId: Int, path: String?)

  @Query("""
    SELECT COALESCE(
      (SELECT customThumbnailPath FROM PlaylistEntity WHERE id = :playlistId),
      (SELECT filePath FROM PlaylistItemEntity WHERE playlistId = :playlistId ORDER BY position ASC LIMIT 1)
    ) AS thumbnailPath
  """)
  abstract suspend fun getEffectiveThumbnailPath(playlistId: Int): String?

  // Play history operations
  @Query(
    """
    UPDATE PlaylistItemEntity 
    SET lastPlayedAt = :timestamp, playCount = playCount + 1, lastPosition = :position
    WHERE playlistId = :playlistId AND filePath = :filePath
    """,
  )
  abstract suspend fun updatePlayHistory(playlistId: Int, filePath: String, timestamp: Long, position: Long)

  @Query(
    """
    SELECT * FROM PlaylistItemEntity 
    WHERE playlistId = :playlistId 
    ORDER BY lastPlayedAt DESC
    LIMIT :limit
    """,
  )
  abstract suspend fun getRecentlyPlayedInPlaylist(playlistId: Int, limit: Int): List<PlaylistItemEntity>

  @Query(
    """
    SELECT * FROM PlaylistItemEntity 
    WHERE playlistId = :playlistId 
    ORDER BY lastPlayedAt DESC
    LIMIT :limit
    """,
  )
  abstract fun observeRecentlyPlayedInPlaylist(playlistId: Int, limit: Int): Flow<List<PlaylistItemEntity>>

  @Query(
    """
    SELECT * FROM PlaylistItemEntity 
    WHERE playlistId = :playlistId AND filePath = :filePath
    """,
  )
  abstract suspend fun getPlaylistItemByPath(playlistId: Int, filePath: String): PlaylistItemEntity?

  // Pagination support for large playlists
  @Query(
    """
    SELECT * FROM PlaylistItemEntity 
    WHERE playlistId = :playlistId 
    ORDER BY position ASC 
    LIMIT :limit OFFSET :offset
    """,
  )
  abstract suspend fun getPlaylistItemsWindow(playlistId: Int, offset: Int, limit: Int): List<PlaylistItemEntity>

  @Query(
    """
    SELECT * FROM PlaylistItemEntity 
    WHERE playlistId = :playlistId AND position >= :startPosition AND position < :endPosition
    ORDER BY position ASC
    """,
  )
  abstract suspend fun getPlaylistItemsInRange(playlistId: Int, startPosition: Int, endPosition: Int): List<PlaylistItemEntity>
}
