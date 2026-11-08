
package com.example.moa.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val chartColors = listOf(
    Color(0xFF4285F4),
    Color(0xFFFFA726),
    Color(0xFF66BB6A),
    Color(0xFFAB47BC),
    Color(0xFF26A69A),
    Color(0xFFEF5350),
    Color(0xFF78909C)
)

private fun formatMoney(value: Long): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "월별 통계",
            style = MaterialTheme.typography.headlineMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = viewModel::previousMonth) {
                Text("〈")
            }

            Text(
                "${state.month.year}년 ${state.month.monthValue}월",
                style = MaterialTheme.typography.titleLarge
            )

            TextButton(onClick = viewModel::nextMonth) {
                Text("〉")
            }
        }

        TextButton(onClick = viewModel::goToCurrentMonth) {
            Text("이번 달로 이동")
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        } else {
            SummarySection(state)

            HorizontalDivider()

            Text(
                "카테고리별 지출",
                style = MaterialTheme.typography.titleLarge
            )

            if (state.categories.isEmpty()) {
                Text("이번 달 지출 내역이 없습니다.")
            } else {
                ExpenseDonutChart(state.categories)

                state.categories.forEachIndexed { index, category ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = chartColors[index % chartColors.size],
                                modifier = Modifier.size(12.dp),
                                shape = MaterialTheme.shapes.small
                            ) {}

                            Text(category.name)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatMoney(category.amount),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                String.format(
                                    Locale.KOREA,
                                    "%.1f%%",
                                    category.ratio * 100f
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                "최근 6개월 수입·지출",
                style = MaterialTheme.typography.titleLarge
            )

            MonthlyBarChart(state.trends)

            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("■ 수입", color = Color(0xFF4285F4))
                Text("■ 지출", color = Color(0xFFEF5350))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummarySection(state: StatisticsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryItem("총수입", state.income, Color(0xFF1976D2))
        SummaryItem("총지출", state.expense, Color(0xFFD32F2F))
        SummaryItem(
            "잔액",
            state.balance,
            MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SummaryItem(
    title: String,
    amount: Long,
    color: Color
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title)
            Text(
                formatMoney(amount),
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ExpenseDonutChart(
    categories: List<CategoryExpense>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val stroke = 34.dp.toPx()
            val diameter = size.minDimension - stroke
            val topLeft = Offset(stroke / 2, stroke / 2)
            val arcSize = Size(diameter, diameter)

            var startAngle = -90f

            categories.forEachIndexed { index, category ->
                val sweep = category.ratio * 360f

                drawArc(
                    color = chartColors[index % chartColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt)
                )

                startAngle += sweep
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("지출 비중")
            Text(
                "${categories.size}개 분류",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun MonthlyBarChart(
    trends: List<MonthlyTrend>
) {
    if (trends.isEmpty()) {
        Text("통계 데이터가 없습니다.")
        return
    }

    val maxValue = trends.maxOf {
        maxOf(it.income, it.expense)
    }.coerceAtLeast(1L)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val barGroupWidth = size.width / trends.size
            val barWidth = barGroupWidth * 0.28f
            val chartHeight = size.height

            trends.forEachIndexed { index, trend ->
                val centerX = barGroupWidth * (index + 0.5f)

                val incomeHeight =
                    chartHeight * (trend.income.toFloat() / maxValue)

                val expenseHeight =
                    chartHeight * (trend.expense.toFloat() / maxValue)

                drawRect(
                    color = Color(0xFF4285F4),
                    topLeft = Offset(
                        centerX - barWidth,
                        chartHeight - incomeHeight
                    ),
                    size = Size(barWidth, incomeHeight)
                )

                drawRect(
                    color = Color(0xFFEF5350),
                    topLeft = Offset(
                        centerX,
                        chartHeight - expenseHeight
                    ),
                    size = Size(barWidth, expenseHeight)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            trends.forEach { trend ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${trend.month.monthValue}월",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}
