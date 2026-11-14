package com.worldradio.auto.data.repository

import com.worldradio.auto.data.local.StationDao
import com.worldradio.auto.data.local.StationEntity
import com.worldradio.auto.data.model.Country
import com.worldradio.auto.data.model.CuratedStations
import com.worldradio.auto.data.model.Station
import com.worldradio.auto.data.model.StationDto
import com.worldradio.auto.data.remote.RadioBrowserApi
import com.worldradio.auto.util.UserLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale

class RadioRepository(
    private val api: RadioBrowserApi,
    private val dao: StationDao
) {

    /**
     * Favorites tab: Alphabetical sorting by station name
     */
    fun getFavoriteStations(): Flow<List<Station>> {
        return dao.getFavoriteStations().map { list ->
            list.map { it.toDomain() }.sortedBy { it.name.lowercase() }
        }
    }

    suspend fun getFavoriteStationsSync(): List<Station> = withContext(Dispatchers.IO) {
        dao.getFavoriteStationsSync()
            .map { it.toDomain() }
            .sortedBy { it.name.lowercase() }
    }

    fun getAllFavoriteStationIds(): Flow<Set<String>> {
        return dao.getAllFavoriteStationIds().map { it.toSet() }
    }

    /**
     * Recent tab: Chronological order by last played timestamp (NOT alphabetical)
     */
    fun getRecentStations(limit: Int = 30): Flow<List<Station>> {
        return dao.getRecentStations(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getRecentStationsSync(limit: Int = 30): List<Station> = withContext(Dispatchers.IO) {
        dao.getRecentStationsSync(limit).map { it.toDomain() }
    }

    /**
     * Hidden tab: Stations explicitly hidden by the user, sorted alphabetically
     */
    fun getHiddenStations(): Flow<List<Station>> {
        return dao.getHiddenStations().map { list ->
            list.map { it.toDomain() }.sortedBy { it.name.lowercase() }
        }
    }

    suspend fun getHiddenStationsSync(): List<Station> = withContext(Dispatchers.IO) {
        dao.getHiddenStationsSync()
            .map { it.toDomain() }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun getHiddenStationIdsSync(): Set<String> = withContext(Dispatchers.IO) {
        dao.getHiddenStationIdsSync().toSet()
    }

    /**
     * Popular tab: Sorted by votes / listener rank (NOT alphabetical)
     */
    suspend fun getPopularStations(): Result<List<Station>> = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = api.getTopVotedStations(limit = 70)
            mapAndEnrichStations(dtos).sortedByDescending { it.votes }
        }
    }

    /**
     * Local tab: Stations strictly from around the user's specific location (city/region).
     * Eliminates redundancy with the World country tab!
     */
    suspend fun getLocalStationsAroundLocation(location: UserLocation): Result<List<Station>> = withContext(Dispatchers.IO) {
        runCatching {
            val countryCode = location.countryCode.ifBlank { "RO" }
            val region = location.region.trim()
            val city = location.city.trim()

            val allDtos = api.getStationsByCountry(countryCode = countryCode, limit = 400)
            val enriched = mapAndEnrichStations(allDtos)

            val searchTerms = buildList {
                if (region.isNotBlank()) {
                    add(region.lowercase())
                    if (region.contains("Bucharest", ignoreCase = true) || region.contains("Bucuresti", ignoreCase = true) || region.contains("Ilfov", ignoreCase = true)) {
                        add("bucharest")
                        add("bucuresti")
                        add("ilfov")
                    }
                    if (region.contains("Timis", ignoreCase = true) || region.contains("Timisoara", ignoreCase = true)) {
                        add("timis")
                        add("timisoara")
                    }
                    if (region.contains("Cluj", ignoreCase = true)) {
                        add("cluj")
                    }
                    if (region.contains("Vrancea", ignoreCase = true) || region.contains("Focsani", ignoreCase = true)) {
                        add("vrancea")
                        add("focsani")
                        add("adjud")
                    }
                    if (region.contains("Brasov", ignoreCase = true)) {
                        add("brasov")
                    }
                    if (region.contains("Constanta", ignoreCase = true)) {
                        add("constanta")
                    }
                    if (region.contains("Iasi", ignoreCase = true)) {
                        add("iasi")
                    }
                    if (region.contains("Dolj", ignoreCase = true) || region.contains("Craiova", ignoreCase = true)) {
                        add("dolj")
                        add("craiova")
                    }
                    if (region.contains("Galati", ignoreCase = true) || region.contains("Braila", ignoreCase = true)) {
                        add("galati")
                        add("braila")
                    }
                }
                if (city.isNotBlank()) {
                    add(city.lowercase())
                }
            }.distinct()

            val matched = enriched.filter { station ->
                val stateLower = station.state?.lowercase() ?: ""
                val nameLower = station.name.lowercase()
                val tagsLower = station.tags.joinToString(" ").lowercase()

                searchTerms.any { term ->
                    stateLower.contains(term) || nameLower.contains(term) || tagsLower.contains(term)
                }
            }

            val regionalCurated = CuratedStations.getRegionalStationsFor(region, city)
            val hiddenIds = dao.getHiddenStationIdsSync().toSet()

            val combined = (regionalCurated + matched)
                .distinctBy { it.id }
                .filter { it.id !in hiddenIds }
                .sortedBy { it.name.lowercase() }

            if (combined.isNotEmpty()) {
                combined
            } else {
                regionalCurated.ifEmpty { CuratedStations.ROMANIA_STARTER_STATIONS }
                    .filter { it.id !in hiddenIds }
                    .sortedBy { it.name.lowercase() }
            }
        }
    }

    /**
     * Local tab: Scenario 3 (Option B) - Ranked by real-time Stream Clicks (highest clicks to lowest, including all stations).
     */
    suspend fun getLocalStations(countryCode: String = "RO"): Result<List<Station>> = withContext(Dispatchers.IO) {
        runCatching {
            val code = countryCode.trim().uppercase(Locale.ROOT).ifBlank { "RO" }
            val dtos = api.getStationsByCountry(
                countryCode = code,
                limit = 500,
                order = "clickcount",
                reverse = true
            )
            val enriched = mapAndEnrichStations(dtos)
            // Option B: Show all stations ordered by stream clicks descending, then by votes
            enriched.sortedWith(
                compareByDescending<Station> { it.clickCount }
                    .thenByDescending { it.votes }
            )
        }
    }

    /**
     * World tab - Countries list: Alphabetical sorting by country name
     */
    suspend fun getCountries(): Result<List<Country>> = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = api.getCountries()
            dtos.filter { it.stationCount > 10 && it.code.isNotBlank() }
                .map { Country(name = it.name.trim(), code = it.code.trim().uppercase(), stationCount = it.stationCount) }
                .sortedBy { it.name.lowercase() }
        }
    }

    /**
     * World tab - Stations within country: Scenario 2 - Ranked by Lifetime Popularity (Upvotes descending)
     */
    suspend fun getStationsByCountry(countryCode: String): Result<List<Station>> = withContext(Dispatchers.IO) {
        runCatching {
            val code = countryCode.trim().uppercase(Locale.ROOT).ifBlank { "RO" }
            val dtos = api.getStationsByCountry(
                countryCode = code,
                limit = 500,
                order = "votes",
                reverse = true
            )
            // Scenario 2: Sorted by Votes (Popularity Rank) descending
            mapAndEnrichStations(dtos).sortedByDescending { it.votes }
        }
    }

    /**
     * Search results: Alphabetical sorting by station name
     */
    suspend fun searchStations(query: String): Result<List<Station>> = withContext(Dispatchers.IO) {
        runCatching {
            val dtos = api.searchStationsByName(name = query.trim(), limit = 60)
            mapAndEnrichStations(dtos).sortedBy { it.name.lowercase() }
        }
    }

    suspend fun toggleFavorite(station: Station) = withContext(Dispatchers.IO) {
        val currentEntity = dao.getStationById(station.id)
        val newFav = !(currentEntity?.isFavorite ?: station.isFavorite)
        if (currentEntity != null) {
            dao.setFavorite(station.id, newFav)
        } else {
            val entity = StationEntity.fromDomain(
                station = station,
                isFavorite = newFav,
                isHidden = station.isHidden
            )
            dao.insertOrUpdate(entity)
        }
    }

    suspend fun toggleHidden(station: Station): Boolean = withContext(Dispatchers.IO) {
        val currentEntity = dao.getStationById(station.id)
        val newHidden = !(currentEntity?.isHidden ?: station.isHidden)
        if (currentEntity != null) {
            dao.setHidden(station.id, newHidden)
        } else {
            val entity = StationEntity.fromDomain(
                station = station,
                isFavorite = station.isFavorite,
                isHidden = newHidden
            )
            dao.insertOrUpdate(entity)
        }
        newHidden
    }

    suspend fun recordPlayed(station: Station) = withContext(Dispatchers.IO) {
        val currentEntity = dao.getStationById(station.id)
        val isFav = currentEntity?.isFavorite ?: station.isFavorite
        val isHidden = currentEntity?.isHidden ?: station.isHidden
        val entity = StationEntity.fromDomain(
            station = station,
            isFavorite = isFav,
            isHidden = isHidden,
            lastPlayed = System.currentTimeMillis()
        )
        dao.insertOrUpdate(entity)
    }

    suspend fun getStationById(id: String): Station? = withContext(Dispatchers.IO) {
        dao.getStationById(id)?.toDomain()
    }

    private suspend fun mapAndEnrichStations(dtos: List<StationDto>): List<Station> {
        val favorites = dao.getFavoriteStationsSync().associateBy { it.stationId }
        val hiddenIds = dao.getHiddenStationIdsSync().toSet()
        return dtos
            .filter { it.stationuuid !in hiddenIds }
            .map { dto ->
                val isFav = favorites.containsKey(dto.stationuuid)
                Station(
                    id = dto.stationuuid,
                    name = dto.name.trim(),
                    streamUrl = dto.urlResolved?.ifBlank { dto.url } ?: dto.url,
                    favicon = dto.favicon?.ifBlank { null },
                    country = dto.country?.trim()?.ifBlank { null },
                    countryCode = dto.countrycode?.trim()?.uppercase()?.ifBlank { null },
                    state = dto.state?.trim()?.ifBlank { null },
                    language = dto.language?.trim()?.ifBlank { null },
                    tags = dto.tags?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
                    votes = dto.votes ?: 0,
                    clickCount = dto.clickcount ?: 0,
                    codec = dto.codec?.trim()?.ifBlank { null },
                    bitrate = dto.bitrate ?: 0,
                    isFavorite = isFav,
                    isHidden = false
                )
            }
    }
}
