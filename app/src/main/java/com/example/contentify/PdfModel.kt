package com.example.contentify

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Model class that represents the app.
 * [PdfItems] - list of all the pdf items to be presented
 */
data class PdfModel(
    val pdfItems: List<PdfItem>,
)

/**
 * The class representing a pdf item.
 */
@Parcelize
data class PdfItem(val name: String, val uri: String?=null, var job_id: String?=null)
    : Parcelable
