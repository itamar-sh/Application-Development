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
import com.example.contentify.viewmodel.Status
import com.example.contentify.viewmodel.SummarizeScreenViewModel
import com.example.contentify.viewmodel.SummarizeScreenViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class SummarizeScreenFragment: Fragment(R.layout.summarize_screen_fragment) {
    private val args: SummarizeScreenFragmentArgs by navArgs()
    private lateinit var name: TextView
    private lateinit var summarizedText: TextView
    private lateinit var summaryScreen: ScrollView
    private lateinit var summarizeScreenViewModel: SummarizeScreenViewModel
    private lateinit var coroutineExceptionHandler : CoroutineExceptionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        summarizeScreenViewModel = ViewModelProvider(this,SummarizeScreenViewModelFactory(
                imageProcessorRepositoryFactory = ::PdfProcessorRepository,
            Dispatchers.IO)
        ).get()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coroutineExceptionHandler = CoroutineExceptionHandler { _, _ ->
            summarizeScreenViewModel.noInternet()
            val snack = Snackbar.make(
                view, SNACKBAR_MASSAGE_GET_SUMMARY,
                Snackbar.LENGTH_LONG
            )
            snack.show()
        }
        view.setupViews()
        summarizeScreenViewModel.getSummarizedPdf(args.jobId, coroutineExceptionHandler)
        observeState()
    }

    private fun observeState() {
        summarizeScreenViewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                Status.FINISH_CREATE_SUMMARY -> showSummarizeText()
                Status.PROCESSING_SUMMARY -> {
                    Thread.sleep(5000)
                    summarizeScreenViewModel.getSummarizedPdf(args.jobId, coroutineExceptionHandler)
                }
                Status.FAIL_CREATE_SUMMERY ->{
                    val snack = Snackbar.make(
                        requireView(), SNACKBAR_MASSAGE_FAIL,
                        Snackbar.LENGTH_LONG
                    )
                    snack.show()
                    summaryScreen.setBackgroundResource(R.drawable.background_sumarry)
                }
                Status.NO_INTERNET ->
                    summaryScreen.setBackgroundResource(R.drawable.background_sumarry)
                else -> {}
            }
        }
    }

    private fun showSummarizeText() {
        summarizeScreenViewModel.pdfUrl.observe(viewLifecycleOwner) {
            GlobalScope.launch(Dispatchers.IO) {
                val url = URL(it)
                val inputStream = url.openStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val text = reader.readText()

                withContext(Dispatchers.Main) {
                    // Set the text of the TextView
                    summarizedText.text = text
                    summaryScreen.setBackgroundResource(R.drawable.background_sumarry)
                }
            }
        }
    }

    private fun View.setupViews() {
        summaryScreen = findViewById(R.id.summary_screen)
        name = findViewById(R.id.name)
        summarizedText = findViewById(R.id.summarized_text)
        name.text = args.pdfItem.name
    }

    companion object {
        private const val SNACKBAR_MASSAGE_GET_SUMMARY = "Can't get summary without Internet"
        private const val SNACKBAR_MASSAGE_FAIL = "Failed in getting summary"
    }

}
