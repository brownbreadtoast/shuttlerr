package com.example.shuttlerr.data.repository

import com.example.shuttlerr.data.db.dao.PlayerDao
import com.example.shuttlerr.data.db.entities.PlayerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerRepositoryImpl @Inject constructor(
    private val playerDao: PlayerDao,
) : PlayerRepository {
    override fun getAllPlayers(): Flow<List<String>> =
        playerDao.getAllPlayers().map { it.map { e -> e.name } }

    override suspend fun addPlayer(name: String) {
        playerDao.insert(PlayerEntity(UUID.randomUUID().toString(), name))
    }
}
