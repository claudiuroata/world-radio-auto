package com.worldradio.auto.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StationDao {

    @Query("SELECT * FROM stations WHERE isFavorite = 1 AND isHidden = 0 ORDER BY name ASC")
    fun getFavoriteStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isFavorite = 1 AND isHidden = 0 ORDER BY name ASC")
    suspend fun getFavoriteStationsSync(): List<StationEntity>

    @Query("SELECT stationId FROM stations WHERE isFavorite = 1")
    fun getAllFavoriteStationIds(): Flow<List<String>>

    @Query("SELECT * FROM stations WHERE lastPlayedTimestamp > 0 AND isHidden = 0 ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    fun getRecentStations(limit: Int = 30): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE lastPlayedTimestamp > 0 AND isHidden = 0 ORDER BY lastPlayedTimestamp DESC LIMIT :limit")
    suspend fun getRecentStationsSync(limit: Int = 30): List<StationEntity>

    @Query("SELECT * FROM stations WHERE isHidden = 1 ORDER BY name ASC")
    fun getHiddenStations(): Flow<List<StationEntity>>

    @Query("SELECT * FROM stations WHERE isHidden = 1 ORDER BY name ASC")
    suspend fun getHiddenStationsSync(): List<StationEntity>

    @Query("SELECT stationId FROM stations WHERE isHidden = 1")
    suspend fun getHiddenStationIdsSync(): List<String>

    @Query("SELECT * FROM stations WHERE stationId = :stationId LIMIT 1")
    suspend fun getStationById(stationId: String): StationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(station: StationEntity)

    @Query("UPDATE stations SET isFavorite = :isFavorite WHERE stationId = :stationId")
    suspend fun setFavorite(stationId: String, isFavorite: Boolean)

    @Query("UPDATE stations SET isHidden = :isHidden WHERE stationId = :stationId")
    suspend fun setHidden(stationId: String, isHidden: Boolean)

    @Query("UPDATE stations SET lastPlayedTimestamp = :timestamp WHERE stationId = :stationId")
    suspend fun updateLastPlayed(stationId: String, timestamp: Long)

    @Query("SELECT isFavorite FROM stations WHERE stationId = :stationId LIMIT 1")
    suspend fun isFavorite(stationId: String): Boolean?

    @Query("SELECT isHidden FROM stations WHERE stationId = :stationId LIMIT 1")
    suspend fun isHidden(stationId: String): Boolean?
}
