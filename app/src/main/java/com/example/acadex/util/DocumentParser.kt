package com.example.acadex.util

import android.content.Context
import com.example.acadex.data.model.FileType
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.extractor.WordExtractor
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream

object DocumentParser {

    fun extractText(file: File, fileType: FileType): String {
        return when (fileType) {
            FileType.DOCX -> extractDocxText(file)
            FileType.PPTX -> extractPptxText(file)
            FileType.DOC -> extractDocText(file)
            FileType.TXT -> extractTxtText(file)
            else -> ""
        }
    }

    private fun extractDocxText(file: File): String {
        return try {
            FileInputStream(file).use { fis ->
                XWPFDocument(fis).use { doc ->
                    val text = StringBuilder()
                    doc.paragraphs.forEach { para ->
                        text.append(para.text).append("\n")
                    }
                    text.toString()
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractPptxText(file: File): String {
        return try {
            FileInputStream(file).use { fis ->
                XMLSlideShow(fis).use { ppt ->
                    val text = StringBuilder()
                    ppt.slides.forEach { slide: XSLFSlide ->
                        slide.shapes.forEach { shape ->
                            if (shape is org.apache.poi.xslf.usermodel.XSLFTextShape) {
                                shape.textParagraphs.forEach { para ->
                                    text.append(para.text).append("\n")
                                }
                            }
                        }
                        text.append("\n--- Slide ---\n\n")
                    }
                    text.toString()
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractDocText(file: File): String {
        return try {
            FileInputStream(file).use { fis ->
                HWPFDocument(fis).use { doc ->
                    WordExtractor(doc).text
                }
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun extractTxtText(file: File): String {
        return try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
