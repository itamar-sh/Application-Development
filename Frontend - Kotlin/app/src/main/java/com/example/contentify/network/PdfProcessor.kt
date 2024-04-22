package com.example.contentify.network

import okhttp3.RequestBody

interface PdfProcessor {

    suspend fun uploadReq(fileName: String): Result<UploadReq>
    suspend fun uploadPdf(url: String, body: RequestBody?): Result<Unit>
    suspend fun getSummarizedPdf(jobId: String): Result<ProcessedPdfResults>
    suspend fun getStudies(jobId: String): Result<ProcessedPdfResults>
    suspend fun getQuestions(jobId: String): Result<ProcessedPdfResults>

}
class AWSError(error: String) : Exception(error)