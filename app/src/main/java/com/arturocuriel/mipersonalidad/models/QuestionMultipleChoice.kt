package com.arturocuriel.mipersonalidad.models

open class QuestionMultipleChoice(id: Int, text: String,
                                  val options: List<String>) : Question(id, text){
                                      var selection: Int? = null
}