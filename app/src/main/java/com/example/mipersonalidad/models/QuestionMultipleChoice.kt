package com.example.mipersonalidad.models

class QuestionMultipleChoice(id: Int, text: String,
                             val options: List<String>) : Question(id, text){
                                 var selection: Int? = null
}