package com.example.acadex.data.model

sealed class DocElement {
    data class Heading(val text: String, val level: Int) : DocElement()
    data class Paragraph(val text: CharSequence) : DocElement()
    data class TableBlock(val rows: List<List<String>>) : DocElement()
    data class ImageBlock(val bitmapData: ByteArray) : DocElement()
    data object Divider : DocElement()
    data object EmptyLine : DocElement()
}

sealed class SlideElement {
    data class TextElement(val text: CharSequence, val xRatio: Float, val yRatio: Float, val widthRatio: Float, val heightRatio: Float) : SlideElement()
    data class ImageElement(val bytes: ByteArray, val xRatio: Float, val yRatio: Float, val widthRatio: Float, val heightRatio: Float) : SlideElement()
}

data class SlideData(val slideNumber: Int, val backgroundColor: Int, val elements: List<SlideElement>)
