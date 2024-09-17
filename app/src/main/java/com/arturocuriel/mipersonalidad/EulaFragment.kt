package com.arturocuriel.mipersonalidad

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import java.util.UUID

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [EulaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EulaFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment_eula, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val webView: WebView = view.findViewById(R.id.webviewEULA)
        val cbAccept: CheckBox = view.findViewById(R.id.cbAccept)
        val btnAccept: Button = view.findViewById(R.id.btnAccept)
        val btnDecline: Button = view.findViewById(R.id.btnDecline)

        // Load HTML file from assets
        webView.loadUrl("file:///android_asset/eula.html")

        btnAccept.setOnClickListener {
            if (cbAccept.isChecked) {
                saveLicenseAccepted()
                navigateToNextScreen() // Navigate to the app's personal data form
            } else {
                Toast.makeText(context,
                    "Por favor acepte el contrato de licencia para continuar.",
                    Toast.LENGTH_SHORT).show()
            }
        }

        btnDecline.setOnClickListener {
            navigateToPreviousScreen()
            //activity?.finish()
        }
    }

    private fun saveLicenseAccepted() {
        val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("LICENSE_ACCEPTED", true)
            putBoolean("HAS_PERSONAL_DATA", false)
            putString("UUID", generateUUID()) // Generate a UUID for sending results
            apply()
        }
    }

    private fun generateUUID(): String {
        return UUID.randomUUID().toString()
    }

    private fun navigateToNextScreen() {
        findNavController().navigate(R.id.action_eulaFragment_to_personalDataFragment)
    }

    private fun navigateToPreviousScreen() {
        findNavController().navigate(R.id.action_eulaFragment_to_firstFragment)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment EulaFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            EulaFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}