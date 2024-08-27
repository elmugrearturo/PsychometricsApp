package com.example.mipersonalidad.models

class QuestionBFI(id: Int,
                  text: String,
                  options: List<String>,
                  val trait:String) : QuestionMultipleChoice(id, text, options)