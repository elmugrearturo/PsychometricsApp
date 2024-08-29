package com.example.mipersonalidad

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.example.mipersonalidad.room.AppDatabase
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [TestSelectionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class TestSelectionFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

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
        return inflater.inflate(R.layout.fragment_test_selection, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Clear navigation back stack
        findNavController().popBackStack(R.id.firstFragment, false)

        val informationButton = view.findViewById<Button>(R.id.btnBFIInformation)
        val resultsButton = view.findViewById<Button>(R.id.btnBFITestResults)
        val researchButton = view.findViewById<Button>(R.id.btnResearch)

        val db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java, "app-database"
        ).build()

        lifecycleScope.launch {
            val rowCount = db.bfiDao().getRowCount()
            if(rowCount == 0){
                // Research and results button are inactive
                // until having results
                resultsButton.isEnabled = false
                researchButton.isEnabled = false
            }else{
                if(!resultsButton.isEnabled){
                    resultsButton.isEnabled = true
                }

                if(!researchButton.isEnabled){
                    researchButton.isEnabled = true
                }
            }
        }

        informationButton.setOnClickListener{
            // Load BF Information Fragment
            findNavController().navigate(R.id.action_firstFragment_to_bfInformationFragment)
        }

        // Handle BFI button click
        view.findViewById<Button>(R.id.btnBFITest).setOnClickListener {
            // Load BFI Fragment
            findNavController().navigate(R.id.action_firstFragment_to_bfiFragment)
        }

        resultsButton.setOnClickListener {
            // Load BFI Results Fragment
            findNavController().navigate(R.id.action_firstFragment_to_bfiResultsFragment)
        }

        researchButton.setOnClickListener{
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            with(alertDialogBuilder)
            {
                setTitle("Proyectos de Investigación")
                setMessage("Por el momento no tenemos ningún proyecto activo.\n\nGracias por tu interés.")
                setNeutralButton("Aceptar", null)
                show()
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
         * @return A new instance of fragment TestSelectionFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            TestSelectionFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}