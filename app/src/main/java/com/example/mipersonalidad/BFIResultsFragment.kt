package com.example.mipersonalidad

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
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

    private fun displayResults(result: BFIScores) {
        val normalizedScores = normalizeScores(result)
        val entries = mutableListOf<BarEntry>().apply {
            add(BarEntry(0f, normalizedScores["Openness"]!!.toFloat()))
            add(BarEntry(1f, normalizedScores["Conscientiousness"]!!.toFloat()))
            add(BarEntry(2f, normalizedScores["Extraversion"]!!.toFloat()))
            add(BarEntry(3f, normalizedScores["Agreeableness"]!!.toFloat()))
            add(BarEntry(4f, normalizedScores["Neuroticism"]!!.toFloat()))
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
        val explanation = buildExplanation(result)
        textExplanation.text = explanation
    }

    private fun buildExplanation(result: BFIScores): String {
        val explanationBuilder = StringBuilder()

        explanationBuilder.append("Tus resultados:\n")
        explanationBuilder.append("Apertura a la experiencia: ${result.openness}/5\n")
        explanationBuilder.append("Responsabilidad: ${result.conscientiousness}/5\n")
        explanationBuilder.append("Extraversión: ${result.extraversion}/5\n")
        explanationBuilder.append("Amabilidad: ${result.agreeableness}/5\n")
        explanationBuilder.append("Neuroticismo: ${result.neuroticism}/5\n\n")

        explanationBuilder.append("Apertura a la experiencia: Este rasgo refleja la curiosidad intelectual, la creatividad y la preferencia por la variedad y la novedad. Las personas con alta apertura son imaginativas, artísticas y están abiertas a nuevas ideas y experiencias. Por el contrario, quienes tienen baja apertura tienden a ser más convencionales, prácticas y prefieren rutinas familiares.\n")
        explanationBuilder.append("Responsabilidad: Este factor indica el grado de organización, diligencia y control que tiene una persona sobre sus impulsos. Las personas con alta responsabilidad son organizadas, meticulosas y tienen un fuerte sentido del deber. Aquellas con baja responsabilidad pueden ser más impulsivas, desorganizadas y menos enfocadas en alcanzar metas a largo plazo.\n")
        explanationBuilder.append("Extraversión: Este rasgo se refiere a la energía social, la sociabilidad y la tendencia a buscar la estimulación externa. Las personas extrovertidas son enérgicas, asertivas y disfrutan de la interacción social. Por otro lado, las personas con baja extraversión, o introvertidas, tienden a ser más reservadas, reflexivas y disfrutan de la soledad.\n")
        explanationBuilder.append("Amabilidad: Este factor mide la tendencia a ser compasivo, cooperativo y a mantener relaciones interpersonales positivas. Las personas con alta amabilidad son empáticas, confiables y buscan evitar conflictos. Las personas con baja amabilidad pueden ser más competitivas, críticas y enfocadas en sus propios intereses.\n")
        explanationBuilder.append("Neuroticismo: Este rasgo evalúa la estabilidad emocional y la tendencia a experimentar emociones negativas como ansiedad, depresión o irritabilidad. Las personas con alto neuroticismo son más propensas a sufrir estrés y tener fluctuaciones emocionales intensas. Las personas con bajo neuroticismo son más emocionalmente estables y tienden a manejar mejor las situaciones estresantes.\n")

        return explanationBuilder.toString()
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