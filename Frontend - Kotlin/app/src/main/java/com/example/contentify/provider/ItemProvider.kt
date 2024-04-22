package com.example.contentify.provider

import com.example.contentify.PdfModel

interface ItemProvider {

    suspend fun getAllItems(): Result<PdfModel>

    fun addItem(uri: String, pdfName: String): PdfModel


}