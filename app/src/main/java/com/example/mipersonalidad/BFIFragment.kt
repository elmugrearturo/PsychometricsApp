package com.example.mipersonalidad

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mipersonalidad.models.QuestionBFI
import com.example.mipersonalidad.room.AppDatabase
import com.example.mipersonalidad.room.BFIScores
import kotlinx.coroutines.launch

import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException


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

    private lateinit var questionList: List<QuestionBFI>
    private lateinit var testInstructions: String
    private var currentQuestionIndex: Int = 0

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

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the questions from the JSON file
        questionList = loadQuestionsFromAssets()

        // Display instructions
        view.findViewById<TextView>(R.id.instructionsText)?.text = testInstructions

        // Display the first question or handle the questions as needed
        displayQuestion(currentQuestionIndex)

        // LISTENERS
        val previousButton = view.findViewById<Button>(R.id.previousQuestionButton)
        val nextButton = view.findViewById<Button>(R.id.nextQuestionButton)
        val radioGroup = view.findViewById<RadioGroup>(R.id.answerOptions)

        // Enable next on selection
        radioGroup.setOnCheckedChangeListener { _, _ ->
            // Enable the next button when an option is selected
            nextButton.isEnabled = true
        }

        // Handle next button click
        nextButton.setOnClickListener {

            // Save selection
            val selection = radioGroup.checkedRadioButtonId
            questionList[currentQuestionIndex].selection = selection

            if (currentQuestionIndex < questionList.size - 1) {
                currentQuestionIndex++
                displayQuestion(currentQuestionIndex)
            } else {
                // Submit the test and show results
                val scores = calculateScores()
                val normalized_scores = normalizeScores(scores)

                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java, "app-database"
                ).build()

                lifecycleScope.launch{
                    val bfiScores = BFIScores(
                        extraversion = scores["Extraversion"]!!,
                        agreeableness = scores["Agreeableness"]!!,
                        openness = scores["Openness"]!!,
                        conscientiousness = scores["Conscientiousness"]!!,
                        neuroticism = scores["Neuroticism"]!!,
                        timestamp = System.currentTimeMillis()
                        )
                    db.bfiDao().insertScore(bfiScores)
                }

                // Load ResultsFragment
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, TestSelectionFragment())
                    .commit()
            }
        }

        // Handle previous button click
        previousButton.setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                displayQuestion(currentQuestionIndex)
            }
        }


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

    private fun loadQuestionsFromAssets(): List<QuestionBFI> {
        // Parse the JSON file into a list of QuestionMultipleChoice objects
        val questionList = mutableListOf<QuestionBFI>()

        // Gets main object
        val jsonMainObject = JSONObject(loadJSONFromAssets())

        // Get Instructions
        testInstructions = jsonMainObject.getJSONObject("instructions").getString("es")

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
            val trait = jsonQuestionObject.getString("trait")

            // Create a Question object and add it to the list
            val question = QuestionBFI(id, text, optionsList, trait)
            questionList.add(question)
        }

        return questionList
    }

    private fun displayQuestion(index: Int) {
        val previousButton = view?.findViewById<Button>(R.id.previousQuestionButton)
        val nextButton = view?.findViewById<Button>(R.id.nextQuestionButton)
        val question = questionList[index]

        if (index == 0){
            previousButton?.isEnabled = false
        } else {
            if(!previousButton?.isEnabled!!) {
                previousButton.isEnabled = true
            }
        }

        if (index < questionList.size - 1) {
            if (nextButton?.text != "Siguiente"){
                nextButton?.text = "Siguiente"
                }
        } else {
            nextButton?.text = "Ver resultado"
        }

        if (question.selection == null){
            nextButton?.isEnabled = false
        }

        view?.findViewById<TextView>(R.id.questionText)?.text = question.text

        val radioGroup = view?.findViewById<RadioGroup>(R.id.answerOptions)
        radioGroup?.removeAllViews()

        question.options.forEachIndexed { optionIndex, option ->
            val radioButton = RadioButton(context)
            radioButton.text = option
            radioButton.id = optionIndex
            radioGroup?.addView(radioButton)
        }
        if (question.selection != null) {
            val selectedRadioButton = radioGroup?.getChildAt(question.selection!!) as RadioButton
            selectedRadioButton.isChecked = true
        }
    }

    private fun calculateScores() : Map<String, Int> {

        val scores = mutableMapOf<String, Int>(
            "Openness" to 0,
            "Conscientiousness" to 0,
            "Extraversion" to 0,
            "Agreeableness" to 0,
            "Neuroticism" to 0
        )

        val toReverse = arrayOf(2, 6, 8, 9, 12, 13, 16, 18, 19, 22, 25, 27, 33, 35, 42, 44)

        fun reverseScore(score:Int) : Int{
            return 6 - score
        }

        for (question in questionList){

            val score = if(question.id in toReverse){
                reverseScore(question.selection!! + 1)
            } else {
                question.selection!! + 1
            }

            //Log.d("SCORE" + ": ", question.id.toString() + ": " + score.toString())

            scores[question.trait] = scores[question.trait]!! + score
        }

        return scores
    }

    private fun normalizeScores(input_scores : Map<String, Int>) : Map<String, Int> {

        val output_scores = mutableMapOf<String, Int>(
            "Openness" to input_scores["Openness"]!!,
            "Conscientiousness" to input_scores["Conscientiousness"]!!,
            "Extraversion" to input_scores["Extraversion"]!!,
            "Agreeableness" to input_scores["Agreeableness"]!!,
            "Neuroticism" to input_scores["Neuroticism"]!!
        )

        // Normalize to 0-100
        output_scores["Openness"] = (output_scores["Openness"]!! * 100) / 50
        output_scores["Conscientiousness"] = (output_scores["Conscientiousness"]!! * 100) / 45
        output_scores["Extraversion"] = (output_scores["Extraversion"]!! * 100) / 40
        output_scores["Agreeableness"] = (output_scores["Agreeableness"]!! * 100) / 45
        output_scores["Neuroticism"] = (output_scores["Neuroticism"]!! * 100) / 40

        return output_scores
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