package com.example.contentify

import android.net.Uri
import com.example.contentify.viewmodel.*
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PdfToResultTest {
    private var fakePdfProcessor = FakePdfProcessor()

    @Before
    fun initValue(){
        fakePdfProcessor.hasInternet = true
        fakePdfProcessor.setUrlAndStatus(null, "crated")
        fakePdfProcessor.isSuccessful = true
    }

    @Test
    fun testPdfScreenStatus() = runTest {
        val viewModel = PdfScreenViewModel(fakePdfProcessor,
            UnconfinedTestDispatcher(testScheduler))
        val coroutineExceptionHandler = CoroutineExceptionHandler {_, _ ->
            viewModel.noInternet()
        }
        assertEquals(viewModel.status.value, null)
        viewModel.uploadReq(pdfItem1, coroutineExceptionHandler)
        assertEquals(Status.GET_URL_AND_JOD_ID, viewModel.status.value)
        viewModel.uploadPdf(pdfItem1, coroutineExceptionHandler)
        assertEquals(Status.FINISH_UPLOAD_PDF, viewModel.status.value)
        fakePdfProcessor.isSuccessful = false
        viewModel.uploadReq(pdfItem1, coroutineExceptionHandler)
        assertEquals(Status.FAIL_IN_GET_URL_AND_JOD_ID, viewModel.status.value)
        fakePdfProcessor.isSuccessful = true
        viewModel.uploadReq(pdfItem1, coroutineExceptionHandler)
        fakePdfProcessor.isSuccessful = false
        viewModel.uploadPdf(pdfItem1, coroutineExceptionHandler)
        assertEquals(Status.FAIL_UPLOAD_PDF, viewModel.status.value)
        fakePdfProcessor.isSuccessful = true
        fakePdfProcessor.hasInternet = false
        viewModel.uploadReq(pdfItem1, coroutineExceptionHandler)
        assertEquals(Status.NO_INTERNET, viewModel.status.value)
        fakePdfProcessor.hasInternet = true
        viewModel.uploadReq(pdfItem1, coroutineExceptionHandler)
        fakePdfProcessor.hasInternet = false
        viewModel.uploadPdf(pdfItem1, coroutineExceptionHandler)
        assertEquals(Status.NO_INTERNET, viewModel.status.value)
    }

    @Test
    fun testStudiesScreenStatus() = runTest {
        val viewModel = StudiesScreenViewModel(fakePdfProcessor,
            UnconfinedTestDispatcher(testScheduler))
        val coroutineExceptionHandler = CoroutineExceptionHandler {_, _ ->
            viewModel.noInternet()
        }
        assertEquals(null, viewModel.status.value)
        viewModel.getStudies(jobId, coroutineExceptionHandler)
        assertEquals(Status.PROCESSING_STUDIES, viewModel.status.value)
        assertEquals(null, viewModel.pdfUrl.value)
        fakePdfProcessor.setUrlAndStatus(myUrl, "success")
        viewModel.getStudies(jobId, coroutineExceptionHandler)
        assertEquals(Status.FINISH_CREATE_STUDIES, viewModel.status.value)
        assertEquals(myUrl, viewModel.pdfUrl.value)
        fakePdfProcessor.hasInternet = false
        viewModel.getStudies(jobId, coroutineExceptionHandler)
        assertEquals(Status.NO_INTERNET, viewModel.status.value)
        fakePdfProcessor.hasInternet = true
        fakePdfProcessor.isSuccessful = false
        viewModel.getStudies(jobId, coroutineExceptionHandler)
        assertEquals(Status.FAIL_CREATE_STUDIES, viewModel.status.value)
    }

    @Test
    fun testSummaryScreenStatus() = runTest {
        val viewModel = SummarizeScreenViewModel(fakePdfProcessor,
            UnconfinedTestDispatcher(testScheduler))
        val coroutineExceptionHandler = CoroutineExceptionHandler {_, _ ->
            viewModel.noInternet()
        }
        assertEquals(null, viewModel.status.value)
        viewModel.getSummarizedPdf(jobId, coroutineExceptionHandler)
        assertEquals(Status.PROCESSING_SUMMARY, viewModel.status.value)
        assertEquals(null, viewModel.pdfUrl.value)
        fakePdfProcessor.setUrlAndStatus(myUrl, "success")
        viewModel.getSummarizedPdf(jobId, coroutineExceptionHandler)
        assertEquals(Status.FINISH_CREATE_SUMMARY, viewModel.status.value)
        assertEquals(myUrl, viewModel.pdfUrl.value)
        fakePdfProcessor.hasInternet = false
        viewModel.getSummarizedPdf(jobId, coroutineExceptionHandler)
        assertEquals(Status.NO_INTERNET, viewModel.status.value)
        fakePdfProcessor.hasInternet = true
        fakePdfProcessor.isSuccessful = false
        viewModel.getSummarizedPdf(jobId, coroutineExceptionHandler)
        assertEquals(Status.FAIL_CREATE_SUMMERY, viewModel.status.value)
    }

    @Test
    fun testQuestionScreenStatus() = runTest {
        val viewModel = QuestionsScreenViewModel(fakePdfProcessor,
            UnconfinedTestDispatcher(testScheduler))
        val coroutineExceptionHandler = CoroutineExceptionHandler {_, _ ->
            viewModel.noInternet()
        }
        assertEquals(null, viewModel.status.value)
        viewModel.getQuestion(jobId, coroutineExceptionHandler)
        assertEquals(Status.PROCESSING_QUESTIONS, viewModel.status.value)
        assertEquals(null, viewModel.pdfUrl.value)
        fakePdfProcessor.setUrlAndStatus(myUrl, "success")
        viewModel.getQuestion(jobId, coroutineExceptionHandler)
        assertEquals(Status.FINISH_CREATE_QUESTIONS, viewModel.status.value)
        assertEquals(myUrl, viewModel.pdfUrl.value)
        fakePdfProcessor.hasInternet = false
        viewModel.getQuestion(jobId, coroutineExceptionHandler)
        assertEquals(Status.NO_INTERNET, viewModel.status.value)
        fakePdfProcessor.hasInternet = true
        fakePdfProcessor.isSuccessful = false
        viewModel.getQuestion(jobId, coroutineExceptionHandler)
        assertEquals(Status.FAIL_CREATE_QUESTIONS, viewModel.status.value)
    }



    companion object {
        private val pdfItem1 = PdfItem("test1_pdf", Uri.EMPTY.toString())
        private const val jobId = "0"
        private const val myUrl = "my_url"
    }


}