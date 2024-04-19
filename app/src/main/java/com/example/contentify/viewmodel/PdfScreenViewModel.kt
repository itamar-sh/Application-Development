package com.example.contentify.viewmodel


import androidx.core.net.toUri
import androidx.lifecycle.*
import com.example.contentify.PdfItem
import com.example.contentify.network.*
import kotlinx.coroutines.*
import okhttp3.RequestBody
import java.util.*

class PdfScreenViewModel(
    private val pdfProcessorRepository: PdfProcessor,
    private val coroutineDispatcher: CoroutineDispatcher,
    private val requestBodyBuilder: RequestBodyBuilder? = null
) : ViewModel(){
    private var url: String? = null
    var jobId: String? = null

    private val _status = MutableLiveData<Status>()
    val status: LiveData<Status> = _status

    fun checkIfExist(pdfItem: PdfItem): String? {
        if (pdfItem.job_id != null){
            jobId = pdfItem.job_id
            return pdfItem.job_id
        }
        return null
    }

    fun noInternet(){
        _status.value = Status.NO_INTERNET
    }

    fun uploadReq(pdfItem: PdfItem, exceptionHandler: CoroutineExceptionHandler){
        viewModelScope.launch(exceptionHandler) {
            var tempStatus = Status.WAITING_FOR_RESPONSE
            _status.value = tempStatus
            withContext(coroutineDispatcher){
                pdfProcessorRepository.uploadReq(pdfItem.name)
                    .onSuccess { uploadRequest ->
                        url = uploadRequest.url
                        jobId = uploadRequest.jobId
                        pdfItem.job_id = jobId
                        tempStatus = Status.GET_URL_AND_JOD_ID
                    }
                    .onFailure {tempStatus = Status.FAIL_IN_GET_URL_AND_JOD_ID}
            }
            _status.value = tempStatus
        }
    }


    fun uploadPdf(pdfItem: PdfItem, exceptionHandler: CoroutineExceptionHandler) {
        viewModelScope.launch(exceptionHandler){
            var tempStatus = Status.WAITING_FOR_RESPONSE
            _status.value = tempStatus
            withContext(coroutineDispatcher) {
                var requestBody: RequestBody? = null
                if (requestBodyBuilder!=null){
                    requestBody = requestBodyBuilder.build(pdfItem.uri!!.toUri())
                }
                if (url != null) {
                    pdfProcessorRepository.uploadPdf(url!!, requestBody)
                        .onSuccess { tempStatus = Status.FINISH_UPLOAD_PDF}
                        .onFailure { tempStatus = Status.FAIL_UPLOAD_PDF}
                } else {
                    tempStatus = Status.FAIL_UPLOAD_PDF
                }
            }
            _status.value = tempStatus
        }
    }
}


class PdfScreenViewModelFactory(
    private val requestBodyBuilderFactory: () -> RequestBodyBuilder,
    private val imageProcessorRepositoryFactory: (ProcessPdfApi) -> PdfProcessorRepository,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val processImageApi = ProcessPdfApi.instance
        return PdfScreenViewModel(
            imageProcessorRepositoryFactory.invoke(processImageApi),
            coroutineDispatcher,
            requestBodyBuilderFactory.invoke()
        ) as T
    }
}