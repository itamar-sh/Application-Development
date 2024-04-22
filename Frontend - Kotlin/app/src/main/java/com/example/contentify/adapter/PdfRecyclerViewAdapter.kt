package com.example.contentify.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.contentify.PdfItem
import com.example.contentify.R

class PdfRecyclerViewAdapter (val onItemClick: (PdfItem) -> Unit):
    ListAdapter<PdfItem, PdfRecyclerViewAdapter.PdfRecyclerViewHolder>(ItemDiffUtil()){

    inner class PdfRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val pdfTextView: TextView = itemView.findViewById(R.id.text_view)

        fun onBind(item: PdfItem) {
            pdfTextView.text = item.name
            pdfTextView.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfRecyclerViewHolder {
        val v: View = LayoutInflater.from(parent.context).inflate(R.layout.pdf_item, parent, false)
        return PdfRecyclerViewHolder(v)
    }

    override fun onBindViewHolder(holder: PdfRecyclerViewHolder, position: Int) {
        val item = getItem(position)
        holder.onBind(item)
    }

    class ItemDiffUtil : DiffUtil.ItemCallback<PdfItem>() {
        override fun areItemsTheSame(oldItem: PdfItem, newItem: PdfItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: PdfItem, newItem: PdfItem): Boolean {
            return oldItem == newItem
        }
    }
}
