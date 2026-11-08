
package com.example.moa.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import com.example.moa.data.local.entity.FixedExpenseEntity
import com.example.moa.ui.common.ThousandsSeparatorVisualTransformation
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import kotlin.math.absoluteValue

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
    var showFixedExpenseDialog by remember { mutableStateOf(false) }

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

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = viewModel::applyFixedExpenses,
                        enabled = !state.isApplyingFixedExpenses
                    ) {
                        Text(
                            if (state.isApplyingFixedExpenses) {
                                "적용 중"
                            } else {
                                "고정비 적용"
                            }
                        )
                    }

                    TextButton(onClick = { showFixedExpenseDialog = true }) {
                        Text("추가")
                    }
                }
            }
        }

        state.fixedExpenseMessage?.let { message ->
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = viewModel::clearFixedExpenseMessage) {
                            Text("확인")
                        }
                    }
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::goToToday) {
                        Text("오늘")
                    }
                    IconButton(onClick = viewModel::nextMonth) {
                        Text("〉")
                    }
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

    if (showFixedExpenseDialog) {
        FixedExpenseDialog(
            fixedExpenses = state.fixedExpenses,
            onAdd = viewModel::addFixedExpense,
            onDelete = viewModel::deleteFixedExpense,
            onDismiss = { showFixedExpenseDialog = false }
        )
    }
}

@Composable
private fun FixedExpenseDialog(
    fixedExpenses: List<FixedExpenseEntity>,
    onAdd: (String, Int, Long) -> Unit,
    onDelete: (FixedExpenseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var day by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("고정비 추가") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "매달 반복되는 항목을 정해 두세요. 적용 버튼을 눌러야 " +
                        "선택한 달의 지출로 입력됩니다."
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = { Text("항목 (예: 월세, 넷플릭스)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = day,
                    onValueChange = {
                        day = it.filter(Char::isDigit).take(2)
                        error = null
                    },
                    label = { Text("매월 며칠") },
                    suffix = { Text("일") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter(Char::isDigit)
                        error = null
                    },
                    label = { Text("금액") },
                    suffix = { Text("원") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    visualTransformation = ThousandsSeparatorVisualTransformation,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "31일이 없는 달에는 그 달의 마지막 날로 들어갑니다. " +
                        "카테고리는 ‘고정비’로 자동 지정됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Button(
                    onClick = {
                        val dayValue = day.toIntOrNull()
                        val amountValue = amount.toLongOrNull()

                        error = when {
                            name.isBlank() -> "항목을 입력해 주세요."
                            dayValue == null || dayValue !in 1..31 ->
                                "날짜는 1일부터 31일 사이로 입력해 주세요."
                            amountValue == null || amountValue <= 0L ->
                                "0원보다 큰 금액을 입력해 주세요."
                            else -> null
                        }

                        if (error == null && dayValue != null && amountValue != null) {
                            onAdd(name, dayValue, amountValue)
                            name = ""
                            day = ""
                            amount = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("고정비 항목 저장")
                }

                HorizontalDivider()

                Text(
                    "저장된 고정비",
                    style = MaterialTheme.typography.titleMedium
                )

                if (fixedExpenses.isEmpty()) {
                    Text(
                        "아직 저장된 고정비가 없습니다.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    fixedExpenses.forEach { fixedExpense ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    fixedExpense.name,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "매월 ${fixedExpense.dayOfMonth}일 · " +
                                        money(fixedExpense.amount),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = { onDelete(fixedExpense) }) {
                                Text("삭제")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PreviousMonthExpenseCard(
                currentExpense = state.expense,
                previousExpense = state.previousMonthExpense,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            SummaryCard(
                title = "월간 잔액",
                amount = state.balance,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun PreviousMonthExpenseCard(
    currentExpense: Long,
    previousExpense: Long,
    modifier: Modifier = Modifier
) {
    val difference = currentExpense - previousExpense
    val comparisonText = when {
        difference > 0L -> "${money(difference.absoluteValue)} 증가"
        difference < 0L -> "${money(difference.absoluteValue)} 감소"
        else -> "변동 없음"
    }
    val comparisonColor = when {
        difference > 0L -> expenseColor
        difference < 0L -> incomeColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "지난달 대비 지출",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = comparisonText,
                color = comparisonColor,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "지난달 ${money(previousExpense)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall
            )
        }
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
