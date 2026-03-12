package com.example.shuttlerr.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameFormat { SINGLES, DOUBLES }

@Serializable
enum class CourtSide { LEFT, RIGHT }

@Serializable
enum class ServiceSide { EVEN, ODD }

@Serializable
enum class Player { A, B }
