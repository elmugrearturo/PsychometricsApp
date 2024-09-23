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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.arturocuriel.mipersonalidad.models.SacksPayload
import com.arturocuriel.mipersonalidad.models.ServerCommunication
import com.arturocuriel.mipersonalidad.models.UserBFIPayload
import com.arturocuriel.mipersonalidad.room.AppDatabase
import com.google.gson.Gson
import kotlinx.coroutines.launch

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
            // Navigate to the first fragment
            findNavController().navigate(R.id.action_researchProjectsFragment_to_firstFragment)
        }

        // Check if we need to send info
        resendUserDataIfNecessary()

        // Configure project list
        val cardView1 = view.findViewById<CardView>(R.id.card1)
        val cardView2 = view.findViewById<CardView>(R.id.card2)

        cardView1.setOnClickListener{
            val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            val sacksItemsReady = sharedPref.getBoolean("SACKS_ITEMS_READY", false)

            if (sacksItemsReady) {
                findNavController().navigate(R.id.action_researchProjectsFragment_to_sacksResultsFragment)
            } else {
                findNavController().navigate(R.id.action_researchProjectsFragment_to_sacksFragment)
            }

        }

        cardView2.setOnClickListener{
            val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
            val dassItemsReady = sharedPref.getBoolean("DASS_ITEMS_READY", false)

            if (dassItemsReady) {
                findNavController().navigate(R.id.action_researchProjectsFragment_to_dassResultsFragment)
            } else {
                findNavController().navigate(R.id.action_researchProjectsFragment_to_dassFragment)
            }
        }
    }

    private fun resendUserDataIfNecessary() {
        val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val usedDataSent = sharedPref.getBoolean("USER_DATA_SENT", false)

        if (!usedDataSent) {
            lifecycleScope.launch {
                // Prepare user and BFI data for sending to server
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java, "app-database"
                ).build()

                val userData = db.usersDao().getLastInsertedUser()
                val bfiScores = db.bfiDao().getLastInsertedScore()
                val bfiItems = db.bfiItemsDao().getItemResponses()

                val uBFIPayload = UserBFIPayload(
                    users = userData!!,
                    bigFiveItems = bfiItems,
                    bigFiveResults = bfiScores!!
                )

                // Gson object
                val gson = Gson()
                val uBFIPayloadJson = gson.toJson(uBFIPayload)

                // Send to server
                val comm = ServerCommunication(
                    getString(R.string.testServerDomain),
                    getString(R.string.testbfiEndpoint),
                    getString(R.string.testSha56hash),
                    uBFIPayloadJson
                )

                val success = comm.sendData(secure = false, callback = { success ->
                    with(sharedPref.edit()) {
                        putBoolean("USER_DATA_SENT", success)
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