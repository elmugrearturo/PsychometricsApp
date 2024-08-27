package com.example.mipersonalidad

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.example.mipersonalidad.room.AppDatabase
import com.example.mipersonalidad.room.BFIDao
import com.example.mipersonalidad.room.BFIScores
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BFIResultsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BFIResultsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var barChart : BarChart
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
        return inflater.inflate(R.layout.fragment_b_f_i_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Override back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            //val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            //val navController = navHostFragment.navController

            // Navigate to the first fragment
            findNavController().navigate(R.id.action_bfiResultsFragment_to_firstFragment)
        }

        barChart = view.findViewById(R.id.bfiBarChart)
        textExplanation = view.findViewById(R.id.textExplanation)

        // Initialize the Room database
        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "app-database"
        ).build()

        lifecycleScope.launch {
            val bfiScores = db.bfiDao().getLastInsertedScore()
            bfiScores?.let {
                displayResults(it)
            }
        }

    }

    private fun normalizeScores(inputScores : BFIScores) : Map<String, Int> {

        val outputScores = mutableMapOf<String, Int>(
            "Openness" to inputScores.openness,
            "Conscientiousness" to inputScores.conscientiousness,
            "Extraversion" to inputScores.extraversion,
            "Agreeableness" to inputScores.agreeableness,
            "Neuroticism" to inputScores.neuroticism
        )

        // Normalize to 0-100
        outputScores["Openness"] = (outputScores["Openness"]!! * 100) / 50
        outputScores["Conscientiousness"] = (outputScores["Conscientiousness"]!! * 100) / 45
        outputScores["Extraversion"] = (outputScores["Extraversion"]!! * 100) / 40
        outputScores["Agreeableness"] = (outputScores["Agreeableness"]!! * 100) / 45
        outputScores["Neuroticism"] = (outputScores["Neuroticism"]!! * 100) / 40

        return outputScores
    }

    private fun normalizeScoresMinMax(inputScores : BFIScores) : Map<String, Float> {

        val outputScores = mutableMapOf<String, Float>(
            "Openness" to inputScores.openness / 10f,
            "Conscientiousness" to inputScores.conscientiousness / 9f,
            "Extraversion" to inputScores.extraversion / 8f,
            "Agreeableness" to inputScores.agreeableness / 9f,
            "Neuroticism" to inputScores.neuroticism / 8f
        )

        // Normalize to 0-1
        // (value - min) / (max - min)
        val maxValue = 5
        val minValue = 1
        val denominator = maxValue - minValue

        outputScores["Openness"] = (outputScores["Openness"]!! * minValue * 100) / denominator
        outputScores["Conscientiousness"] = (outputScores["Conscientiousness"]!! * minValue * 100) / denominator
        outputScores["Extraversion"] = (outputScores["Extraversion"]!! * minValue * 100) / denominator
        outputScores["Agreeableness"] = (outputScores["Agreeableness"]!! * minValue * 100) / denominator
        outputScores["Neuroticism"] = (outputScores["Neuroticism"]!! * minValue * 100) / denominator

        return outputScores
    }

    private fun displayResults(results: BFIScores) {
        val normalizedScores = normalizeScoresMinMax(results)
        val entries = mutableListOf<BarEntry>().apply {
            add(BarEntry(0f, normalizedScores["Openness"]!!))
            add(BarEntry(1f, normalizedScores["Conscientiousness"]!!))
            add(BarEntry(2f, normalizedScores["Extraversion"]!!))
            add(BarEntry(3f, normalizedScores["Agreeableness"]!!))
            add(BarEntry(4f, normalizedScores["Neuroticism"]!!))
        }

        val barDataSet = BarDataSet(entries, "Resultados de los Cinco Factores")
        val barData = BarData(barDataSet)
        barChart.data = barData

        // Customize the chart
        val labels = listOf("Apertura", "Responsabilidad", "Extraversión", "Amabilidad", "Neuroticismo")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f  // Assuming the max score is 5 for BFI
        barChart.axisRight.isEnabled = false
        barChart.invalidate()  // Refresh the chart

        // Create a text explanation
        val explanation = buildExplanation(results, normalizedScores)
        textExplanation.text = explanation
    }

    private fun buildExplanation(results: BFIScores, normalizedScores:Map<String, Float>): String {
        val explanationBuilder = StringBuilder()

        explanationBuilder.append("Tus resultados:\n")
        explanationBuilder.append("Apertura a la experiencia: ${results.openness/10f}/5.00\n")
        explanationBuilder.append("Responsabilidad: ${results.conscientiousness/9f}/5.00\n")
        explanationBuilder.append("Extraversión: ${results.extraversion/8f}/5.00\n")
        explanationBuilder.append("Amabilidad: ${results.agreeableness/9f}/5.00\n")
        explanationBuilder.append("Neuroticismo: ${results.neuroticism/8f}/5.00\n\n")

        val openness = normalizedScores["Openness"]!!.toInt()
        val conscientiousness = normalizedScores["Conscientiousness"]!!.toInt()
        val extraversion = normalizedScores["Extraversion"]!!.toInt()
        val agreeableness = normalizedScores["Agreeableness"]!!.toInt()
        val neuroticism = normalizedScores["Neuroticism"]!!.toInt()

        explanationBuilder.append(getOpennessFeedback(openness))
        explanationBuilder.append(getConscientiousnessFeedback(conscientiousness))
        explanationBuilder.append(getExtraversionFeedback(extraversion))
        explanationBuilder.append(getAgreeablenessFeedback(agreeableness))
        explanationBuilder.append(getNeuroticismFeedback(neuroticism))

        return explanationBuilder.toString()
    }

    private fun getOpennessFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.openness_low)
            in 31..60 -> getString(R.string.openness_medium)
            in 61..100 -> getString(R.string.openness_high)
            else -> getString(R.string.score_out_of_range)
        }
    }

    private fun getConscientiousnessFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.conscientiousness_low)
            in 31..60 -> getString(R.string.conscientiousness_medium)
            in 61..100 -> getString(R.string.conscientiousness_high)
            else -> getString(R.string.score_out_of_range)
        }
    }

    private fun getExtraversionFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.extraversion_low)
            in 31..60 -> getString(R.string.extraversion_medium)
            in 61..100 -> getString(R.string.extraversion_high)
            else -> getString(R.string.score_out_of_range)
        }
    }

    private fun getAgreeablenessFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.agreeableness_low)
            in 31..60 -> getString(R.string.agreeableness_medium)
            in 61..100 -> getString(R.string.agreeableness_high)
            else -> getString(R.string.score_out_of_range)
        }
    }

    private fun getNeuroticismFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.neuroticism_low)
            in 31..60 -> getString(R.string.neuroticism_medium)
            in 61..100 -> getString(R.string.neuroticism_high)
            else -> getString(R.string.score_out_of_range)
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BFIResultsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BFIResultsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}