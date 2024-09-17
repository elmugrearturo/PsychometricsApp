package com.arturocuriel.mipersonalidad

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.cardview.widget.CardView
import androidx.navigation.fragment.findNavController

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ResearchProjectsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ResearchProjectsFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_research_projects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



        // Override back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            //val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            //val navController = navHostFragment.navController

            // Navigate to the first fragment
            findNavController().navigate(R.id.action_researchProjectsFragment_to_firstFragment)
        }

        val cardView = view.findViewById<CardView>(R.id.card1)

        cardView.setOnClickListener{
            val alertDialogBuilder = AlertDialog.Builder(requireContext())
            with(alertDialogBuilder)
            {
                setTitle("Proyectos de Investigación")
                setMessage("Por el momento el proyecto está inactivo.\n\nGracias por tu interés.")
                setNeutralButton("Aceptar", null)
                show()
            }
        }
    }

    private fun resendIfNecessary() {
        val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val usedDataSent = sharedPref.getBoolean("USER_DATA_SENT", false)

        if (!usedDataSent) {

        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ResearchProjectsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ResearchProjectsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}