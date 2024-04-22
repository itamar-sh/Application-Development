package com.example.contentify.fragment

import android.content.pm.PackageManager
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.get
import com.example.contentify.PdfItem
import com.example.contentify.R
import com.example.contentify.adapter.PdfRecyclerViewAdapter
import com.example.contentify.provider.PdfItemsProvider
import com.example.contentify.viewmodel.PdfManagementViewModelFactory
import com.example.contentify.viewmodel.WelcomeScreenViewModel
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.contentify.viewmodel.QuestionsScreenViewModelFactory
import com.example.contentify.viewmodel.Status
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers

class WelcomeScreenFragment: Fragment(R.layout.welcome_screen_fragment) {
    private lateinit var welcomeScreenViewModel: WelcomeScreenViewModel
    private lateinit var title: TextView
    private lateinit var appDescription: TextView
    private lateinit var textview: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PdfRecyclerViewAdapter
    private lateinit var coroutineExceptionHandler : CoroutineExceptionHandler

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                welcomeScreenViewModel.refreshPdfItems(coroutineExceptionHandler)
            } else {
                Snackbar.make(requireView(), R.string.no_permissions_message,
                    Snackbar.LENGTH_INDEFINITE).show()
            }
        }

    private fun requestPermissionIfNeeded() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            welcomeScreenViewModel.refreshPdfItems(coroutineExceptionHandler)
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        welcomeScreenViewModel = ViewModelProvider(this, PdfManagementViewModelFactory(
            pdfItemsProviderFactory = ::PdfItemsProvider,
            coroutineDispatcher = Dispatchers.IO)
        ).get()
    }

    override fun onStart() {
        super.onStart()
        requestPermissionIfNeeded()
    }

    private fun observeState() {
        welcomeScreenViewModel.status.observe(viewLifecycleOwner) {
            when (it) {
                Status.FAIL_GET_PDF_ITEM -> {
                    val snack = Snackbar.make(
                        requireView(), SNACKBAR_MASSAGE_FAIL,
                        Snackbar.LENGTH_LONG
                    )
                    snack.show()
                }
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        coroutineExceptionHandler = CoroutineExceptionHandler { _, _ ->
            welcomeScreenViewModel.noInternet()
            val snack = Snackbar.make(
                view,  SNACKBAR_MASSAGE_GET_PDF,
                Snackbar.LENGTH_LONG
            )
            snack.show()
            }
        initRecyclerView()
        initSelectedPdfTextView(view)
        observeState()
    }
    private fun initSelectedPdfTextView(view: View){
        textview = view.findViewById(R.id.selectedPdf)
        title = view.findViewById(R.id.title)
        appDescription = view.findViewById(R.id.app_description)
        textview.setOnClickListener{
            selectPdf()
        }
    }

    // Intent for navigating to the files
    private fun selectPdf() {
        val pdfIntent = Intent(Intent.ACTION_GET_CONTENT)
        pdfIntent.type = "application/pdf"
        pdfIntent.addCategory(Intent.CATEGORY_OPENABLE)
        startActivityForResult(pdfIntent, 12)
    }

    @SuppressLint("Range")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            12 -> if (resultCode == RESULT_OK) {
                val uri: Uri = data?.data!!
                val uriString: String = uri.toString()
                var pdfName: String? = null
                if (uriString.startsWith("content://")) {
                    var myCursor: Cursor? = null
                    try {
                        // Setting the PDF to the TextView
                        myCursor = requireContext().contentResolver.query(uri,
                            null, null, null, null)
                        if (myCursor != null && myCursor.moveToFirst()) {
                            pdfName = myCursor.getString(myCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                            welcomeScreenViewModel.addItem(uriString, pdfName)
                            refreshRecyclerView()
                        }
                    } finally {
                        myCursor?.close()
                    }
                }
            }
        }
    }


    private fun initRecyclerView() {
        recyclerView = requireView().findViewById(R.id.previous_pdf)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), NUMBER_OF_COLUMNS)
        refreshRecyclerView()
    }

    private fun refreshRecyclerView() {
        adapter = PdfRecyclerViewAdapter(::onItemClick)
        recyclerView.adapter = adapter
        welcomeScreenViewModel.getWelcomeScreenModel().observe(viewLifecycleOwner) {
            adapter.submitList(it.pdfItems)
        }
    }

    private fun onItemClick(pdfItem : PdfItem) {
        val action = WelcomeScreenFragmentDirections
            .actionWelcomeScreenFragmentToPdfManagementFragment(pdfItem)
        findNavController().navigate(action)
    }

    companion object {
        private const val NUMBER_OF_COLUMNS = 3
        private const val SNACKBAR_MASSAGE_GET_PDF = "Can't get old pdf from db without Internet"
        private const val SNACKBAR_MASSAGE_FAIL = "Failed in getting old pdf"
    }
}
