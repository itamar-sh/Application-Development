package com.example.contentify

import com.example.contentify.network.*
import okhttp3.RequestBody

class FakePdfProcessor : PdfProcessor
{
    var isSuccessful = false
    private var num = 0
    private var myUrl: String? = "url"
    private var myStatus = "created"
    var hasInternet = true

    override suspend fun uploadReq(fileName: String): Result<UploadReq> {
        if (!hasInternet) {
            throw InternetException()
        }
        return when {
            isSuccessful -> {
                num += 1
                Result.success(UploadReq(num.toString(),fileName))
            }
            else ->
                Result.failure(AWSError("Error: Could not request upload"))
        }
    }

    override suspend fun uploadPdf(url: String, body: RequestBody?): Result<Unit> {
        if (!hasInternet) {
            throw InternetException()
        }
        return when {
            isSuccessful -> Result.success(Unit)
            else -> Result.failure(AWSError("Error: Could not upload pdf"))
        }
    }

    override suspend fun getSummarizedPdf(jobId: String): Result<ProcessedPdfResults> {
        return getResponse()
    }

    override suspend fun getStudies(jobId: String): Result<ProcessedPdfResults> {
        return getResponse()
    }

    override suspend fun getQuestions(jobId: String): Result<ProcessedPdfResults> {
        return getResponse()
    }

    fun setUrlAndStatus(url: String?, status: String){
        myUrl = url
        myStatus = status
    }

     private fun getResponse(): Result<ProcessedPdfResults> {
         if (!hasInternet) {
             throw InternetException()
         }
         return when {
            isSuccessful  -> {
                when (myUrl) {
                    null -> Result.success(ProcessedPdfResults.Processing(status = myStatus))
                    else -> Result.success(ProcessedPdfResults.Ready(url = myUrl!!))
                }
            }
             else -> Result.failure(AWSError("Error: can't get pdf url"))
        }
    }
}