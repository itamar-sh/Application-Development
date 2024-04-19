package com.example.contentify.fragment
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.contentify.R
import com.example.contentify.network.PdfProcessorRepository
import com.example.contentify.network.RequestBodyBuilder
import com.example.contentify.viewmodel.PdfScreenViewModel
import com.example.contentify.viewmodel.PdfScreenViewModelFactory
import com.example.contentify.viewmodel.Status
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers


class PdfManagementFragment: Fragment(R.layout.pdf_management_fragment) {
    private val args: PdfManagementFragmentArgs by navArgs()
    private lateinit var studies: Button
    private lateinit var questions: Button
    private lateinit var summarize: Button
    private lateinit var imageView: ImageView
    private lateinit var name: TextView
    private lateinit var loading: TextView
    var jobId: String? = null
    private lateinit var pdfScreenViewModel: PdfScreenViewModel
    private lateinit var coroutineExceptionHandler : CoroutineExceptionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pdfScreenViewModel = ViewModelProvider(this, PdfScreenViewModelFactory(
            imageProcessorRepositoryFactory = ::PdfProcessorRepository,
            requestBodyBuilderFactory = {
                RequestBodyBuilder(requireContext().applicationContext.contentResolver) },
            coroutineDispatcher = Dispatchers.IO
        )
        ).get()
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coroutineExceptionHandler = CoroutineExceptionHandler { _, _ ->
            pdfScreenViewModel.noInternet()
            val snack = Snackbar.make(
                view, SNACKBAR_MASSAGE_FAIL_UPLOAD_PDF,
                Snackbar.LENGTH_LONG
            )
            loading.visibility = View.INVISIBLE
            imageView.setImageResource(R.color.white)
            snack.show()

        }
        view.setupViews()
        jobId = pdfScreenViewModel.checkIfExist(args.pdfItem)
        if (jobId == null){
            pdfScreenViewModel.uploadReq(args.pdfItem, coroutineExceptionHandler)
            observeState()
        }
        else {
            initOptions()
            Status.FINISH_UPLOAD_PDF
        }
    }

    private fun observeState() {
        pdfScreenViewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                Status.GET_URL_AND_JOD_ID -> pdfScreenViewModel.uploadPdf(args.pdfItem,
                    coroutineExceptionHandler)
                Status.FINISH_UPLOAD_PDF -> initOptions()
                Status.FAIL_UPLOAD_PDF -> showFailSnackbar(Status.FAIL_UPLOAD_PDF)
                Status.FAIL_IN_GET_URL_AND_JOD_ID -> showFailSnackbar(Status.FAIL_IN_GET_URL_AND_JOD_ID)
                else -> {}
            }
        }
    }

    private fun showFailSnackbar(failStatus: Status) {
        Snackbar.make(requireView(), failStatus.toString(),
            Snackbar.LENGTH_INDEFINITE).show()
    }

    private fun initOptions() {
        jobId = pdfScreenViewModel.jobId
        if (jobId != null) {
            studies.visibility = View.VISIBLE
            questions.visibility = View.VISIBLE
            summarize.visibility = View.VISIBLE
            loading.visibility = View.INVISIBLE
            imageView.setImageResource(R.drawable.background_pdf_screen)
            setClickListeners(jobId!!)
        }
    }
    private fun setClickListeners(jobId: String) {
        studies.setOnClickListener {
            val action = PdfManagementFragmentDirections
                .actionPdfManagementFragmentToStudiesScreenFragment(args.pdfItem, jobId)
            findNavController().navigate(action)
        }
        questions.setOnClickListener {
            val action = PdfManagementFragmentDirections
                .actionPdfManagementFragmentToQuestionsScreenFragment(args.pdfItem, jobId)
            findNavController().navigate(action)
        }
        summarize.setOnClickListener {
            val action = PdfManagementFragmentDirections
                .actionPdfManagementFragmentToSummarizeScreenFragment(args.pdfItem, jobId)
            findNavController().navigate(action)
        }
    }


    private fun View.setupViews() {
        studies = findViewById(R.id.studies)
        questions = findViewById(R.id.questions)
        summarize = findViewById(R.id.summarize)
        loading = findViewById(R.id.loading)
        name =  findViewById(R.id.name)
        imageView = findViewById(R.id.image_view)
        name.text = args.pdfItem.name
    }

    companion object {
        private const val SNACKBAR_MASSAGE_FAIL_UPLOAD_PDF = "Can't upload  pdf without Internet"
    }

}
