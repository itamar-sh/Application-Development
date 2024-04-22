package com.example.contentify.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


sealed class ProcessedPdfResults {
    data class Processing(
        val status: String
    ) : ProcessedPdfResults()

    data class Ready(
        val url: String
    ) : ProcessedPdfResults()
}

data class UploadReq(
    val jobId: String,
    val url: String
)

data class GetPdfReq(
    val jobIds: Array<String>,
    val fileNames : Array<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GetPdfReq

        if (!jobIds.contentEquals(other.jobIds)) return false
        if (!fileNames.contentEquals(other.fileNames)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = jobIds.contentHashCode()
        result = 31 * result + fileNames.contentHashCode()
        return result
    }
}

@JsonClass(generateAdapter = true)
data class UploadReqResponse(
    @Json(name = "jobId")
    val jobId: String,
    @Json(name = "url")
    val url: String
)

@JsonClass(generateAdapter = true)
data class FileNameJson(
    @Json(name = "fileName")
    val fileName: String
)

@JsonClass(generateAdapter = true)
data class ReqResponse(
    @Json(name = "status")
    val status: String?,
    @Json(name = "url")
    val url: String?
)

@JsonClass(generateAdapter = true)
data class GetPdfResponse(
    @Json(name = "jobs")
    val jobs: List<String>,
    @Json(name = "files_name")
    val files_name: List<String>
)
