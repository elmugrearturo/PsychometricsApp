package com.arturocuriel.mipersonalidad.models

import com.arturocuriel.mipersonalidad.room.BFIScores
import com.arturocuriel.mipersonalidad.room.BFItems
import com.arturocuriel.mipersonalidad.room.Users

data class UserBFIPayload (
    val users : Users,
    val bigFiveItems : List<BFItems>,
    val bigFiveResults : BFIScores
)