package com.example.moa.ui.common

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsSeparatorVisualTransformationTest {

    @Test
    fun filter_formatsDigitsWithoutChangingOriginalValue() {
        val result = ThousandsSeparatorVisualTransformation.filter(
            AnnotatedString("123456")
        )

        assertEquals("123,456", result.text.text)
    }

    @Test
    fun filter_keepsCursorAtEndAfterCommaIsInserted() {
        val result = ThousandsSeparatorVisualTransformation.filter(
            AnnotatedString("1234")
        )

        assertEquals(5, result.offsetMapping.originalToTransformed(4))
        assertEquals(4, result.offsetMapping.transformedToOriginal(5))
    }

    @Test
    fun filter_mapsCursorAcrossMiddleSeparator() {
        val result = ThousandsSeparatorVisualTransformation.filter(
            AnnotatedString("123456")
        )

        assertEquals(4, result.offsetMapping.originalToTransformed(3))
        assertEquals(3, result.offsetMapping.transformedToOriginal(4))
    }
}
