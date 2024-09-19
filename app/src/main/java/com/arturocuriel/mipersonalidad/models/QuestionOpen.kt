package com.arturocuriel.mipersonalidad.models

class QuestionOpen(id: Int, text: String) : Question(id, text) {
    var response: String? = null // Selection
}