package com.example.contentify.viewmodel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.contentify.network.ProcessedPdfResults

abstract class ProcessorViewModel : ViewModel(){

    protected val _status = MutableLiveData<Status>()
    protected val _pdfUrl = MutableLiveData<String>()
    val status: LiveData<Status> = _status
    val pdfUrl: LiveData<String> = _pdfUrl

    fun noInternet(){
        _status.value = Status.NO_INTERNET
    }
}