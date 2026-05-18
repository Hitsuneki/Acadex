package com.example.acadex.util

import android.graphics.Color
import android.text.SpannableString
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import com.example.acadex.data.model.DocElement
import com.example.acadex.data.model.SlideData
import com.example.acadex.data.model.SlideElement
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFPictureShape
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.apache.poi.xwpf.usermodel.IBodyElement
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream

object ViewerParser {

    fun parseDocx(file: File): List<DocElement> {
        val elements = mutableListOf<DocElement>()
        FileInputStream(file).use { fis ->
            XWPFDocument(fis).use { doc ->
                for (bodyElement in doc.bodyElements) {
                    when (bodyElement) {
                        is XWPFParagraph -> {
                            val text = bodyElement.text
                            if (text.isBlank()) {
                                elements.add(DocElement.EmptyLine)
                                continue
                            }
                            val style = bodyElement.styleID ?: ""
                            val isHeading = style.contains("Heading", ignoreCase = true) ||
                                    bodyElement.runs.any { run ->
                                        val size = run.fontSizeAsDouble
                                        size != null && size >= 16.0
                                    }
                            
                            if (isHeading) {
                                val level = if (style.contains("Heading1", true)) 1 
                                            else if (style.contains("Heading2", true)) 2 
                                            else 3
                                elements.add(DocElement.Heading(text, level))
                            } else {
                                val spannable = SpannableString(text)
                                var currentIndex = 0
                                for (run in bodyElement.runs) {
                                    val runText = run.text() ?: ""
                                    if (runText.isEmpty()) continue
                                    val start = text.indexOf(runText, currentIndex)
                                    if (start >= 0) {
                                        val end = start + runText.length
                                        if (run.isBold) spannable.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, end, 0)
                                        if (run.isItalic) spannable.setSpan(StyleSpan(android.graphics.Typeface.ITALIC), start, end, 0)
                                        if (run.underline != org.apache.poi.xwpf.usermodel.UnderlinePatterns.NONE) {
                                            spannable.setSpan(UnderlineSpan(), start, end, 0)
                                        }
                                        currentIndex = end
                                    }
                                }
                                elements.add(DocElement.Paragraph(spannable))
                            }
                            
                            for (run in bodyElement.runs) {
                                for (pic in run.embeddedPictures) {
                                    elements.add(DocElement.ImageBlock(pic.pictureData.data))
                                }
                            }
                        }
                        is XWPFTable -> {
                            val rows = bodyElement.rows.map { row ->
                                row.tableCells.map { cell -> cell.text }
                            }
                            elements.add(DocElement.TableBlock(rows))
                        }
                    }
                }
            }
        }
        return elements
    }

    fun parsePptx(file: File): List<SlideData> {
        val slidesData = mutableListOf<SlideData>()
        FileInputStream(file).use { fis ->
            XMLSlideShow(fis).use { ppt ->
                    val pptObj: Any = ppt
                    val pageSizeObj: Any = pptObj.javaClass.getMethod("getPageSize").invoke(pptObj)
                        ?: return@use
                    val pW = (pageSizeObj.javaClass.getMethod("getWidth").invoke(pageSizeObj) as? Double
                        ?: (pageSizeObj.javaClass.getMethod("getWidth").invoke(pageSizeObj) as? Int)?.toDouble()
                        ?: 720.0).toFloat()
                    val pH = (pageSizeObj.javaClass.getMethod("getHeight").invoke(pageSizeObj) as? Double
                        ?: (pageSizeObj.javaClass.getMethod("getHeight").invoke(pageSizeObj) as? Int)?.toDouble()
                        ?: 405.0).toFloat()

                    ppt.slides.forEachIndexed { index, slide ->
                        val slideObj: Any = slide
                        val bgObj: Any? = slideObj.javaClass.getMethod("getBackground").invoke(slideObj)
                        val bgColorObj: Any? = bgObj?.javaClass?.getMethod("getFillColor")?.invoke(bgObj)
                        val colorInt = if (bgColorObj != null) {
                            val r = bgColorObj.javaClass.getMethod("getRed").invoke(bgColorObj) as Int
                            val g = bgColorObj.javaClass.getMethod("getGreen").invoke(bgColorObj) as Int
                            val b = bgColorObj.javaClass.getMethod("getBlue").invoke(bgColorObj) as Int
                            Color.rgb(r, g, b)
                        } else {
                            Color.WHITE
                        }
                        val elements = mutableListOf<SlideElement>()

                        slide.shapes.forEach { shape ->
                            val shapeObj: Any = shape
                            when (shape) {
                                is XSLFTextShape -> {
                                    val text = shape.text
                                    if (text.isNotBlank()) {
                                        val anchorObj: Any = shapeObj.javaClass.getMethod("getAnchor").invoke(shapeObj)
                                            ?: return@forEach
                                        val ax = (anchorObj.javaClass.getMethod("getX").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                        val ay = (anchorObj.javaClass.getMethod("getY").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                        val aw = (anchorObj.javaClass.getMethod("getWidth").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                        val ah = (anchorObj.javaClass.getMethod("getHeight").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                        
                                        val xRatio = ax / pW
                                        val yRatio = ay / pH
                                        val wRatio = aw / pW
                                        val hRatio = ah / pH

                                        val spannable = SpannableString(text)
                                        // Simplified PPTX text formatting parsing to avoid runtime unresolved refs.
                                        elements.add(SlideElement.TextElement(spannable, xRatio, yRatio, wRatio, hRatio))
                                    }
                                }
                                is XSLFPictureShape -> {
                                    val anchorObj: Any = shapeObj.javaClass.getMethod("getAnchor").invoke(shapeObj)
                                        ?: return@forEach
                                    val ax = (anchorObj.javaClass.getMethod("getX").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                    val ay = (anchorObj.javaClass.getMethod("getY").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                    val aw = (anchorObj.javaClass.getMethod("getWidth").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                    val ah = (anchorObj.javaClass.getMethod("getHeight").invoke(anchorObj) as? Double ?: 0.0).toFloat()
                                    
                                    val xRatio = ax / pW
                                    val yRatio = ay / pH
                                    val wRatio = aw / pW
                                    val hRatio = ah / pH
                                    
                                    elements.add(SlideElement.ImageElement(shape.pictureData.data, xRatio, yRatio, wRatio, hRatio))
                                }
                            }
                        }
                        slidesData.add(SlideData(index + 1, colorInt, elements))
                    }
            }
        }
        return slidesData
    }
}
