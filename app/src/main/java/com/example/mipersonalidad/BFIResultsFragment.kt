package com.example.mipersonalidad

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.example.mipersonalidad.room.AppDatabase
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
        numericReport = view.findViewById(R.id.resultsNumbers)
        textExplanation = view.findViewById(R.id.resultsReport)

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

        outputScores["Openness"] = (outputScores["Openness"]!! - minValue) / denominator
        outputScores["Conscientiousness"] = (outputScores["Conscientiousness"]!! - minValue) / denominator
        outputScores["Extraversion"] = (outputScores["Extraversion"]!! - minValue) / denominator
        outputScores["Agreeableness"] = (outputScores["Agreeableness"]!! - minValue) / denominator
        outputScores["Neuroticism"] = (outputScores["Neuroticism"]!! - minValue) / denominator

        outputScores["Openness"] = outputScores["Openness"]!! * 100
        outputScores["Conscientiousness"] = outputScores["Conscientiousness"]!! * 100
        outputScores["Extraversion"] = outputScores["Extraversion"]!! * 100
        outputScores["Agreeableness"] = outputScores["Agreeableness"]!! * 100
        outputScores["Neuroticism"] = outputScores["Neuroticism"]!! * 100

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

        val colors = listOf(
            requireContext().getColor(R.color.openness),
            requireContext().getColor(R.color.conscientiousness),
            requireContext().getColor(R.color.extraversion),
            requireContext().getColor(R.color.agreeableness),
            requireContext().getColor(R.color.neuroticism)
        )

        val barDataSet = BarDataSet(entries, "Resultados de los Cinco Factores de la Personalidad")
        barDataSet.colors = colors
        barDataSet.isHighlightEnabled = false

        val barData = BarData(barDataSet)
        barChart.data = barData

        barChart.description.isEnabled = false
        barChart.setTouchEnabled(false) // Disables all touch interactions
        barChart.isDragEnabled = false  // Disables dragging
        barChart.isScaleXEnabled = false
        barChart.isScaleYEnabled = false

        // Customize the chart
        val labels = listOf("Ap", "Re", "Ex", "Am", "Ne")
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.granularity = 1f
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f  // Assuming the max score is 5 for BFI
        barChart.axisRight.isEnabled = false
        barChart.invalidate()  // Refresh the chart

        // Create a text explanation
        numericReport.text = buildNumericReport(results)
        textExplanation.text = buildExplanation(normalizedScores)
    }

    private fun buildNumericReport(results: BFIScores): String {
        val numericReportBuilder = StringBuilder()

        numericReportBuilder.append("Apertura a la experiencia (Ap): ${results.openness/10f}/5.00\n")
        numericReportBuilder.append("Responsabilidad (Re): ${results.conscientiousness/9f}/5.00\n")
        numericReportBuilder.append("Extraversión (Ex): ${results.extraversion/8f}/5.00\n")
        numericReportBuilder.append("Amabilidad (Am): ${results.agreeableness/9f}/5.00\n")
        numericReportBuilder.append("Neuroticismo (Ne): ${results.neuroticism/8f}/5.00\n\n")

        return numericReportBuilder.toString()
    }

    private fun buildExplanation(normalizedScores:Map<String, Float>): String {
        val explanationBuilder = StringBuilder()

        val openness = normalizedScores["Openness"]!!.toInt()
        val conscientiousness = normalizedScores["Conscientiousness"]!!.toInt()
        val extraversion = normalizedScores["Extraversion"]!!.toInt()
        val agreeableness = normalizedScores["Agreeableness"]!!.toInt()
        val neuroticism = normalizedScores["Neuroticism"]!!.toInt()

        explanationBuilder.append(getOpennessFeedback(openness) + "\n\n")
        explanationBuilder.append(getConscientiousnessFeedback(conscientiousness) + "\n\n")
        explanationBuilder.append(getExtraversionFeedback(extraversion) + "\n\n")
        explanationBuilder.append(getAgreeablenessFeedback(agreeableness) + "\n\n")
        explanationBuilder.append(getNeuroticismFeedback(neuroticism))

        return explanationBuilder.toString()
    }

    private fun getOpennessFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.openness_low)
            in 31..60 -> getString(R.string.openness_medium)
            in 61..100 -> getString(R.string.openness_high)
            else -> getString(R.string.score_out_of_range) + " (O)"
        }
    }

    private fun getConscientiousnessFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.conscientiousness_low)
            in 31..60 -> getString(R.string.conscientiousness_medium)
            in 61..100 -> getString(R.string.conscientiousness_high)
            else -> getString(R.string.score_out_of_range) + " (C)"
        }
    }

    private fun getExtraversionFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.extraversion_low)
            in 31..60 -> getString(R.string.extraversion_medium)
            in 61..100 -> getString(R.string.extraversion_high)
            else -> getString(R.string.score_out_of_range) + " (E)"
        }
    }

    private fun getAgreeablenessFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.agreeableness_low)
            in 31..60 -> getString(R.string.agreeableness_medium)
            in 61..100 -> getString(R.string.agreeableness_high)
            else -> getString(R.string.score_out_of_range) + " (A)"
        }
    }

    private fun getNeuroticismFeedback(score: Int): String {
        return when (score) {
            in 0..30 -> getString(R.string.neuroticism_low)
            in 31..60 -> getString(R.string.neuroticism_medium)
            in 61..100 -> getString(R.string.neuroticism_high)
            else -> getString(R.string.score_out_of_range) + " (N)"
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