package dev.abstra.pro.data.model

import java.util.UUID

/**
 * Represents a discrete entry in the playback queue.
 *
 * Distinct from [Song]: the same [Song] may appear multiple times in a queue
 * (e.g. repeated in a playlist), but each occurrence will have a unique [queueEntryId].
 * This gives Compose and Reorderable lists stable keys and prevents duplicate-removal bugs.
 */
data class QueueItem(
    val queueEntryId: String = UUID.randomUUID().toString(),
    val song: Song
)
