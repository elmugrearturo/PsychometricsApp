package com.example.mipersonalidad

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.example.mipersonalidad.models.QuestionMultipleChoice

// import com.google.gson.Gson
// import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.io.InputStreamReader



// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BFIFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BFIFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var questionList: List<QuestionMultipleChoice>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_b_f_i, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the questions from the JSON file
        questionList = loadQuestionsFromAssets()

        // Display the first question or handle the questions as needed
        displayQuestion(0)
    }

    private fun loadJSONFromAssets(): String? {
        val json: String?
        try {
            val inputStream = requireContext().assets.open("bfi_questions.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    private fun loadQuestionsFromAssets(): List<QuestionMultipleChoice> {
        // Parse the JSON file into a list of QuestionMultipleChoice objects
        val questionList = mutableListOf<QuestionMultipleChoice>()

        // Gets main object
        val jsonMainObject = JSONObject(loadJSONFromAssets())

        // Gets answers
        val jsonAnswerArray = jsonMainObject.getJSONArray("answers")
        val optionsList = mutableListOf<String>()
        for (j in 0 until jsonAnswerArray.length()) {
            val jsonAnswerObject = jsonAnswerArray.getJSONObject(j)
            optionsList.add(jsonAnswerObject.getJSONObject("text").getString("es"))
        }

        // Gets questions
        val jsonQuestionArray = jsonMainObject.getJSONArray("questions")
        // Loop through each object in the array

        for (i in 0 until jsonQuestionArray.length()) {
            val jsonQuestionObject = jsonQuestionArray.getJSONObject(i)

            // Extract the values from the JSON object
            val id = jsonQuestionObject.getInt("id")
            // TODO: Check how it would be done for multiple languages
            val text = jsonQuestionObject.getJSONObject("text").getString("es")
            // val trait = jsonQuestionObject.getString("trait")

            // Create a Question object and add it to the list
            val question = QuestionMultipleChoice(id, text, optionsList)
            questionList.add(question)
        }

        return questionList
    }

    private fun displayQuestion(index: Int) {
        val question = questionList[index]
        view?.findViewById<TextView>(R.id.questionText)?.text = question.text

        val radioGroup = view?.findViewById<RadioGroup>(R.id.answerOptions)
        radioGroup?.removeAllViews()

        question.options.forEach { option ->
            val radioButton = RadioButton(context)
            radioButton.text = option
            radioGroup?.addView(radioButton)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BFIFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BFIFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}