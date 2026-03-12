package com.example.shuttlerr.data.sync

import com.example.shuttlerr.data.repository.MobileMatchRepository
import com.example.shuttlerr.domain.model.Match
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class MobileSyncService : WearableListenerService() {

    @Inject
    lateinit var matchRepository: MobileMatchRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED &&
                event.dataItem.uri.path?.startsWith("/match/") == true
            ) {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val matchJson = dataMap.getString("match_json") ?: return@forEach
                scope.launch {
                    try {
                        val match = json.decodeFromString(Match.serializer(), matchJson)
                        matchRepository.upsertMatch(match)
                    } catch (e: Exception) {
                        // Malformed data — skip
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
