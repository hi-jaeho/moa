package com.example.moa.ui.common

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object ThousandsSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val originalToTransformed = IntArray(raw.length + 1)
        val formatted = buildString {
            originalToTransformed[0] = 0

            raw.forEachIndexed { index, character ->
                append(character)

                val remainingDigits = raw.length - index - 1
                if (remainingDigits > 0 && remainingDigits % 3 == 0) {
                    append(',')
                }

                originalToTransformed[index + 1] = length
            }
        }

        val transformedToOriginal = IntArray(formatted.length + 1)
        var originalOffset = 0
        for (offset in 1..formatted.length) {
            if (formatted[offset - 1] != ',') {
                originalOffset += 1
            }
            transformedToOriginal[offset] = originalOffset
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return originalToTransformed[offset.coerceIn(0, raw.length)]
            }

            override fun transformedToOriginal(offset: Int): Int {
                return transformedToOriginal[offset.coerceIn(0, formatted.length)]
            }
        }

        return TransformedText(
            text = AnnotatedString(formatted),
            offsetMapping = offsetMapping
        )
    }
}
