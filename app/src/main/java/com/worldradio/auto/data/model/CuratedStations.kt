package com.worldradio.auto.data.model

object CuratedStations {

    val ROMANIA_STARTER_STATIONS = listOf(
        Station(
            id = "curated_ro_1",
            name = "Radio România Actualități",
            streamUrl = "https://stream2.srr.ro:8443/rra.mp3",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("News", "Public", "Culture"),
            codec = "MP3",
            bitrate = 128,
            votes = 26888
        ),
        Station(
            id = "curated_ro_2",
            name = "Europa FM",
            streamUrl = "https://astreaming.europafm.ro:8443/europafm_aacp48k",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("Pop", "News", "Hits"),
            codec = "AAC",
            bitrate = 64,
            votes = 14568
        ),
        Station(
            id = "curated_ro_3",
            name = "DIGI FM",
            streamUrl = "https://edge126.rdsnet.ro/digifm/digifm.mp3",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("News", "Talk", "Adult Contemporary"),
            codec = "MP3",
            bitrate = 128,
            votes = 14432
        ),
        Station(
            id = "curated_ro_4",
            name = "Kiss FM Ro",
            streamUrl = "https://live.kissfm.ro/kissfm.aacp",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("Top 40", "Pop", "Dance"),
            codec = "AAC",
            bitrate = 80,
            votes = 13783
        ),
        Station(
            id = "curated_ro_5",
            name = "Pro FM",
            streamUrl = "https://astreaming.pro-fm.ro:8443/profm.aacp",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("Hits", "Dance", "Pop"),
            codec = "AAC",
            bitrate = 64,
            votes = 11009
        ),
        Station(
            id = "curated_ro_6",
            name = "Dance FM 89.5 Bucharest",
            streamUrl = "https://edge126.rdsnet.ro/dancefm/dancefm.mp3",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("Electronic", "Dance", "Club"),
            codec = "MP3",
            bitrate = 192,
            votes = 21316
        )
    )

    val GLOBAL_POPULAR_STATIONS = listOf(
        Station(
            id = "curated_global_1",
            name = "Radio România Actualități",
            streamUrl = "https://stream2.srr.ro:8443/rra.mp3",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("News", "Public"),
            codec = "MP3",
            bitrate = 128,
            votes = 26888
        ),
        Station(
            id = "curated_global_2",
            name = "Dance FM 89.5 Bucharest",
            streamUrl = "https://edge126.rdsnet.ro/dancefm/dancefm.mp3",
            country = "Romania",
            countryCode = "RO",
            tags = listOf("Dance", "House"),
            codec = "MP3",
            bitrate = 192,
            votes = 21316
        ),
        Station(
            id = "curated_global_3",
            name = "Radio Paradise Rock",
            streamUrl = "https://stream.radioparadise.com/rock-128",
            country = "United States",
            countryCode = "US",
            tags = listOf("Rock", "Alternative"),
            codec = "MP3",
            bitrate = 320,
            votes = 18500
        ),
        Station(
            id = "curated_global_4",
            name = "Ibiza Global Radio",
            streamUrl = "https://listenssl.ibizaglobalradio.com:8024/stream",
            country = "Spain",
            countryCode = "ES",
            tags = listOf("Electronic", "Chillout"),
            codec = "MP3",
            bitrate = 256,
            votes = 16200
        ),
        Station(
            id = "curated_global_5",
            name = "BBC Radio 1",
            streamUrl = "https://stream.live.vc.bbcmedia.co.uk/bbc_radio_one",
            country = "United Kingdom",
            countryCode = "GB",
            tags = listOf("Top 40", "Pop"),
            codec = "AAC",
            bitrate = 128,
            votes = 15900
        )
    )

    val FALLBACK_COUNTRIES = listOf(
        Country("Romania", "RO", 482),
        Country("Germany", "DE", 2340),
        Country("United Kingdom", "GB", 1850),
        Country("United States", "US", 5400),
        Country("France", "FR", 1200),
        Country("Spain", "ES", 980),
        Country("Italy", "IT", 850),
        Country("Moldova", "MD", 120),
        Country("Switzerland", "CH", 420),
        Country("Austria", "AT", 360),
        Country("Netherlands", "NL", 890),
        Country("Japan", "JP", 650)
    )

