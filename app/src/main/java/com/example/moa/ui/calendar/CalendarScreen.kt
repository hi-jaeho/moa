
package com.example.moa.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val incomeColor = Color(0xFF1976D2)
private val expenseColor = Color(0xFFD32F2F)

private fun money(amount: Long): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원"

private fun shortMoney(amount: Long): String {
    return when {
        amount >= 10000L -> "${amount / 10000}만"
        amount >= 1000L -> "${amount / 1000}천"
        else -> amount.toString()
    }
}


@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    categoryNames: Map<Long, String>,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Moa",
                    style = MaterialTheme.typography.headlineMedium
                )

                TextButton(onClick = viewModel::goToToday) {
                    Text("오늘")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = viewModel::previousMonth) {
                    Text("〈")
                }

                Text(
                    text = "${state.currentMonth.year}년 " +
                            "${state.currentMonth.monthValue}월",
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = viewModel::nextMonth) {
                    Text("〉")
                }
            }
        }

        item {
            MonthlySummary(state)
        }

        item {
            MonthCalendar(
                month = state.currentMonth,
                selectedDate = state.selectedDate,
                summaries = state.dailySummaries,
                onDateClick = viewModel::selectDate
            )
        }

        item {
            HorizontalDivider()

            Spacer(Modifier.height(12.dp))

            Text(
                text = "${state.selectedDate.monthValue}월 " +
                        "${state.selectedDate.dayOfMonth}일 거래 내역",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (state.isLoading) {
            item {
                CircularProgressIndicator()
            }
        } else if (state.selectedTransactions.isEmpty()) {
            item {
                Text(
                    text = "이 날짜에는 거래 내역이 없습니다.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = state.selectedTransactions,
                key = { it.id }
            ) { transaction ->
                Box(
                    modifier = Modifier.clickable {
                        onTransactionClick(transaction.id)
                    }
                ) {
                    TransactionRow(
                        transaction = transaction,
                        categoryName = categoryNames[transaction.categoryId]
                            ?: "기타"
                    )
                }
            }

        }

        item {
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun MonthlySummary(state: CalendarUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(
                title = "총수입",
                amount = state.income,
                color = incomeColor,
                modifier = Modifier.weight(1f)
            )

            SummaryCard(
                title = "총지출",
                amount = state.expense,
                color = expenseColor,
                modifier = Modifier.weight(1f)
            )
        }

        SummaryCard(
            title = "월간 잔액",
            amount = state.balance,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: Long,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium
            )

            Text(
                text = money(amount),
                color = color,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selectedDate: LocalDate,
    summaries: Map<LocalDate, DaySummary>,
    onDateClick: (LocalDate) -> Unit
) {
    // 일요일을 첫 번째 요일로 사용
    val offset = month.atDay(1).dayOfWeek.value % 7
    val totalDays = month.lengthOfMonth()
    val cellCount = ((offset + totalDays + 6) / 7) * 7

    val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        repeat(cellCount / 7) { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(7) { dayOfWeek ->
                    val index = week * 7 + dayOfWeek
                    val dayNumber = index - offset + 1

                    if (dayNumber in 1..totalDays) {
                        val date = month.atDay(dayNumber)

                        DayCell(
                            date = date,
                            summary = summaries[date],
                            selected = date == selectedDate,
                            onClick = { onDateClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    summary: DaySummary?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = if (selected) {
            BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null
    ) {
        Column(
            modifier = Modifier.padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium
            )

            summary?.let {
                if (it.income > 0L) {
                    Text(
                        text = "+${shortMoney(it.income)}",
                        color = incomeColor,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                if (it.expense > 0L) {
                    Text(
                        text = "-${shortMoney(it.expense)}",
                        color = expenseColor,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionEntity,
    categoryName: String
) {
    val isIncome = transaction.type == TransactionType.INCOME

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = categoryName,
                    fontWeight = FontWeight.Bold
                )

                if (!transaction.memo.isNullOrBlank()) {
                    Text(
                        text = transaction.memo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = (if (isIncome) "+" else "-") +
                        money(transaction.amount),
                color = if (isIncome) incomeColor else expenseColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
