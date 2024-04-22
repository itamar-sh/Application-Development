package com.example.contentify

import android.net.Uri
import org.junit.Test
import org.junit.Assert.*
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.contentify.provider.ItemProvider
import com.example.contentify.provider.PdfItemsProvider
import com.example.contentify.viewmodel.WelcomeScreenViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Rule
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.reflect.jvm.internal.impl.load.java.lazy.descriptors.DeclaredMemberIndex.Empty

@get:Rule
val instantLiveData = InstantTaskExecutorRule()


@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class WelcomeScreenTest {
    private var fakePdfItemsProvider = FakePdfItemsProvider(pdfList1)


    @Before
    fun initInternet(){
        hasInternet = true
    }

    @Test
    fun testLiveDataWithAddItem() = runTest {
        val viewModel = WelcomeScreenViewModel(fakePdfItemsProvider, UnconfinedTestDispatcher(testScheduler))
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, _ -> }
        viewModel.refreshPdfItems(coroutineExceptionHandler)
        assertEquals(fakePdfItemsProvider.getAllItems().getOrNull(), viewModel.getWelcomeScreenModel().value)
        fakePdfItemsProvider.addItem(Uri.EMPTY.toString(), "empty_uri")
        viewModel.refreshPdfItems(coroutineExceptionHandler)
        assertEquals(fakePdfItemsProvider.getAllItems().getOrNull(), viewModel.getWelcomeScreenModel().value)
    }

    @Test
    fun testLiveDataNoInternet() = runTest {
        hasInternet = false
        val viewModel = WelcomeScreenViewModel(fakePdfItemsProvider, UnconfinedTestDispatcher(testScheduler))
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, _ ->
            viewModel.noInternet()
        }
        viewModel.refreshPdfItems(coroutineExceptionHandler)
        assertEquals(emptyList, viewModel.getWelcomeScreenModel().value)
    }

    @Test
    fun realPdfProvider() = runTest {
        val pdfItemsProvider = PdfItemsProvider()
        val viewModel = WelcomeScreenViewModel(pdfItemsProvider, UnconfinedTestDispatcher(testScheduler))
        val coroutineExceptionHandler = CoroutineExceptionHandler { _, _ -> }
        var pdfItems = emptyList
        val oldValues = pdfItemsProvider.getAllItems()
        oldValues.onSuccess {
            pdfItems = it
        }

        val mutablePdfItems = mutableListOf<PdfItem>()
        mutablePdfItems.addAll(pdfItems.pdfItems)
        mutablePdfItems.add(pdfItem1)
        val newPdfItems = PdfModel(mutablePdfItems)
        viewModel.refreshPdfItems(coroutineExceptionHandler)
        assertEquals(oldValues.getOrNull(), viewModel.getWelcomeScreenModel().value)
        viewModel.addItem("myUri", "test1_pdf")
        assertEquals(newPdfItems.pdfItems.size,
            viewModel.getWelcomeScreenModel().value!!.pdfItems.size)
        assertEquals(pdfItem1.name,
            viewModel.getWelcomeScreenModel().value!!.pdfItems[newPdfItems.pdfItems.size - 1].name)
        assertEquals(pdfItem1.uri,
            viewModel.getWelcomeScreenModel().value!!.pdfItems[newPdfItems.pdfItems.size - 1].uri)
    }

    companion object {
        private val pdfItem1 = PdfItem("test1_pdf", "myUri")
        private val pdfList1 = mutableListOf(pdfItem1)
        private val emptyList = PdfModel(listOf())
        private var hasInternet = true


        class FakePdfItemsProvider(pdfList: MutableList<PdfItem>): ItemProvider {
            private val pdfListData = pdfList
            override suspend fun getAllItems(): Result<PdfModel> {
                if (!hasInternet) {
                    throw InternetException()
                }
                return Result.success(PdfModel(pdfListData))
            }

            override fun addItem(uri: String, pdfName: String): PdfModel {
                pdfListData.add(PdfItem(pdfName, uri))
                return PdfModel(pdfListData)
            }
        }
    }
}





class InternetException : Exception()