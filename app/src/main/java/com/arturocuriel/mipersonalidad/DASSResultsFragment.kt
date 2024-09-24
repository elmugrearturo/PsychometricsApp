package com.arturocuriel.mipersonalidad

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.arturocuriel.mipersonalidad.models.DASSPayload
import com.arturocuriel.mipersonalidad.models.SacksPayload
import com.arturocuriel.mipersonalidad.models.ServerCommunication
import com.arturocuriel.mipersonalidad.room.AppDatabase
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

    }

    private fun sendDASSItemsIfNecessary() {
        val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val uuid = sharedPref.getString("UUID", "")
        val sacksItemsSent = sharedPref.getBoolean("DASS_ITEMS_SENT", false)
        val sacksItemsReady = sharedPref.getBoolean("DASS_ITEMS_READY", false)

        if (!sacksItemsSent and sacksItemsReady) {
            lifecycleScope.launch {
                // Prepare user and BFI data for sending to server
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java, "app-database"
                ).build()

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
                    getString(R.string.testServerDomain),
                    getString(R.string.testDASSEndpoint),
                    getString(R.string.testSha56hash),
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