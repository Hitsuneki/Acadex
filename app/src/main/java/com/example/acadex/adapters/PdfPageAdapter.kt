package com.example.acadex.adapters

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.acadex.databinding.ItemPdfPageBinding
import java.io.File

class PdfPageAdapter(private val pdfFile: File) : RecyclerView.Adapter<PdfPageAdapter.VH>() {

    private var renderer: PdfRenderer? = null

    init {
        renderer = PdfRenderer(
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        )
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding.pageImage)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val page = renderer?.openPage(position) ?: return
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        holder.image.setImageBitmap(bitmap)
    }

    override fun getItemCount(): Int = renderer?.pageCount ?: 0

    fun close() {
        renderer?.close()
        renderer = null
    }

    class VH(val image: ImageView) : RecyclerView.ViewHolder(image)
}
