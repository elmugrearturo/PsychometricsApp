package com.arturocuriel.mipersonalidad.models

import com.arturocuriel.mipersonalidad.room.SacksItems

data class SacksPayload (
    val uuid : String,
    val sacksItems : List<SacksItems>,
)