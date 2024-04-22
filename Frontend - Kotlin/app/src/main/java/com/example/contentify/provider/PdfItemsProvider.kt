package com.example.contentify.provider

import com.example.contentify.PdfItem
import com.example.contentify.PdfModel
import com.example.contentify.network.AWSError
import com.example.contentify.network.ProcessPdfApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class PdfItemsProvider(
) : ItemProvider
{
    private val processPdfApi = ProcessPdfApi.instance
    private val pdfList = mutableListOf<PdfItem>()
    private val pdfUri = mutableListOf<String>()

    override suspend fun getAllItems(): Result<PdfModel> {
        var responseStatus = false
            if (pdfList.size == 0) {
                val pdfResponse = processPdfApi.getPdf(num = 10)
                when {
                    pdfResponse.isSuccessful -> {
                        responseStatus = true
                        val jobs = pdfResponse.body()?.jobs
                        val filesName = pdfResponse.body()?.files_name
                        if (filesName != null) {
                            for (i in filesName.indices) {
                                pdfList.add(PdfItem(filesName[i], job_id = jobs?.get(i)))
                            }
                        }
                    }
                }
            }
            else {
                responseStatus = true
            }
        return when {
            responseStatus -> Result.success(PdfModel(pdfList))
            else -> Result.failure(AWSError("Error: can't get old pdf"))
        }

    }

    override fun addItem(uri: String, pdfName: String): PdfModel {
        if (!pdfUri.contains(uri)){
                pdfUri.add(uri)
                pdfList.add(PdfItem(pdfName, uri))
            }
        return PdfModel(pdfList)
    }
}