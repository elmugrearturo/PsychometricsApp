package com.arturocuriel.mipersonalidad

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.room.Room
import com.arturocuriel.mipersonalidad.models.QuestionOpen
import com.arturocuriel.mipersonalidad.models.SacksPayload
import com.arturocuriel.mipersonalidad.models.ServerCommunication
import com.arturocuriel.mipersonalidad.room.AppDatabase
import com.google.gson.Gson
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.TextAlignment
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.OutputStream

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SacksResultsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SacksResultsFragment : Fragment() {
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
        val view = inflater.inflate(R.layout.fragment_sacks_results, container, false)

        // Fill in test info
        val sacksWebView: WebView = view.findViewById(R.id.sacksWebView)
        sacksWebView.loadUrl("file:///android_asset/sacks.html")

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if we need to save info
        sendSacksItemsIfNecessary()

        // Override back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            // Navigate to the research projects fragment
            findNavController().navigate(R.id.action_sacksResultsFragment_to_researchProjectsFragment)
        }

        val pdfButton : Button = view.findViewById<Button>(R.id.sacksDownloadAnswersButton)

        pdfButton.setOnClickListener {
            getAnswersOnPDF()
        }

    }

    private fun getAnswersOnPDF() {
        val questionList = loadQuestionsFromAssets()
        lifecycleScope.launch {
            //Get data from database
            val db = Room.databaseBuilder(
                requireContext(),
                AppDatabase::class.java, "app-database"
            ).build()

            val sacksResponses = db.sacksDao().getItemResponses()

            val pdfFileName = "mis_respuestas_sacks.pdf"

            val outputStream: OutputStream? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore to save in the Downloads folder
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, pdfFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = requireContext().contentResolver?.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let { requireContext().contentResolver?.openOutputStream(it) }
            } else {
                // For older Android versions, save to external storage
                val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
                val file = File(downloadsPath, pdfFileName)
                file.outputStream()
            }

            // Create the PDF
            outputStream?.let { os ->
                val pdfWriter = PdfWriter(os)
                val pdfDocument = PdfDocument(pdfWriter)
                val document = Document(pdfDocument)

                // Set fonts
                val font = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA)
                val boldFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD)

                // Title paragraph (centered, bold)
                val title = Paragraph("Preguntas incompletas de Sacks")
                    .setFont(boldFont)
                    .setFontSize(18f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginBottom(20f)

                document.add(title)

                // Add an introduction paragraph (left-aligned)
                val intro = Paragraph("Estas son sus respuestas al test de Sacks. Llevelos con un especialista si requiere una interpretación adecuada.")
                    .setFont(font)
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.LEFT)
                    .setMarginBottom(20f)

                document.add(intro)

                var contents = ""
                for (response in sacksResponses){
                    val question = questionList[response.itemNumber - 1].text
                    val answer = response.response.lowercase()
                    val questionNumber = response.itemNumber.toString()
                    contents += "$questionNumber. $question $answer\n"
                }

                // Normal text (justified alignment)
                val bodyText = Paragraph(contents)
                    .setFont(font)
                    .setFontSize(12f)
                    .setTextAlignment(TextAlignment.JUSTIFIED)

                document.add(bodyText)

                // Add footer (centered)
                val footer = Paragraph("Fecha: ${java.util.Date()}")
                    .setFont(font)
                    .setFontSize(10f)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(20f)

                document.add(footer)
                document.close()
            }
            Toast.makeText(requireContext(), "Archivo guardado al folder de Descargas.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendSacksItemsIfNecessary() {
        val sharedPref = requireActivity().getSharedPreferences("APP_PREFS", Context.MODE_PRIVATE)
        val uuid = sharedPref.getString("UUID", "")
        val sacksItemsSent = sharedPref.getBoolean("SACKS_ITEMS_SENT", false)
        val sacksItemsReady = sharedPref.getBoolean("SACKS_ITEMS_READY", false)

        if (!sacksItemsSent and sacksItemsReady) {
            lifecycleScope.launch {
                // Prepare user and BFI data for sending to server
                val db = Room.databaseBuilder(
                    requireContext(),
                    AppDatabase::class.java, "app-database"
                ).build()

                val sacksItems = db.sacksDao().getItemResponses()

                val sacksPayload = SacksPayload(
                    uuid = uuid!!,
                    sacksItems = sacksItems,
                )

                // Gson object
                val gson = Gson()
                val sacksPayloadJson = gson.toJson(sacksPayload)

                // Send to server
                val comm = ServerCommunication(
                    getString(R.string.serverDomain),
                    getString(R.string.sacksEndpoint),
                    getString(R.string.sha56hash),
                    sacksPayloadJson
                )

                comm.sendData(secure = false, callback = { success ->
                    with(sharedPref.edit()) {
                        putBoolean("SACKS_ITEMS_SENT", success)
                        apply()
                    }
                })
            }
        }
    }

    private fun loadJSONFromAssets(): String? {
        val json: String?
        try {
            val inputStream = requireContext().assets.open("sacks_questions.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            json = String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }

    private fun loadQuestionsFromAssets(): List<QuestionOpen> {
        // Parse the JSON file into a list of QuestionMultipleChoice objects
        val questionList = mutableListOf<QuestionOpen>()

        // Gets main object
        val jsonMainObject = JSONObject(loadJSONFromAssets()!!)

        // Gets questions
        val jsonQuestionArray = jsonMainObject.getJSONArray("questions")

        // Loop through each object in the array
        for (i in 0 until jsonQuestionArray.length()) {
            val jsonQuestionObject = jsonQuestionArray.getJSONObject(i)

            // Extract the values from the JSON object
            val id = jsonQuestionObject.getInt("id")
            // TODO: Check how it would be done for multiple languages
            val text = jsonQuestionObject.getJSONObject("text").getString("es")

            // Create a Question object and add it to the list
            val question = QuestionOpen(id, text)
            questionList.add(question)
        }

        return questionList
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SacksResultsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SacksResultsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}