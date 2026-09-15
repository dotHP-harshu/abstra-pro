package dev.abstra.pro.ai

import android.content.Context
import androidx.compose.ui.graphics.Color
import dev.abstra.pro.ai.model.*
import dev.abstra.pro.data.db.AppDatabase
import dev.abstra.pro.data.db.entities.MarkovTransitionEntity
import dev.abstra.pro.data.db.entities.PlaybackTelemetryEntity
import dev.abstra.pro.data.model.Song
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

/**
 * 100% On-Device Offline AI & Intelligence Engine.
 * Provides Smart Shuffle, Discovery Queue, Auto Next Prediction, and 8 curated smart mixes.
 */
class AbstraAiEngine(
    private val context: Context,
    private val db: AppDatabase
) {
    private val telemetryDao = db.telemetryDao()
    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _aiMixes = MutableStateFlow<List<AiMix>>(emptyList())
    val aiMixes: StateFlow<List<AiMix>> = _aiMixes.asStateFlow()

    private var previousPlayedSongId: String? = null
    private var lastStartedSongId: String? = null
    private var lastStartedTimeMs: Long = 0L

    // In-memory telemetry cache for ultra-fast scoring
    private val telemetryCache = mutableMapOf<String, PlaybackTelemetryEntity>()

    init {
        engineScope.launch {
            val list = telemetryDao.getAllTelemetrySync()
            list.forEach { telemetryCache[it.songId] = it }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 1. TELEMETRY & HABIT LEARNING
    // ─────────────────────────────────────────────────────────────

    fun onSongStarted(song: Song) {
        val now = System.currentTimeMillis()
        val timeOfDay = TimeOfDay.current()
        val isWeekend = DayType.current() == DayType.WEEKEND

        // Record Markov transition if valid session
        previousPlayedSongId?.let { prevId ->
            if (prevId != song.id && (now - lastStartedTimeMs > 35_000L)) {
                engineScope.launch {
                    val existing = telemetryDao.getTransition(prevId, song.id)
                    val newCount = (existing?.transitionCount ?: 0) + 1
                    telemetryDao.recordTransition(MarkovTransitionEntity(prevId, song.id, newCount))
                }
            }
        }

        val isReplay = lastStartedSongId == song.id && (now - lastStartedTimeMs < 30_000L)
        previousPlayedSongId = lastStartedSongId
        lastStartedSongId = song.id
        lastStartedTimeMs = now

        engineScope.launch {
            val cur = telemetryDao.getTelemetry(song.id) ?: PlaybackTelemetryEntity(songId = song.id)
            val updated = cur.copy(
                playCount = cur.playCount + 1,
                lastPlayedTimestamp = now,
                repeatCount = if (isReplay) cur.repeatCount + 1 else cur.repeatCount,
                weekdayPlays = if (!isWeekend) cur.weekdayPlays + 1 else cur.weekdayPlays,
                weekendPlays = if (isWeekend) cur.weekendPlays + 1 else cur.weekendPlays,
                morningPlays = if (timeOfDay == TimeOfDay.MORNING) cur.morningPlays + 1 else cur.morningPlays,
                afternoonPlays = if (timeOfDay == TimeOfDay.AFTERNOON) cur.afternoonPlays + 1 else cur.afternoonPlays,
                eveningPlays = if (timeOfDay == TimeOfDay.EVENING) cur.eveningPlays + 1 else cur.eveningPlays,
                nightPlays = if (timeOfDay == TimeOfDay.NIGHT) cur.nightPlays + 1 else cur.nightPlays,
                consecutiveSkips = 0
            )
            telemetryCache[song.id] = updated
            telemetryDao.saveTelemetry(updated)
        }
    }

    fun onSongCompleted(song: Song) {
        engineScope.launch {
            val cur = telemetryDao.getTelemetry(song.id) ?: PlaybackTelemetryEntity(songId = song.id)
            val updated = cur.copy(
                fullCompletions = cur.fullCompletions + 1,
                consecutiveSkips = 0,
                totalPlayedMs = cur.totalPlayedMs + song.durationMs
            )
            telemetryCache[song.id] = updated
            telemetryDao.saveTelemetry(updated)
        }
    }

    fun onSongSkipped(song: Song, playedSeconds: Long, totalDurationSeconds: Long) {
        val isFastSkip = playedSeconds < 20 || (totalDurationSeconds > 0 && (playedSeconds.toFloat() / totalDurationSeconds.toFloat()) < 0.15f)
        if (isFastSkip) {
            engineScope.launch {
                val cur = telemetryDao.getTelemetry(song.id) ?: PlaybackTelemetryEntity(songId = song.id)
                val updated = cur.copy(
                    fastSkips = cur.fastSkips + 1,
                    consecutiveSkips = cur.consecutiveSkips + 1
                )
                telemetryCache[song.id] = updated
                telemetryDao.saveTelemetry(updated)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. DYNAMIC AFFINITY SCORING
    // ─────────────────────────────────────────────────────────────

    fun getSongAffinity(songId: String): Float {
        val t = telemetryCache[songId] ?: return 5.0f
        val now = System.currentTimeMillis()
        val timeOfDay = TimeOfDay.current()
        val isWeekend = DayType.current() == DayType.WEEKEND

        var score = (t.playCount * 3.0f) + (t.fullCompletions * 2.0f) - (t.fastSkips * 2.5f) - (t.consecutiveSkips * 4.0f)

        // Time of day bonus
        val timePlays = when (timeOfDay) {
            TimeOfDay.MORNING -> t.morningPlays
            TimeOfDay.AFTERNOON -> t.afternoonPlays
            TimeOfDay.EVENING -> t.eveningPlays
            TimeOfDay.NIGHT -> t.nightPlays
        }
        score += timePlays * 1.5f

        // Weekend vs weekday bonus
        if (isWeekend) score += t.weekendPlays * 1.2f else score += t.weekdayPlays * 1.0f

        // Recency decay: played within last 2 days gets a modest freshness bump
        val daysSincePlayed = if (t.lastPlayedTimestamp > 0) (now - t.lastPlayedTimestamp) / (1000L * 60 * 60 * 24) else 30L
        if (daysSincePlayed in 0..2) score += 3.0f

        return score.coerceAtLeast(0.5f)
    }

    // ─────────────────────────────────────────────────────────────
    // 3. MATHEMATICAL SIMILARITY METRIC
    // ─────────────────────────────────────────────────────────────

    fun computeSongSimilarity(a: Song, b: Song): Float {
        if (a.id == b.id) return 1.0f

        var similarity = 0.0f

        // Same artist bonus (+0.42)
        if (a.artist.equals(b.artist, ignoreCase = true) && a.artist != "Unknown Artist") {
            similarity += 0.42f
        }

        // Genre compatibility bonus (+0.35 / +0.22)
        val genreA = a.genre?.trim()?.lowercase(Locale.getDefault())
        val genreB = b.genre?.trim()?.lowercase(Locale.getDefault())
        if (!genreA.isNullOrEmpty() && !genreB.isNullOrEmpty()) {
            if (genreA == genreB) {
                similarity += 0.35f
            } else if (isCompatibleGenre(genreA, genreB)) {
                similarity += 0.22f
            }
        }

        // Acoustic profile harmonic distance (+0.23)
        val profileA = AcousticProfiler.profileSong(a)
        val profileB = AcousticProfiler.profileSong(b)
        val energyDelta = abs(profileA.energyScore - profileB.energyScore)
        val acousticDelta = abs(profileA.acousticScore - profileB.acousticScore)
        val profileHarmony = 1.0f - ((energyDelta * 0.6f) + (acousticDelta * 0.4f))
        similarity += (profileHarmony * 0.23f).coerceIn(0f, 0.23f)

        return similarity.coerceIn(0f, 1f)
    }

    private fun isCompatibleGenre(g1: String, g2: String): Boolean {
        val rockFamily = setOf("rock", "alternative", "indie", "metal", "punk", "grunge", "hard rock")
        val popFamily = setOf("pop", "dance", "electropop", "synthpop", "indie pop", "disco")
        val electronicFamily = setOf("electronic", "edm", "house", "techno", "synthwave", "ambient", "trance")
        val urbanFamily = setOf("hip hop", "rap", "trap", "r&b", "soul", "reggaeton", "urban")
        val chillFamily = setOf("acoustic", "folk", "classical", "instrumental", "ambient", "lo-fi", "chill", "jazz")

        return (g1 in rockFamily && g2 in rockFamily) ||
                (g1 in popFamily && g2 in popFamily) ||
                (g1 in electronicFamily && g2 in electronicFamily) ||
                (g1 in urbanFamily && g2 in urbanFamily) ||
                (g1 in chillFamily && g2 in chillFamily)
    }

    // ─────────────────────────────────────────────────────────────
    // 4. SMART SHUFFLE & HARMONIC FLOW REORDERING
    // ─────────────────────────────────────────────────────────────

    fun generateSmartShuffle(songs: List<Song>, startingSong: Song? = null): List<Song> {
        if (songs.size <= 2) return songs

        val remaining = songs.toMutableList()
        val result = mutableListOf<Song>()

        val seed = startingSong?.let { s -> remaining.find { it.id == s.id } }
            ?: remaining.maxByOrNull { getSongAffinity(it.id) }
            ?: remaining.first()

        result.add(seed)
        remaining.remove(seed)

        var current = seed
        val random = Random.Default

        while (remaining.isNotEmpty()) {
            val scoredCandidates = remaining.map { candidate ->
                val similarity = computeSongSimilarity(current, candidate)
                val affinity = getSongAffinity(candidate.id)
                val sameArtistPenalty = if (candidate.artist.equals(current.artist, ignoreCase = true)) -0.35f else 0f
                val noise = random.nextFloat() * 0.08f

                val finalScore = (similarity * 0.48f) + ((affinity / 45f).coerceIn(0f, 0.44f)) + sameArtistPenalty + noise
                candidate to finalScore
            }.sortedByDescending { it.second }

            val topSliceSize = minOf(3, scoredCandidates.size)
            val selected = scoredCandidates.take(topSliceSize).random(random).first

            result.add(selected)
            remaining.remove(selected)
            current = selected
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // 5. DISCOVERY QUEUE (2:1 RATIO INTERLEAVING)
    // ─────────────────────────────────────────────────────────────

    fun generateDiscoveryQueue(allSongs: List<Song>, seedSong: Song? = null): List<Song> {
        if (allSongs.isEmpty()) return emptyList()

        val familiar = allSongs.filter { getSongAffinity(it.id) >= 12f }
        val unexplored = allSongs.filter {
            val t = telemetryCache[it.id]
            (t == null || t.playCount <= 1) && !it.isFavorite
        }

        if (unexplored.isEmpty()) return generateSmartShuffle(allSongs, seedSong)

        val seed = seedSong ?: familiar.maxByOrNull { getSongAffinity(it.id) } ?: allSongs.first()
        val result = mutableListOf(seed)

        val rankedUnexplored = unexplored.map { candidate ->
            val maxSimToFavorites = familiar.maxOfOrNull { computeSongSimilarity(it, candidate) } ?: 0.3f
            candidate to maxSimToFavorites
        }.sortedByDescending { it.second }

        val familiarPool = (familiar - seed).toMutableList()
        val unexploredPool = rankedUnexplored.map { it.first }.toMutableList()

        var flip = 0
        while (familiarPool.isNotEmpty() || unexploredPool.isNotEmpty()) {
            if (flip % 3 == 2 && unexploredPool.isNotEmpty()) {
                result.add(unexploredPool.removeAt(0))
            } else if (familiarPool.isNotEmpty()) {
                val last = result.last()
                val nextFam = familiarPool.maxByOrNull { computeSongSimilarity(last, it) } ?: familiarPool.first()
                familiarPool.remove(nextFam)
                result.add(nextFam)
            } else if (unexploredPool.isNotEmpty()) {
                result.add(unexploredPool.removeAt(0))
            }
            flip++
        }

        return result
    }

    // ─────────────────────────────────────────────────────────────
    // 6. NEXT TRACK PREDICTION
    // ─────────────────────────────────────────────────────────────

    suspend fun predictNextSong(currentSong: Song, history: List<Song>, pool: List<Song>): Song? {
        val eligible = pool.filter { candidate ->
            candidate.id != currentSong.id && history.none { it.id == candidate.id }
        }
        if (eligible.isEmpty()) return pool.filter { it.id != currentSong.id }.randomOrNull()

        val recentArtists = history.takeLast(3).map { it.artist.lowercase(Locale.getDefault()) }
        val transitions = telemetryDao.getTransitionsFrom(currentSong.id).associateBy { it.toSongId }

        return eligible.maxByOrNull { candidate ->
            val similarity = computeSongSimilarity(currentSong, candidate)
            val affinity = getSongAffinity(candidate.id)
            val isRecentArtist = candidate.artist.lowercase(Locale.getDefault()) in recentArtists
            val artistPenalty = if (isRecentArtist) -0.45f else 0f
            val markovCount = transitions[candidate.id]?.transitionCount ?: 0
            val markovBonus = (markovCount * 0.10f).coerceAtMost(0.35f)

            (similarity * 0.45f) + ((affinity / 40f).coerceIn(0f, 0.40f)) + artistPenalty + markovBonus
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 7. REFRESH ALL 8 AI MIXES
    // ─────────────────────────────────────────────────────────────

    fun regenerateMixes(allSongs: List<Song>) = refreshMixes(allSongs)

    fun refreshMixes(allSongs: List<Song>) {
        if (allSongs.isEmpty()) return

        engineScope.launch {
            val generated = mutableListOf<AiMix>()
            val currentTime = TimeOfDay.current()
            val isWeekend = DayType.current() == DayType.WEEKEND

            // 1. Daily AI Flow
            val dailySongs = allSongs.sortedByDescending { getSongAffinity(it.id) }.take(30)
            if (dailySongs.size >= 3) {
                generated.add(
                    AiMix(
                        id = "ai_daily_flow",
                        title = "Daily AI Flow",
                        subtitle = "Harmonic selection for your daily routine",
                        category = MixCategory.DAILY_FLOW,
                        songs = generateSmartShuffle(dailySongs),
                        gradientColors = listOf(Color(0xFFF06FA0), Color(0xFFE14E86), Color(0xFFD7BFF3)),
                        iconName = "auto_awesome",
                        description = "Synthesized in real-time combining listening habits, affinity, and smooth acoustic transitions."
                    )
                )
            }

            // 2. Deep Discovery
            val discoverySongs = generateDiscoveryQueue(allSongs).take(25)
            if (discoverySongs.size >= 3) {
                generated.add(
                    AiMix(
                        id = "ai_discovery",
                        title = "Deep Discovery",
                        subtitle = "Hidden gems you might love",
                        category = MixCategory.DISCOVERY,
                        songs = discoverySongs,
                        gradientColors = listOf(Color(0xFFFD9692), Color(0xFFF26F62), Color(0xFFFDECE8)),
                        iconName = "explore",
                        description = "Interleaves high-affinity favorite tracks with unexplored tracks of matching acoustic profiles."
                    )
                )
            }

            // 3. Energy Boost
            val energySongs = allSongs.filter {
                val profile = AcousticProfiler.profileSong(it)
                profile.energyScore > 0.60f || (it.durationMs < 220_000L && getSongAffinity(it.id) > 10f)
            }.sortedByDescending { getSongAffinity(it.id) }.take(25)

            if (energySongs.size >= 3) {
                generated.add(
                    AiMix(
                        id = "ai_energy",
                        title = "Energy Boost",
                        subtitle = "High-tempo workout and cardio beats",
                        category = MixCategory.ENERGY,
                        songs = generateSmartShuffle(energySongs),
                        gradientColors = listOf(Color(0xFFFF4D7E), Color(0xFFFF6F5E), Color(0xFFF06FA0)),
                        iconName = "bolt",
                        description = "High-energy tracks with punchy dynamics for exercise, movement, and focus."
                    )
                )
            }

            // 4. Chill & Focus
            val chillSongs = allSongs.filter {
                val profile = AcousticProfiler.profileSong(it)
                profile.energyScore < 0.48f || profile.isFocusFriendly
            }.sortedByDescending { getSongAffinity(it.id) }.take(25)

            if (chillSongs.size >= 3) {
                generated.add(
                    AiMix(
                        id = "ai_chill",
                        title = "Chill & Focus",
                        subtitle = "Lo-fi, ambient, and peaceful study textures",
                        category = MixCategory.CHILL,
                        songs = generateSmartShuffle(chillSongs),
                        gradientColors = listOf(Color(0xFF8D517B), Color(0xFFCCB0E9), Color(0xFF43283D)),
                        iconName = "spa",
                        description = "Soothing acoustic and ambient frequencies tailored for reading, working, and resting."
                    )
                )
            }

            // 5. Contextual Time / Weekend Mix
            val timeName = when {
                isWeekend -> "Weekend Escape"
                currentTime == TimeOfDay.MORNING -> "Morning Radiance"
                currentTime == TimeOfDay.AFTERNOON -> "Afternoon Groove"
                currentTime == TimeOfDay.EVENING -> "Evening Wind Down"
                else -> "Midnight Echoes"
            }
            val timeSub = when {
                isWeekend -> "Carefree weekend soundtrack"
                currentTime == TimeOfDay.MORNING -> "Gentle rhythms to start the day"
                currentTime == TimeOfDay.AFTERNOON -> "Uplifting midday beats"
                currentTime == TimeOfDay.EVENING -> "Mellow sunset vibes"
                else -> "Deep late-night listening"
            }
            val timeGradients = when (currentTime) {
                TimeOfDay.MORNING -> listOf(Color(0xFFF9A1B2), Color(0xFFFEDFDD), Color(0xFFEE7A91))
                TimeOfDay.AFTERNOON -> listOf(Color(0xFFFD9692), Color(0xFFFDECE8), Color(0xFFF06FA0))
                TimeOfDay.EVENING -> listOf(Color(0xFFE5819E), Color(0xFF443358), Color(0xFF1D1A2D))
                TimeOfDay.NIGHT -> listOf(Color(0xFF241825), Color(0xFF43283D), Color(0xFF8D517B))
            }

            val timeSongs = allSongs.sortedByDescending { getSongAffinity(it.id) }.take(25)
            if (timeSongs.size >= 3) {
                generated.add(
                    AiMix(
                        id = "ai_time_context",
                        title = timeName,
                        subtitle = timeSub,
                        category = MixCategory.TIME_OF_DAY,
                        songs = generateSmartShuffle(timeSongs),
                        gradientColors = timeGradients,
                        iconName = if (currentTime == TimeOfDay.NIGHT) "bedtime" else "wb_sunny",
                        description = "Adapted to the current time of day based on your past listening patterns."
                    )
                )
            }

            // 6. Forgotten Gems
            val now = System.currentTimeMillis()
            val twoWeeksAgo = now - (14L * 24L * 60L * 60L * 1000L)
            val forgottenSongs = allSongs.filter { song ->
                val t = telemetryCache[song.id]
                (song.isFavorite || (t != null && t.playCount >= 2)) &&
                        (t == null || t.lastPlayedTimestamp == 0L || t.lastPlayedTimestamp <= twoWeeksAgo)
            }.shuffled().take(22)

            if (forgottenSongs.size >= 3) {
                generated.add(
                    AiMix(
                        id = "ai_forgotten",
                        title = "Forgotten Gems",
                        subtitle = "Tracks you once loved, revisited",
                        category = MixCategory.FORGOTTEN_GEMS,
                        songs = forgottenSongs,
                        gradientColors = listOf(Color(0xFFB9706A), Color(0xFF6A423C), Color(0xFFFBDDC6)),
                        iconName = "history",
                        description = "Re-discovers high-affinity tracks that have slipped out of recent rotation."
                    )
                )
            }

            // 7. Acoustic & Live Sessions
            val acousticSongs = allSongs.filter { AcousticProfiler.profileSong(it).isLiveOrAcoustic }
            if (acousticSongs.size >= 3) {
                generated.add(
                    AiMix(
                        id = "ai_acoustic",
                        title = "Acoustic & Live",
                        subtitle = "Unplugged instruments and concert recordings",
                        category = MixCategory.ACOUSTIC_LIVE,
                        songs = generateSmartShuffle(acousticSongs),
                        gradientColors = listOf(Color(0xFFB06070), Color(0xFFE0B8C0), Color(0xFFF2748C)),
                        iconName = "mic",
                        description = "Raw acoustic guitar, piano, and live sessions detected from your library."
                    )
                )
            }

            // 8. Top Artist Spotlight
            val topArtistEntry = allSongs
                .groupBy { it.artist }
                .filter { it.key.isNotBlank() && it.key != "Unknown Artist" && it.value.size >= 2 }
                .maxByOrNull { entry -> entry.value.sumOf { getSongAffinity(it.id).toDouble() } }

            if (topArtistEntry != null) {
                val artistSongs = topArtistEntry.value
                val kindredSongs = allSongs.filter {
                    it.artist != topArtistEntry.key && computeSongSimilarity(artistSongs.first(), it) > 0.38f
                }
                val pool = (artistSongs + kindredSongs.take(15)).distinctBy { it.id }
                if (pool.size >= 3) {
                    generated.add(
                        AiMix(
                            id = "ai_artist_spotlight",
                            title = "${topArtistEntry.key} Radio",
                            subtitle = "Spotlight on ${topArtistEntry.key} & kindred tracks",
                            category = MixCategory.ARTIST_SPOTLIGHT,
                            songs = generateSmartShuffle(pool),
                            gradientColors = listOf(Color(0xFFE14E86), Color(0xFFF06FA0), Color(0xFFFFF0F5)),
                            iconName = "person",
                            description = "A continuous session anchored around your most listened artist and acoustically kindred songs."
                        )
                    )
                }
            }

            _aiMixes.value = generated
        }
    }
}
