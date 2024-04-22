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

class StudiesScreenViewModel(
    private val pdfProcessorRepository: PdfProcessor,
    private val coroutineDispatcher: CoroutineDispatcher
): ProcessorViewModel(){

    fun getStudies(jobId: String, exceptionHandler: CoroutineExceptionHandler) {
        viewModelScope.launch(exceptionHandler) {
            var tempStatus = Status.WAITING_FOR_RESPONSE
            var tempPdfUrl = _pdfUrl.value
            _status.value = tempStatus
            withContext(coroutineDispatcher) {
                pdfProcessorRepository.getStudies(jobId)
                    .onSuccess { PdfResults ->
                        when (PdfResults) {
                            is ProcessedPdfResults.Processing ->
                                tempStatus = Status.PROCESSING_STUDIES
                            is ProcessedPdfResults.Ready -> {
                                tempPdfUrl = PdfResults.url
                                tempStatus = Status.FINISH_CREATE_STUDIES
                            }
                        }
                    }
                    .onFailure { tempStatus = Status.FAIL_CREATE_STUDIES }
            }
            _pdfUrl.value = tempPdfUrl
            _status.value = tempStatus
        }
    }
}



class StudiesScreenViewModelFactory(
    private val imageProcessorRepositoryFactory: (ProcessPdfApi) -> PdfProcessorRepository,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val processImageApi = ProcessPdfApi.instance
        return StudiesScreenViewModel(
            imageProcessorRepositoryFactory.invoke(processImageApi),
            coroutineDispatcher
        ) as T
    }
}