    fun getRegionalStationsFor(region: String, city: String): List<Station> {
        val reg = region.lowercase()
        val c = city.lowercase()
        return when {
            reg.contains("vrancea") || c.contains("focsani") || c.contains("adjud") -> listOf(
                Station(
                    id = "curated_vrancea_1",
                    name = "Adjud FM",
                    streamUrl = "https://live.adjudfm.ro:8000/adjudfm.aac",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Vrancea",
                    tags = listOf("Local", "Pop", "Dance"),
                    codec = "AAC",
                    bitrate = 128
                ),
                Station(
                    id = "curated_vrancea_2",
                    name = "Focus FM Regional (Vrancea / Buzău)",
                    streamUrl = "http://live.focusfm.ro:8000/focusfmlow.ogg",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Vrancea",
                    tags = listOf("Regional", "News", "Hits"),
                    codec = "OGG",
                    bitrate = 96
                ),
                Station(
                    id = "curated_vrancea_3",
                    name = "Radio Dada Focșani",
                    streamUrl = "https://live.laris.ro:8004/radiodada128.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Vrancea",
                    tags = listOf("Local", "Rock", "Jazz"),
                    codec = "MP3",
                    bitrate = 128
                ),
                Station(
                    id = "curated_moldova_iasi",
                    name = "Radio România Iași (Regional)",
                    streamUrl = "https://stream2.srr.ro:8443/iasi.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Moldova",
                    tags = listOf("Regional", "News", "Culture"),
                    codec = "MP3",
                    bitrate = 128
                )
            )
            reg.contains("bucuresti") || reg.contains("bucharest") || reg.contains("ilfov") || c.contains("bucuresti") || c.contains("bucharest") -> listOf(
                Station(
                    id = "curated_bucuresti_1",
                    name = "Radio România București FM",
                    streamUrl = "https://stream2.srr.ro:8443/bucuresti.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "București",
                    tags = listOf("Local", "News", "Hits"),
                    codec = "MP3",
                    bitrate = 128
                ),
                Station(
                    id = "curated_bucuresti_2",
                    name = "Dance FM 89.5 Bucharest",
                    streamUrl = "https://edge126.rdsnet.ro/dancefm/dancefm.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "București",
                    tags = listOf("Local", "Dance", "Electronic"),
                    codec = "MP3",
                    bitrate = 192
                ),
                Station(
                    id = "curated_bucuresti_3",
                    name = "Romantic FM București",
                    streamUrl = "https://astreaming.romanticfm.ro:8443/romanticfm.aacp",
                    country = "Romania",
                    countryCode = "RO",
                    state = "București",
                    tags = listOf("Local", "Easy Listening", "Love Songs"),
                    codec = "AAC",
                    bitrate = 64
                ),
                Station(
                    id = "curated_bucuresti_4",
                    name = "Gold FM București",
                    streamUrl = "https://live.goldfm.ro:8443/goldfm.aacp",
                    country = "Romania",
                    countryCode = "RO",
                    state = "București",
                    tags = listOf("Local", "Classic Rock", "Hits"),
                    codec = "AAC",
                    bitrate = 64
                )
            )
            reg.contains("cluj") || c.contains("cluj") -> listOf(
                Station(
                    id = "curated_cluj_1",
                    name = "Radio România Cluj",
                    streamUrl = "https://stream2.srr.ro:8443/cluj.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Cluj",
                    tags = listOf("Local", "News", "Culture"),
                    codec = "MP3",
                    bitrate = 128
                ),
                Station(
                    id = "curated_cluj_2",
                    name = "Radio Transilvania Cluj",
                    streamUrl = "https://stream.radiotransilvania.ro:8000/cluj",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Cluj",
                    tags = listOf("Local", "Pop", "Hits"),
                    codec = "MP3",
                    bitrate = 128
                )
            )
            reg.contains("timis") || c.contains("timisoara") -> listOf(
                Station(
                    id = "curated_timis_1",
                    name = "Radio România Timișoara",
                    streamUrl = "https://stream2.srr.ro:8443/timisoara.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Timiș",
                    tags = listOf("Local", "News", "Culture"),
                    codec = "MP3",
                    bitrate = 128
                ),
                Station(
                    id = "curated_resita_1",
                    name = "Radio România Reșița",
                    streamUrl = "https://stream2.srr.ro:8443/resita.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Caraș-Severin",
                    tags = listOf("Regional", "News", "Culture"),
                    codec = "MP3",
                    bitrate = 128
                )
            )
            reg.contains("brasov") || c.contains("brasov") -> listOf(
                Station(
                    id = "curated_brasov_1",
                    name = "Radio România Brașov FM",
                    streamUrl = "https://stream2.srr.ro:8443/brasov.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Brașov",
                    tags = listOf("Local", "News", "Hits"),
                    codec = "MP3",
                    bitrate = 128
                ),
                Station(
                    id = "curated_brasov_2",
                    name = "Radio Brașov",
                    streamUrl = "https://live.radiobrasov.ro/live",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Brașov",
                    tags = listOf("Local", "Hits", "Pop"),
                    codec = "MP3",
                    bitrate = 128
                )
            )
            reg.contains("constanta") || c.contains("constanta") -> listOf(
                Station(
                    id = "curated_constanta_1",
                    name = "Radio România Constanța",
                    streamUrl = "https://stream2.srr.ro:8443/constanta.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Constanța",
                    tags = listOf("Local", "News", "Litoral"),
                    codec = "MP3",
                    bitrate = 128
                ),
                Station(
                    id = "curated_constanta_2",
                    name = "Radio Sky Constanța",
                    streamUrl = "https://live.radiosky.ro:8443/sky",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Constanța",
                    tags = listOf("Local", "Dance", "Hits"),
                    codec = "MP3",
                    bitrate = 128
                )
            )
            reg.contains("craiova") || reg.contains("dolj") || c.contains("craiova") -> listOf(
                Station(
                    id = "curated_craiova_1",
                    name = "Radio România Oltenia Craiova",
                    streamUrl = "https://stream2.srr.ro:8443/craiova.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Dolj",
                    tags = listOf("Local", "News", "Culture"),
                    codec = "MP3",
                    bitrate = 128
                )
            )
            reg.contains("iasi") || c.contains("iasi") -> listOf(
                Station(
                    id = "curated_iasi_1",
                    name = "Radio România Iași",
                    streamUrl = "https://stream2.srr.ro:8443/iasi.mp3",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Iași",
                    tags = listOf("Local", "News", "Culture"),
                    codec = "MP3",
                    bitrate = 128
                ),
                Station(
                    id = "curated_iasi_2",
                    name = "Viva FM Iași",
                    streamUrl = "https://live.radiovivafm.ro:8443/vivafmiasi",
                    country = "Romania",
                    countryCode = "RO",
                    state = "Iași",
                    tags = listOf("Local", "Hits", "Pop"),
                    codec = "MP3",
                    bitrate = 128
                )
            )
            else -> emptyList()
        }
    }
}
