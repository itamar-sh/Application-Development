package com.example.contentify.viewmodel
import androidx.lifecycle.*
import com.example.contentify.PdfModel
import com.example.contentify.network.PdfProcessorRepository
import com.example.contentify.network.ProcessPdfApi
import com.example.contentify.provider.ItemProvider
import com.example.contentify.provider.PdfItemsProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class WelcomeScreenViewModel(
    private val pdfItemsProvider: ItemProvider,
    private val coroutineDispatcher: CoroutineDispatcher
): ViewModel() {
    private val pdfModel = MutableLiveData<PdfModel>()
    protected val _status = MutableLiveData<Status>()
    val status: LiveData<Status> = _status

    fun getWelcomeScreenModel(): LiveData<PdfModel> = pdfModel

    fun noInternet(){
        pdfModel.value = PdfModel(listOf())
        _status.value = Status.NO_INTERNET
    }

    fun refreshPdfItems(exceptionHandler: CoroutineExceptionHandler) {
        var tempPdfStatus = Status.FAIL_GET_PDF_ITEM
        var tempPdfModel = PdfModel(listOf())
        viewModelScope.launch(exceptionHandler) {
            withContext(coroutineDispatcher) {
                pdfItemsProvider.getAllItems()
                    .onSuccess { pdfModelValue ->
                        tempPdfModel = pdfModelValue
                        tempPdfStatus = Status.FINISH_GET_PDF_ITEM
                    }
                    .onFailure { tempPdfStatus = Status.FAIL_GET_PDF_ITEM }
            }
            pdfModel.value = tempPdfModel
            _status.value = tempPdfStatus
        }

    }

    fun addItem(uri: String, pdfName: String) {
        pdfModel.value = pdfItemsProvider.addItem(uri, pdfName)
    }

}

class  PdfManagementViewModelFactory(
    private val pdfItemsProviderFactory: () -> PdfItemsProvider,
    private val coroutineDispatcher: CoroutineDispatcher
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return WelcomeScreenViewModel(pdfItemsProviderFactory(),
            coroutineDispatcher) as T
    }
}