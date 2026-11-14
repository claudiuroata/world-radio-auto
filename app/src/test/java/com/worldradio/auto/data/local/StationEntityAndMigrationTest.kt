package com.worldradio.auto.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.worldradio.auto.data.model.Station
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StationEntityAndMigrationTest {

    private lateinit var context: Context
    private lateinit var db: RadioDatabase
    private lateinit var dao: StationDao

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RadioDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.stationDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun stationEntity_fromDomainAndToDomain_roundTripsAllFieldsIncludingFlagsAndTags() {
        val domain = Station(
            id = "st_1",
            name = "Rock FM",
            streamUrl = "https://stream.rockfm.ro/live.mp3",
            favicon = "https://rockfm.ro/logo.png",
            country = "Romania",
            countryCode = "RO",
            state = "Bucharest",
            language = "romanian",
            tags = listOf("rock", "classic rock", "hits"),
            votes = 5000,
            codec = "MP3",
            bitrate = 128,
            isFavorite = true,
            isHidden = true,
            lastPlayedTimestamp = 123456789L
        )

        val entity = StationEntity.fromDomain(domain)
        assertEquals("st_1", entity.stationId)
        assertTrue(entity.isFavorite)
        assertTrue(entity.isHidden)
        assertEquals(123456789L, entity.lastPlayedTimestamp)

        val restored = entity.toDomain()
        assertEquals(domain.id, restored.id)
        assertEquals(domain.name, restored.name)
        assertEquals(domain.streamUrl, restored.streamUrl)
        assertEquals(domain.favicon, restored.favicon)
        assertEquals(domain.country, restored.country)
        assertEquals(domain.countryCode, restored.countryCode)
        assertEquals(domain.state, restored.state)
        assertEquals(domain.language, restored.language)
        assertEquals(domain.tags, restored.tags)
        assertEquals(domain.votes, restored.votes)
        assertEquals(domain.codec, restored.codec)
        assertEquals(domain.bitrate, restored.bitrate)
        assertTrue(restored.isFavorite)
        assertTrue(restored.isHidden)
        assertEquals(123456789L, restored.lastPlayedTimestamp)
    }

    @Test
    fun stationEntity_handlesNullAndEmptyTagsGracefully() {
        val entity = StationEntity(
            stationId = "st_null",
            name = "Minimal",
            streamUrl = "https://minimal.stream/aac",
            favicon = null,
            country = null,
            countryCode = null,
            state = null,
            language = null,
            tags = " , , ",
            votes = 0,
            codec = null,
            bitrate = 0
        )

        val domain = entity.toDomain()
        assertTrue(domain.tags.isEmpty())
        assertFalse(domain.isFavorite)
        assertFalse(domain.isHidden)
        assertEquals(0L, domain.lastPlayedTimestamp)
        assertNull(domain.favicon)
    }

    @Test
    fun dao_hiddenStationsAreExcludedFromFavoritesAndRecents_andListedOnlyInHiddenSortedAZ() = runBlocking {
        val favVisible = Station(id = "fav_vis", name = "Alpha FM", streamUrl = "https://s/1", isFavorite = true, isHidden = false, lastPlayedTimestamp = 100L)
        val favHidden = Station(id = "fav_hid", name = "Beta FM", streamUrl = "https://s/2", isFavorite = true, isHidden = true, lastPlayedTimestamp = 300L)
        val recentVisible = Station(id = "rec_vis", name = "Gamma FM", streamUrl = "https://s/3", isFavorite = false, isHidden = false, lastPlayedTimestamp = 200L)
        val hiddenZ = Station(id = "hid_z", name = "Zebra Radio", streamUrl = "https://s/4", isFavorite = false, isHidden = true, lastPlayedTimestamp = 0L)
        val hiddenA = Station(id = "hid_a", name = "Aardvark Radio", streamUrl = "https://s/5", isFavorite = false, isHidden = true, lastPlayedTimestamp = 400L)

        listOf(favVisible, favHidden, recentVisible, hiddenZ, hiddenA).forEach {
            dao.insertOrUpdate(StationEntity.fromDomain(it))
        }

        // Favorites query must exclude favHidden
        val favSync = dao.getFavoriteStationsSync()
        assertEquals(listOf("fav_vis"), favSync.map { it.stationId })
        assertEquals(listOf("fav_vis"), dao.getFavoriteStations().first().map { it.stationId })

        // Recent query must exclude favHidden and hiddenA even though their timestamps are highest (300, 400)
        val recSync = dao.getRecentStationsSync(limit = 30)
        assertEquals(listOf("rec_vis", "fav_vis"), recSync.map { it.stationId })
        assertEquals(listOf("rec_vis", "fav_vis"), dao.getRecentStations(limit = 30).first().map { it.stationId })

        // Hidden query must contain hiddenA, favHidden, hiddenZ sorted A-Z by name
        val hidSync = dao.getHiddenStationsSync()
        assertEquals(listOf("hid_a", "fav_hid", "hid_z"), hidSync.map { it.stationId })
        assertEquals(listOf("hid_a", "fav_hid", "hid_z"), dao.getHiddenStations().first().map { it.stationId })
        assertEquals(setOf("hid_a", "fav_hid", "hid_z"), dao.getHiddenStationIdsSync().toSet())

        // Unhiding favHidden restores it immediately to both Favorites and Recent (preserving star & timestamp)
        dao.setHidden("fav_hid", false)
        assertEquals(listOf("fav_vis", "fav_hid"), dao.getFavoriteStationsSync().map { it.stationId })
        assertEquals(listOf("fav_hid", "rec_vis", "fav_vis"), dao.getRecentStationsSync().map { it.stationId })
        assertEquals(listOf("hid_a", "hid_z"), dao.getHiddenStationsSync().map { it.stationId })
    }

    @Test
    fun migration1To2_preservesExistingFavoritesAndRecentsAndAddsIsHiddenFalseDefault() = runBlocking {
        val dbFile = File(context.cacheDir, "migration_test_radio_v1.db")
        if (dbFile.exists()) dbFile.delete()

        // Create authentic Room v1 schema without isHidden column
        val v1Callback = object : SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `stations` (
                        `stationId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `streamUrl` TEXT NOT NULL,
                        `favicon` TEXT,
                        `country` TEXT,
                        `countryCode` TEXT,
                        `state` TEXT,
                        `language` TEXT,
                        `tags` TEXT,
                        `votes` INTEGER NOT NULL,
                        `codec` TEXT,
                        `bitrate` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `lastPlayedTimestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`stationId`)
                    )
                    """.trimIndent()
                )
            }

            override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
        }

        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbFile.absolutePath)
                .callback(v1Callback)
                .build()
        )
        helper.writableDatabase.use { v1Db ->
            v1Db.execSQL(
                """
                INSERT INTO stations (
                    stationId, name, streamUrl, favicon, country, countryCode, state,
                    language, tags, votes, codec, bitrate, isFavorite, lastPlayedTimestamp
                ) VALUES (
                    'legacy_fav_1', 'Europa FM', 'https://stream.europafm.ro/aac', NULL,
                    'Romania', 'RO', 'Bucharest', 'romanian', 'pop,news', 14000,
                    'AAC', 64, 1, 987654321
                )
                """.trimIndent()
            )
            v1Db.execSQL(
                """
                INSERT INTO stations (
                    stationId, name, streamUrl, favicon, country, countryCode, state,
                    language, tags, votes, codec, bitrate, isFavorite, lastPlayedTimestamp
                ) VALUES (
                    'legacy_rec_2', 'DIGI FM', 'https://stream.digifm.ro/mp3', NULL,
                    'Romania', 'RO', NULL, 'romanian', 'news', 12000,
                    'MP3', 128, 0, 999999999
                )
                """.trimIndent()
            )
        }
        helper.close()

        // Open with Room v2 + MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(context, RadioDatabase::class.java, dbFile.absolutePath)
            .addMigrations(RadioDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val migratedDao = migratedDb.stationDao()
            val favs = migratedDao.getFavoriteStationsSync()
            assertEquals(1, favs.size)
            assertEquals("legacy_fav_1", favs.first().stationId)
            assertFalse(favs.first().isHidden)
            assertEquals(987654321L, favs.first().lastPlayedTimestamp)

            val recents = migratedDao.getRecentStationsSync()
            assertEquals(listOf("legacy_rec_2", "legacy_fav_1"), recents.map { it.stationId })
            assertTrue(migratedDao.getHiddenStationsSync().isEmpty())

            // Verify post-migration hide works properly
            migratedDao.setHidden("legacy_fav_1", true)
            assertTrue(migratedDao.getFavoriteStationsSync().isEmpty())
            assertEquals(listOf("legacy_rec_2"), migratedDao.getRecentStationsSync().map { it.stationId })
            assertEquals(listOf("legacy_fav_1"), migratedDao.getHiddenStationsSync().map { it.stationId })
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }
}
