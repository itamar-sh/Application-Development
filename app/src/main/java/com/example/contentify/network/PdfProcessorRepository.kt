package com.example.contentify.network

import okhttp3.RequestBody
import retrofit2.Response

class PdfProcessorRepository(
    private val processPdfApi: ProcessPdfApi,
) : PdfProcessor {


    override suspend fun uploadReq(fileName: String): Result<UploadReq> {
        val file = FileNameJson(fileName)
        val response = processPdfApi.uploadReq(file)
        return when {
            response.isSuccessful && response.body() != null -> {
                val uploadReqResponse = response.body()!!
                Result.success(
                    UploadReq(
                        jobId = uploadReqResponse.jobId,
                        url = uploadReqResponse.url
                    )
                )
            }
            else -> Result.failure(AWSError("Error: Could not request upload"))
        }
    }

    override suspend fun uploadPdf(url: String, body: RequestBody?): Result<Unit> {
        val response = processPdfApi.uploadPdf(url, body!!)
        return when {
            response.isSuccessful -> Result.success(Unit)
            else -> Result.failure(AWSError("Error: Could not upload pdf"))
        }
    }

    override suspend fun getSummarizedPdf(jobId: String): Result<ProcessedPdfResults> {
        val response = processPdfApi.getSummarizedPdf(jobId = jobId)
        return getResponse(response)
    }


    override suspend fun getStudies(jobId: String): Result<ProcessedPdfResults> {
        val response = processPdfApi.getStudies(jobId = jobId)
        return getResponse(response)
    }

    override suspend fun getQuestions(jobId: String): Result<ProcessedPdfResults> {
        val response = processPdfApi.getQuestions(jobId = jobId)
        return getResponse(response)
    }

    private fun getResponse(response:Response<ReqResponse>): Result<ProcessedPdfResults> {
        return when {
            response.isSuccessful && response.body() != null -> {
                val body: ReqResponse = response.body()!!
                when (body.url) {
                    null -> when (body.status) {
                        null -> Result.failure(AWSError("Error: can't get pdf processing status"))
                        else ->
                            Result.success(ProcessedPdfResults.Processing(status = body.status))
                    }
                    else -> Result.success(ProcessedPdfResults.Ready(url = body.url))
                }
            }
            else -> Result.failure(AWSError("Error: can't get pdf url"))
        }
    }
}

