package com.example.shuttlerr.data.sync

import com.example.shuttlerr.data.repository.MatchRepository
import com.example.shuttlerr.domain.model.Match
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class WearSyncManager @Inject constructor(
    private val dataClient: DataClient,
    private val matchRepository: MatchRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun syncAllUnsynced() = withContext(Dispatchers.IO) {
        val unsynced = matchRepository.getUnsyncedMatches()
        for (match in unsynced) {
            try {
                val matchJson = json.encodeToString(Match.serializer(), match)
                val request = PutDataMapRequest.create("/match/${match.id}").apply {
                    dataMap.putString("match_json", matchJson)
                }.asPutDataRequest().setUrgent()

                suspendCancellableCoroutine<Unit> { cont ->
                    dataClient.putDataItem(request)
                        .addOnSuccessListener { cont.resume(Unit) }
                        .addOnFailureListener { e -> cont.resumeWithException(e) }
                }
                matchRepository.markSynced(match.id)
            } catch (e: Exception) {
                // Sync will be retried next time
            }
        }
    }
}
