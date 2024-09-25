package com.arturocuriel.mipersonalidad

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.arturocuriel.mipersonalidad.models.DASSPayload
import com.arturocuriel.mipersonalidad.models.QuestionBFI
import com.arturocuriel.mipersonalidad.models.ServerCommunication
import com.arturocuriel.mipersonalidad.room.AppDatabase
import com.arturocuriel.mipersonalidad.room.BFIScores
import com.arturocuriel.mipersonalidad.room.BFItems
import com.arturocuriel.mipersonalidad.room.DASSItems
import com.arturocuriel.mipersonalidad.room.DASSScores
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DASSFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DASSFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_dass, container, false)
    }

    @SuppressLint("CutPasteId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load the questions from the JSON file
        questionList = loadQuestionsFromAssets()

        // Display instructions
        view.findViewById<TextView>(R.id.dassInstructionsText)?.text = testInstructions

        // Display the first question or handle the questions as needed
        displayQuestion(currentQuestionIndex)

        // LISTENERS
        val previousButton = view.findViewById<Button>(R.id.dassPreviousQuestionButton)
        val nextButton = view.findViewById<Button>(R.id.dassNextQuestionButton)
        val radioGroup = view.findViewById<RadioGroup>(R.id.dassAnswerOptions)
        val progressIndicator = view.findViewById<LinearProgressIndicator>(R.id.dassProgressIndicator)

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
                progressIndicator.progress++
                displayQuestion(currentQuestionIndex)
            } else {
                // Submit the test and show results
                val scores = calculateScores()
                val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)

                val db = AppDatabase.getDatabase(requireContext())

                lifecycleScope.launch{
                    val dassScores = DASSScores(
                        depression = scores["Depression"]!!,
                        anxiety = scores["Anxiety"]!!,
                        stress = scores["Stress"]!!,
                        timestamp = System.currentTimeMillis()
                    )
                    // Insert global scores
                    db.dassDao().insertScore(dassScores)

                    // Clean previous individual scores
                    db.dassItemsDao().emptyItemResponses()

                    // Insert new individual scores
                    for (question in questionList){
                        val responseItem = DASSItems(
                            itemNumber = question.id,
                            response = question.selection!! + 1,
                            timestamp = System.currentTimeMillis()
                        )
                        db.dassItemsDao().insertItemResponse(responseItem)
                    }

                    val uuid = sharedPref.getString("UUID", "")
                    val itemResponses = db.dassItemsDao().getItemResponses()

                    // Send
                    val dassPayload = DASSPayload(
                        uuid = uuid!!,
                        dassItems = itemResponses,
                        dassResults = dassScores
                    )

                    // Gson object
                    val gson = Gson()
                    val dassPayloadJson = gson.toJson(dassPayload)

                    // Send to server
                    val comm = ServerCommunication(
                        getString(R.string.serverDomain),
                        getString(R.string.dassEndpoint),
                        getString(R.string.sha56hash),
                        dassPayloadJson
                    )

                    comm.sendData(secure = false, callback = { success ->
                        with(sharedPref.edit()) {
                            putBoolean("DASS_ITEMS_SENT", success)
                            apply()
                        }
                    })
                }

                // Signal that the view has finished it's job
                with(sharedPref.edit()){
                    putBoolean("DASS_ITEMS_READY", true)
                    apply()
                }

                // Load ResultsFragment
                findNavController().navigate(R.id.action_dassFragment_to_dassResultsFragment)
            }
        }

        // Handle previous button click
        previousButton.setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                progressIndicator.progress--
                displayQuestion(currentQuestionIndex)
            }
        }

    }

    private fun loadJSONFromAssets(): String? {
        val json: String?
        try {
            val inputStream = requireContext().assets.open("dass_questions.json")
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
        val jsonMainObject = JSONObject(loadJSONFromAssets()!!)

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
        val previousButton = view?.findViewById<Button>(R.id.dassPreviousQuestionButton)
        val nextButton = view?.findViewById<Button>(R.id.dassNextQuestionButton)
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

        view?.findViewById<TextView>(R.id.dassQuestionText)?.text = question.text

        val radioGroup = view?.findViewById<RadioGroup>(R.id.dassAnswerOptions)
        radioGroup?.removeAllViews()

        question.options.forEachIndexed { optionIndex, option ->
            val radioButton = RadioButton(requireContext())
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
            "Depression" to 0,
            "Anxiety" to 0,
            "Stress" to 0
        )

        for (question in questionList){
            scores[question.trait] = scores[question.trait]!! + question.selection!!
        }

        return scores
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DASSFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DASSFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}