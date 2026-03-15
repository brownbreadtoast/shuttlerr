package com.example.shuttlerr.data.repository

import kotlinx.coroutines.flow.Flow

interface PlayerRepository {
    fun getAllPlayers(): Flow<List<String>>
    suspend fun addPlayer(name: String)
}
