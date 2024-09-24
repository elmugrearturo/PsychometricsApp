package com.arturocuriel.mipersonalidad.models

import com.arturocuriel.mipersonalidad.room.DASSItems
import com.arturocuriel.mipersonalidad.room.DASSScores

data class DASSPayload (
    val uuid : String,
    val dassItems : List<DASSItems>,
    val dassResults : DASSScores
)