package com.arturocuriel.mipersonalidad

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.arturocuriel.mipersonalidad.models.DASSPayload
import com.arturocuriel.mipersonalidad.models.SacksPayload
import com.arturocuriel.mipersonalidad.models.ServerCommunication
import com.arturocuriel.mipersonalidad.room.AppDatabase
import com.arturocuriel.mipersonalidad.room.BFIScores
import com.arturocuriel.mipersonalidad.room.DASSScores
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DASSResultsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DASSResultsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var numericReport : TextView
    private lateinit var textExplanation : TextView

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
        return inflater.inflate(R.layout.fragment_dass_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if we need to save info
        sendDASSItemsIfNecessary()

        // Override back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Navigate to the research projects fragment
            findNavController().navigate(R.id.action_dassResultsFragment_to_researchProjectsFragment)
        }

        numericReport = view.findViewById(R.id.dassResultsNumbers)
        textExplanation = view.findViewById(R.id.dassResultsReport)

        // Initialize the Room database
        val db = AppDatabase.getDatabase(requireContext())

        lifecycleScope.launch {
            val dassScores = db.dassDao().getLastInsertedScore()
            dassScores?.let {
                displayResults(it)
            }
        }
    }

    private fun displayResults(results: DASSScores) {

        val numericReportBuilder = StringBuilder()

        numericReportBuilder.append(
            "Depresión: %d/21\n".format(results.depression))
        numericReportBuilder.append(
            "Ansiedad: %d/21\n".format(results.anxiety))
        numericReportBuilder.append(
            "Estrés: %d/21\n".format(results.stress))

        val explanationBuilder = StringBuilder()

        explanationBuilder.append(getDepressionFeedback(results.depression) + "\n\n")
        explanationBuilder.append(getAnxietyFeedback(results.anxiety) + "\n\n")
        explanationBuilder.append(getStressFeedback(results.stress) + "\n\n")

        // Create a text explanation
        numericReport.text = numericReportBuilder.toString()
        textExplanation.text = explanationBuilder.toString()
    }

    private fun getDepressionFeedback(score: Int): String {
        return when (score) {
            in 0..4 -> getString(R.string.depression_none)
            in 5..6 -> getString(R.string.depression_low)
            in 7..10 -> getString(R.string.depression_medium)
            in 11..13 -> getString(R.string.depression_high)
            else -> getString(R.string.depression_extreme)
        }
    }

    private fun getAnxietyFeedback(score: Int): String {
        return when (score) {
            in 0..3 -> getString(R.string.anxiety_none)
            in 4..4 -> getString(R.string.anxiety_low)
            in 5..7 -> getString(R.string.anxiety_medium)
            in 8..9 -> getString(R.string.anxiety_high)
            else -> getString(R.string.anxiety_extreme)
        }
    }

    private fun getStressFeedback(score: Int): String {
        return when (score) {
            in 0..7 -> getString(R.string.stress_none)
            in 8..9 -> getString(R.string.stress_low)
            in 10..12 -> getString(R.string.stress_medium)
            in 13..16 -> getString(R.string.stress_high)
            else -> getString(R.string.stress_extreme)
        }
    }

    private fun sendDASSItemsIfNecessary() {
        val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val uuid = sharedPref.getString("UUID", "")
        val dassItemsSent = sharedPref.getBoolean("DASS_ITEMS_SENT", false)
        val dassItemsReady = sharedPref.getBoolean("DASS_ITEMS_READY", false)

        if (!dassItemsSent and dassItemsReady) {
            lifecycleScope.launch {
                // Prepare DASS data for sending to server
                val db = AppDatabase.getDatabase(requireContext())

                val dassItems = db.dassItemsDao().getItemResponses()
                val dassScores = db.dassDao().getLastInsertedScore()

                val dassPayload = DASSPayload(
                    uuid = uuid!!,
                    dassItems = dassItems,
                    dassResults = dassScores!!
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
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment DASSResultsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            DASSResultsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}