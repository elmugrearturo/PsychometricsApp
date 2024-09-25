package com.arturocuriel.mipersonalidad

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.arturocuriel.mipersonalidad.models.ServerCommunication
import com.arturocuriel.mipersonalidad.models.UserBFIPayload
import com.arturocuriel.mipersonalidad.room.AppDatabase
import com.arturocuriel.mipersonalidad.room.Users
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [PersonalDataFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PersonalDataFragment : Fragment() {

    private var param1 : String? = null
    private var param2 : String? = null

    private val displayToBackendNationalityChoices = mapOf(
        "México" to "mexican",
        "Colombia" to "colombian",
        "Argentina" to "argentinian",
        "España" to "spanish",
        "Estados Unidos" to "american",
        "Peru" to "peruvian",
        "Chile" to "chilean"
    )

    private val displayToBackendGenderChoices = mapOf(
        "Femenino" to "female",
        "Masculino" to "male",
        "Transgenero femenino" to "transfemale",
        "Transgenero masculino" to "transmale",
        "No binario" to "nonbinary",
        "Sin género" to "agender",
        "Prefiero no especificar" to "other"
    )

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
        return inflater.inflate(R.layout.fragment_personal_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Override back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            //val navHostFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            //val navController = navHostFragment.navController

            // Navigate to the first fragment
            findNavController().navigate(R.id.action_personalDataFragment_to_firstFragment)
        }

        val registerButton : Button = view.findViewById(R.id.registerData)
        val cancelButton : Button = view.findViewById(R.id.cancelDataRegistration)
        val genderSpinner : Spinner = view.findViewById(R.id.genderSpinner)
        val countrySpinner : Spinner = view.findViewById(R.id.countrySpinner)

        val agePicker : NumberPicker = view.findViewById(R.id.agePicker)

        agePicker.minValue = 18
        agePicker.maxValue = 100
        agePicker.value = 18

        registerButton.setOnClickListener(){
            registerData(genderSpinner, countrySpinner, agePicker)
        }

        cancelButton.setOnClickListener(){
            findNavController().navigate(R.id.action_personalDataFragment_to_firstFragment)
        }

    }

    private fun registerData(genderSpinner:Spinner, countrySpinner:Spinner, agePicker: NumberPicker){
        // Get UUID
        val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val userUUID = sharedPref.getString("UUID", "")

        if (userUUID == ""){
            // Something was wrong, force to re-accept licence.
            with(sharedPref.edit()) {
                putBoolean("LICENSE_ACCEPTED", false)
                apply()
            }

            findNavController().navigate(R.id.action_personalDataFragment_to_firstFragment)
            Toast.makeText(requireContext(), "Algo salió mal, reintente de nuevo", Toast.LENGTH_SHORT).show()
        }

        // Map selections to db schema values
        val selectedGenderDisplayText = genderSpinner.selectedItem
        val backendGender = displayToBackendGenderChoices[selectedGenderDisplayText]

        val selectedCountryDisplayText = countrySpinner.selectedItem
        val backendNationality = displayToBackendNationalityChoices[selectedCountryDisplayText]

        val age = agePicker.value

        val userData = Users(
            uuid = userUUID!!,
            gender = backendGender!!,
            nationality = backendNationality!!,
            age = age,
            timestamp = System.currentTimeMillis()
        )

        lifecycleScope.launch {
            // Prepare user and BFI data for sending to server
            val db = AppDatabase.getDatabase(requireContext())

            // Insert User Data locally
            db.usersDao().insertUser(userData)

            // Load BFI data
            val bfiScores = db.bfiDao().getLastInsertedScore()
            val bfiItems = db.bfiItemsDao().getItemResponses()

            val uBFIPayload = UserBFIPayload(
                    users = userData,
                    bigFiveItems = bfiItems,
                    bigFiveResults = bfiScores!!
                )

            // Gson object
            val gson = Gson()
            val uBFIPayloadJson = gson.toJson(uBFIPayload)

            // Send to server
            val comm = ServerCommunication(
                getString(R.string.serverDomain),
                getString(R.string.bfiEndpoint),
                getString(R.string.sha56hash),
                uBFIPayloadJson
            )

            comm.sendData(secure = false, callback = { success ->
                // Set a flag for User Data
                with(sharedPref.edit()) {
                    putBoolean("USER_DATA_SENT", success)
                    apply()
                }
            })
        }

        // Signal that the view has finished it's job
        with(sharedPref.edit()) {
            putBoolean("REGISTERED_USER_DATA", true)
            apply()
        }

        // Navigate to projects list
        findNavController().navigate(R.id.action_personalDataFragment_to_researchProjectsFragment)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment PersonalDataFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PersonalDataFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}