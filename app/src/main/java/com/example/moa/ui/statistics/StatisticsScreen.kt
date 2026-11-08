
package com.example.moa.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.moa.ui.common.ThousandsSeparatorVisualTransformation
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
    var showInitialBalanceDialog by remember { mutableStateOf(false) }
    var initialBalanceInput by remember { mutableStateOf("") }

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
            FinancialOverviewSection(
                state = state,
                onEditInitialBalance = {
                    initialBalanceInput = state.initialBalance.toString()
                    showInitialBalanceDialog = true
                }
            )

            HorizontalDivider()

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

    if (showInitialBalanceDialog) {
        AlertDialog(
            onDismissRequest = { showInitialBalanceDialog = false },
            title = { Text("시작 보유금액 설정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Moa에 수입·지출을 기록하기 전에 이미 가지고 있던 " +
                            "현금과 통장 잔액의 합계를 입력해 주세요."
                    )
                    OutlinedTextField(
                        value = initialBalanceInput,
                        onValueChange = { input ->
                            initialBalanceInput = input.filter(Char::isDigit)
                        },
                        label = { Text("시작 보유금액") },
                        suffix = { Text("원") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        visualTransformation = ThousandsSeparatorVisualTransformation,
                        singleLine = true
                    )
                    Text(
                        "예: 시작할 때 100만 원이 있었다면 1,000,000원을 입력합니다. " +
                            "이후 등록한 수입과 지출은 자동으로 반영됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = initialBalanceInput.toLongOrNull() != null,
                    onClick = {
                        initialBalanceInput.toLongOrNull()?.let {
                            viewModel.saveInitialBalance(it)
                            showInitialBalanceDialog = false
                        }
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInitialBalanceDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
private fun FinancialOverviewSection(
    state: StatisticsUiState,
    onEditInitialBalance: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "전체 재정 현황",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "${state.month.year}년 ${state.month.monthValue}월 말 예상 보유금액",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatMoney(state.estimatedBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (state.estimatedBalance < 0L) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Text(
                "시작 ${formatMoney(state.initialBalance)} + " +
                    "누적 수입 ${formatMoney(state.cumulativeIncome)} - " +
                    "누적 지출 ${formatMoney(state.cumulativeExpense)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(onClick = onEditInitialBalance) {
                Text("시작 보유금액 수정")
            }
        }
    }
}

@Composable
private fun SummarySection(state: StatisticsUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryItem("이번 달 수입", state.income, Color(0xFF1976D2))
        SummaryItem("이번 달 지출", state.expense, Color(0xFFD32F2F))
        SummaryItem(
            "이번 달 증감",
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
