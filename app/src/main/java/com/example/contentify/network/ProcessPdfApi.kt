package com.example.contentify.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

interface ProcessPdfApi {

    @POST("/")
    suspend fun uploadReq(
        @Body body: FileNameJson
    ): Response<UploadReqResponse>

    @PUT
    suspend fun uploadPdf(
        @Url url: String,
        @Body body: RequestBody
    ) : Response<Unit>

    @GET
    suspend fun getSummarizedPdf(
        @Url url: String = SUMMARIZED_URL,
        @Query("jobId") jobId: String
    ) : Response<ReqResponse>

    @GET
    suspend fun getQuestions(
        @Url url: String = QUESTIONS_URL,
        @Query("jobId") jobId: String
    ) : Response<ReqResponse>

    @GET
    suspend fun getStudies(
        @Url url: String = STUDIES_URL,
        @Query("jobId") jobId: String
    ) : Response<ReqResponse>

    @GET
    suspend fun getPdf(
        @Url url: String = GET_PDF_URL,
        @Query("num") num: Int
    ) : Response<GetPdfResponse>

    companion object{
        private const val UPLOAD_URL = "https://uit455n663ekp2trteepdsszem0qwzno.lambda-url.us-west-2.on.aws/"
        const val SUMMARIZED_URL =  "https://2lre5rd475xmlof23i3qhoragm0cfvru.lambda-url.us-west-2.on.aws/"
        const val QUESTIONS_URL =  "https://mb3zvq76s5dirhoktgamc72k2m0bkvwz.lambda-url.us-west-2.on.aws//"
        const val STUDIES_URL =  "https://sjlyjn4qjs54jfr6myqr7pc5uu0ccueh.lambda-url.us-west-2.on.aws/"
        const val GET_PDF_URL = "https://d6e3ztgdojevpqc4up4xhukxdu0pjrsv.lambda-url.us-west-2.on.aws/"


        val instance: ProcessPdfApi by lazy {
            val retrofit: Retrofit = createRetrofit()
            retrofit.create(ProcessPdfApi::class.java)
        }

        private fun createRetrofit(): Retrofit {

            // Create converter
            val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

            // Create logger
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)

            // Create client
            val httpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            // Build Retrofit
            return Retrofit.Builder()
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .baseUrl(UPLOAD_URL)
                .client(httpClient)
                .build()
        }
    }
}
