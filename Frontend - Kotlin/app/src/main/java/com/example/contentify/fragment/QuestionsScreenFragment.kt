package com.example.contentify.fragment

import android.annotation.SuppressLint
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
import java.lang.Thread.sleep
import java.net.URL


class QuestionsScreenFragment: Fragment(R.layout.questions_screen_fragment) {
    private val args: QuestionsScreenFragmentArgs by navArgs()
    private lateinit var name: TextView
    private lateinit var questionsText: TextView
    private lateinit var questionsScreen: ScrollView
    private lateinit var questionsScreenViewModel: QuestionsScreenViewModel
    private lateinit var coroutineExceptionHandler : CoroutineExceptionHandler


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        questionsScreenViewModel = ViewModelProvider(this,QuestionsScreenViewModelFactory(
            imageProcessorRepositoryFactory = ::PdfProcessorRepository,
            coroutineDispatcher = Dispatchers.IO
        )
        ).get()
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coroutineExceptionHandler = CoroutineExceptionHandler { _, _ ->
            questionsScreenViewModel.noInternet()
            val snack = Snackbar.make(
                view, SNACKBAR_MASSAGE_GET_QUESTION,
                Snackbar.LENGTH_LONG
            )
            snack.show()
        }
        view.setupViews()
        questionsScreenViewModel.getQuestion(args.jobId, coroutineExceptionHandler)
        observeState()
    }

    private fun observeState() {
        questionsScreenViewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                Status.FINISH_CREATE_QUESTIONS -> showSummarizeText()
                Status.PROCESSING_QUESTIONS -> {
                    sleep(5000)
                    questionsScreenViewModel.getQuestion(args.jobId, coroutineExceptionHandler)
                }
                Status.FAIL_CREATE_QUESTIONS -> {
                    val snack = Snackbar.make(
                    requireView(), SNACKBAR_MASSAGE_FAIL,
                    Snackbar.LENGTH_LONG
                    )
                    snack.show()
                    questionsScreen.setBackgroundResource(R.drawable.background_questions)
                }
                Status.NO_INTERNET ->
                    questionsScreen.setBackgroundResource(R.drawable.background_questions)
                else -> {}
            }
        }
    }

    private fun showSummarizeText() {
        questionsScreenViewModel.pdfUrl.observe(viewLifecycleOwner) {
            GlobalScope.launch(Dispatchers.IO) {
                val url = URL(it)
                val inputStream = url.openStream()
                val reader = BufferedReader(InputStreamReader(inputStream))
                val text = reader.readText()

                withContext(Dispatchers.Main) {
                    // Set the text of the TextView
                    questionsText.text = text
                    questionsScreen.setBackgroundResource(R.drawable.background_questions)
                }
            }
        }
    }

    private fun View.setupViews() {
        questionsScreen = findViewById(R.id.questions_screen)
        name = findViewById(R.id.name)
        questionsText = findViewById(R.id.questions_text)
        name.text = args.pdfItem.name
    }

    companion object {
        private const val SNACKBAR_MASSAGE_GET_QUESTION = "Can't get question without Internet"
        private const val SNACKBAR_MASSAGE_FAIL = "Failed in getting question"
    }

}