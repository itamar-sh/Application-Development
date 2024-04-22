package com.example.contentify.viewmodel

import androidx.lifecycle.*
import com.example.contentify.network.PdfProcessor
import com.example.contentify.network.PdfProcessorRepository
import com.example.contentify.network.ProcessPdfApi
import com.example.contentify.network.ProcessedPdfResults
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SummarizeScreenViewModel(
    private val pdfProcessorRepository: PdfProcessor,
    private val coroutineDispatcher: CoroutineDispatcher
): ProcessorViewModel(){

    fun getSummarizedPdf(jobId: String, exceptionHandler: CoroutineExceptionHandler) {
        viewModelScope.launch(exceptionHandler) {
            var tempStatus = Status.WAITING_FOR_RESPONSE
            var tempPdfUrl = _pdfUrl.value
            _status.value = tempStatus
            withContext(coroutineDispatcher) {
                pdfProcessorRepository.getSummarizedPdf(jobId)
                    .onSuccess { PdfResults ->
                        when (PdfResults) {
                            is ProcessedPdfResults.Processing ->
                                tempStatus = Status.PROCESSING_SUMMARY
                            is ProcessedPdfResults.Ready -> {
                                tempPdfUrl = PdfResults.url
                                tempStatus = Status.FINISH_CREATE_SUMMARY
                            }
                        }
                    }
                    .onFailure { tempStatus = Status.FAIL_CREATE_SUMMERY }
            }
            _pdfUrl.value = tempPdfUrl
            _status.value = tempStatus
        }
    }
}



class SummarizeScreenViewModelFactory(
    private val imageProcessorRepositoryFactory: (ProcessPdfApi) -> PdfProcessorRepository,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val processImageApi = ProcessPdfApi.instance
        return SummarizeScreenViewModel(
            imageProcessorRepositoryFactory.invoke(processImageApi),
            coroutineDispatcher
        ) as T
    }
}



