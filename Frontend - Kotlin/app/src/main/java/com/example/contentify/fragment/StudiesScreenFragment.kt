package com.example.contentify.fragment

import android.os.Bundle
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.navArgs
import com.example.contentify.R
import com.example.contentify.network.PdfProcessorRepository
import com.example.contentify.viewmodel.*
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL


class StudiesScreenFragment: Fragment(R.layout.studies_screen_fragment) {
    private val args: StudiesScreenFragmentArgs by navArgs()
    private lateinit var name: TextView
    private lateinit var studiesScreen: ScrollView
    private lateinit var studiesText: TextView
    private lateinit var studiesScreenViewModel: StudiesScreenViewModel
    private lateinit var coroutineExceptionHandler : CoroutineExceptionHandler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        studiesScreenViewModel = ViewModelProvider(this,StudiesScreenViewModelFactory(
            imageProcessorRepositoryFactory = ::PdfProcessorRepository,
            coroutineDispatcher = Dispatchers.IO
        )
        ).get()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coroutineExceptionHandler = CoroutineExceptionHandler { _, _ ->
            studiesScreenViewModel.noInternet()
            val snack = Snackbar.make(
                view, SNACKBAR_MASSAGE_GET_STUDIES,
                Snackbar.LENGTH_LONG
            )
            snack.show()

        }
        view.setupViews()
        studiesScreenViewModel.getStudies(args.jobId, coroutineExceptionHandler)
        observeState()
    }

    private fun observeState() {
        studiesScreenViewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                Status.FINISH_CREATE_STUDIES -> showSummarizeText()
                Status.PROCESSING_STUDIES -> {
                    Thread.sleep(5000)
                    studiesScreenViewModel.getStudies(args.jobId, coroutineExceptionHandler)
                }
                Status.FAIL_CREATE_STUDIES -> {
                    val snack = Snackbar.make(
                        requireView(), SNACKBAR_MASSAGE_FAIL,
                        Snackbar.LENGTH_LONG
                    )
                    snack.show()
                    studiesScreen.setBackgroundResource(R.drawable.background_studies)
                }
                Status.NO_INTERNET ->
                    studiesScreen.setBackgroundResource(R.drawable.background_studies)
                else -> {}
            }
        }
    }

    private fun showSummarizeText() {
        studiesScreenViewModel.pdfUrl.observe(viewLifecycleOwner) {
            GlobalScope.launch(Dispatchers.IO) {
                val url = URL(it)
                val inputStream = url.openStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val text = reader.readText()

                withContext(Dispatchers.Main) {
                    // Set the text of the TextView
                    studiesText.text = text
                    studiesScreen.setBackgroundResource(R.drawable.background_studies)
                }
            }
        }
    }

    private fun View.setupViews() {
        studiesScreen = findViewById(R.id.studies_screen)
        name = findViewById(R.id.name)
        studiesText = findViewById(R.id.studies_text)
        name.text = args.pdfItem.name
    }

    companion object {
        private const val SNACKBAR_MASSAGE_GET_STUDIES = "Can't get studies without Internet"
        private const val SNACKBAR_MASSAGE_FAIL = "Failed in getting studies"
    }
}