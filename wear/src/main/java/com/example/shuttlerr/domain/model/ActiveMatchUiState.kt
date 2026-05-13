package com.example.shuttlerr.domain.model

data class ActiveMatchUiState(
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val gameWinner: Player? = null,
    val matchWinner: Player? = null,
)
