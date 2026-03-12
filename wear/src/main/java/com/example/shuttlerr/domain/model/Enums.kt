package com.example.shuttlerr.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameFormat { SINGLES, DOUBLES }

@Serializable
enum class CourtSide { LEFT, RIGHT }

@Serializable
enum class ServiceSide { EVEN, ODD }  // EVEN = right service box, ODD = left service box

@Serializable
enum class Player { A, B }

@Serializable
enum class DoublesSlot { ONE, TWO }  // player 1 or 2 within a team

fun CourtSide.opposite(): CourtSide = if (this == CourtSide.LEFT) CourtSide.RIGHT else CourtSide.LEFT
fun DoublesSlot.other(): DoublesSlot = if (this == DoublesSlot.ONE) DoublesSlot.TWO else DoublesSlot.ONE
