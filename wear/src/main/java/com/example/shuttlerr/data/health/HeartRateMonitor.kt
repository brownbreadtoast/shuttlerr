package com.example.shuttlerr.data.health

import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HeartRateMonitor @Inject constructor(
    private val healthServicesClient: HealthServicesClient,
) {
    val heartRateFlow: Flow<Int?> = callbackFlow {
        val measureClient = healthServicesClient.measureClient

        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(
                dataType: DeltaDataType<*, *>,
                availability: Availability,
            ) {
                if (availability == DataTypeAvailability.UNAVAILABLE) {
                    trySend(null)
                }
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRateSamples = data.getData(DataType.HEART_RATE_BPM)
                heartRateSamples.lastOrNull()?.let { trySend(it.value.toInt()) }
            }
        }

        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        awaitClose {
            // Best-effort unregister — result is not awaited
            measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback)
        }
    }
}
