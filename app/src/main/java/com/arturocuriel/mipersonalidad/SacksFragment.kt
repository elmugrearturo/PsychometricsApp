package com.arturocuriel.mipersonalidad

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.arturocuriel.mipersonalidad.models.QuestionBFI
import com.arturocuriel.mipersonalidad.models.QuestionOpen
import com.arturocuriel.mipersonalidad.models.SacksPayload
import com.arturocuriel.mipersonalidad.models.ServerCommunication
import com.arturocuriel.mipersonalidad.room.AppDatabase
import com.arturocuriel.mipersonalidad.room.BFIScores
import com.arturocuriel.mipersonalidad.room.BFItems
import com.arturocuriel.mipersonalidad.room.SacksItems
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
 * Use the [SacksFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SacksFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var questionList: List<QuestionOpen>
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
        return inflater.inflate(R.layout.fragment_sacks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        questionList = loadQuestionsFromAssets()

        // Display instructions
        view.findViewById<TextView>(R.id.sacksInstructionsText)?.text = testInstructions

        // Display the first question or handle the questions as needed
        displayQuestion(currentQuestionIndex)

        // LISTENERS
        val previousButton = view.findViewById<Button>(R.id.sacksPreviousQuestionButton)
        val nextButton = view.findViewById<Button>(R.id.sacksNextQuestionButton)
        val answerInput = view.findViewById<EditText>(R.id.sacksAnswer)
        val progressIndicator = view.findViewById<LinearProgressIndicator>(R.id.sacksProgressIndicator)

        // Enable next on selection
        answerInput.addTextChangedListener(object: TextWatcher {
            // Enable the next button when text is inserted

            override fun afterTextChanged(s: Editable?) {
                // This is called after the text is changed
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This is called before the text is changed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                nextButton.isEnabled = s.toString().isNotEmpty()
            }
        })

        // Handle next button click
        nextButton.setOnClickListener {

            // Save text
            val userResponse = answerInput.text.toString()
            questionList[currentQuestionIndex].response = userResponse

            if (currentQuestionIndex < questionList.size - 1) {
                currentQuestionIndex++
                progressIndicator.progress++
                displayQuestion(currentQuestionIndex)
            } else {
                val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)

                // Submit the test and show results
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java, "app-database"
                ).build()

                lifecycleScope.launch{
                    // Clean previous scores
                    db.sacksDao().emptyItemResponses()

                    // Insert new individual scores
                    for (question in questionList){
                        val responseItem = SacksItems(
                            itemNumber = question.id,
                            response = question.response!!,
                            timestamp = System.currentTimeMillis()
                        )
                        db.sacksDao().insertItemResponse(responseItem)
                    }

                    val uuid = sharedPref.getString("UUID", "")
                    val sacksItems = db.sacksDao().getItemResponses()

                    val sacksPayload = SacksPayload(
                        uuid = uuid!!,
                        sacksItems = sacksItems,
                    )

                    // Gson object
                    val gson = Gson()
                    val sacksPayloadJson = gson.toJson(sacksPayload)

                    // Send to server
                    val comm = ServerCommunication(
                        getString(R.string.serverDomain),
                        getString(R.string.sacksEndpoint),
                        getString(R.string.sha56hash),
                        sacksPayloadJson
                    )

                    comm.sendData(secure = false, callback = { success ->
                        with(sharedPref.edit()) {
                            putBoolean("SACKS_ITEMS_SENT", success)
                            apply()
                        }
                    })
                }

                // Signal that the view has finished it's job
                with(sharedPref.edit()){
                    putBoolean("SACKS_ITEMS_READY", true)
                    apply()
                }

                // Load SacksResultsFragment where info will be sent
                findNavController().navigate(R.id.action_sacksFragment_to_sacksResultsFragment)
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
            val inputStream = requireContext().assets.open("sacks_questions.json")
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

    private fun loadQuestionsFromAssets(): List<QuestionOpen> {
        // Parse the JSON file into a list of QuestionMultipleChoice objects
        val questionList = mutableListOf<QuestionOpen>()

        // Gets main object
        val jsonMainObject = JSONObject(loadJSONFromAssets()!!)

        // Get Instructions
        testInstructions = jsonMainObject.getJSONObject("instructions").getString("es")

        // Gets questions
        val jsonQuestionArray = jsonMainObject.getJSONArray("questions")

        // Loop through each object in the array
        for (i in 0 until jsonQuestionArray.length()) {
            val jsonQuestionObject = jsonQuestionArray.getJSONObject(i)

            // Extract the values from the JSON object
            val id = jsonQuestionObject.getInt("id")
            // TODO: Check how it would be done for multiple languages
            val text = jsonQuestionObject.getJSONObject("text").getString("es")

            // Create a Question object and add it to the list
            val question = QuestionOpen(id, text)
            questionList.add(question)
        }

        return questionList
    }

    private fun displayQuestion(index: Int) {
        val previousButton = view?.findViewById<Button>(R.id.sacksPreviousQuestionButton)
        val nextButton = view?.findViewById<Button>(R.id.sacksNextQuestionButton)
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
            nextButton?.text = "Enviar"
        }

        var currentResponse = ""
        if (question.response == null){
            nextButton?.isEnabled = false
        } else {
            nextButton?.isEnabled = true
            currentResponse = question.response!!
        }

        view?.findViewById<TextView>(R.id.sacksQuestionText)?.text = question.text
        view?.findViewById<TextView>(R.id.sacksAnswer)?.text = currentResponse
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SacksFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SacksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}