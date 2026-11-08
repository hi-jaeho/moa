
package com.example.moa.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

@Composable
fun ResponsiveLayout(
    modifier: Modifier = Modifier,
    compactContent: @Composable () -> Unit,
    expandedContent: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp

    Box(modifier = modifier.fillMaxSize()) {
        if (width >= 600) {
            expandedContent()
        } else {
            compactContent()
        }
    }
}
