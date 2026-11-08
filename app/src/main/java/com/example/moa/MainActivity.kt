
package com.example.moa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moa.ui.backup.BackupScreen
import com.example.moa.ui.backup.BackupViewModel
import com.example.moa.ui.calendar.CalendarScreen
import com.example.moa.ui.calendar.CalendarViewModel
import com.example.moa.ui.category.CategoryScreen
import com.example.moa.ui.category.CategoryViewModel
import com.example.moa.ui.statistics.StatisticsScreen
import com.example.moa.ui.statistics.StatisticsViewModel
import com.example.moa.ui.transaction.TransactionEditScreen
import com.example.moa.ui.transaction.TransactionEditViewModel
import com.example.moa.ui.transaction.TransactionFormScreen
import com.example.moa.ui.transaction.TransactionViewModel
import com.example.moa.ui.theme.MoaTheme
import com.example.moa.ui.common.ResponsiveLayout
import androidx.compose.foundation.layout.Row
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.moa.ui.settings.ThemeViewModel
import com.example.moa.ui.settings.SettingsScreen
import com.example.moa.ui.theme.MoaTheme
import com.example.moa.ui.security.PinViewModel
import com.example.moa.ui.security.PinScreen
import com.example.moa.ui.security.PinStatus
import androidx.fragment.app.FragmentActivity
import com.example.moa.ui.security.BiometricAuthManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.moa.ui.budget.BudgetScreen
import com.example.moa.ui.budget.BudgetViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as MoaApplication

        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>
            ): T = when (modelClass) {
                TransactionViewModel::class.java ->
                    TransactionViewModel(app) as T
                TransactionEditViewModel::class.java ->
                    TransactionEditViewModel(app) as T
                CalendarViewModel::class.java ->
                    CalendarViewModel(app) as T
                StatisticsViewModel::class.java ->
                    StatisticsViewModel(app) as T
                BudgetViewModel::class.java ->
                    BudgetViewModel(app) as T
                CategoryViewModel::class.java ->
                    CategoryViewModel(app) as T
                BackupViewModel::class.java ->
                    BackupViewModel(app) as T
                else -> throw IllegalArgumentException(
                    "Unknown ViewModel: ${modelClass.name}"
                )
            }
        }

        val provider = ViewModelProvider(this, factory)

        val transactionVM = provider[TransactionViewModel::class.java]
        val editVM = provider[TransactionEditViewModel::class.java]
        val calendarVM = provider[CalendarViewModel::class.java]
        val statisticsVM = provider[StatisticsViewModel::class.java]
        val budgetVM = provider[BudgetViewModel::class.java]
        val categoryVM = provider[CategoryViewModel::class.java]
        val backupVM = provider[BackupViewModel::class.java]
        val biometricAuthManager = BiometricAuthManager(this)

        setContent {
            val themeViewModel: ThemeViewModel = viewModel()
            val themeMode by themeViewModel.themeMode.collectAsState()
            val pinViewModel: PinViewModel = viewModel()
            val pinStatus by pinViewModel.status.collectAsState()
            val pinError by pinViewModel.error.collectAsState()
            val securitySettings by pinViewModel.securitySettings.collectAsState()
            val securityMessage by pinViewModel.securityMessage.collectAsState()

            val lifecycleOwner = LocalLifecycleOwner.current
            val lockoutUntil by pinViewModel.lockoutUntil.collectAsState()
            val pinChangeSuccess by pinViewModel.pinChangeSuccess.collectAsState()

            DisposableEffect(lifecycleOwner, pinViewModel) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        pinViewModel.lockIfEnabled()
                    }
                }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            MoaTheme(themeMode = themeMode) {
                // 지문 인증 자동 실행

                LaunchedEffect(
                    pinStatus,
                    securitySettings.biometricEnabled
                ) {
                    if (
                        pinStatus == PinStatus.LOCKED &&
                        securitySettings.biometricEnabled &&
                        biometricAuthManager.isAvailable()
                    ) {
                        biometricAuthManager.authenticate(
                            onSuccess = {
                                pinViewModel.unlockWithBiometric()
                            },
                            onError = {
                                // PIN 입력 유지
                            }
                        )
                    }
                }


                when (pinStatus) {
                    PinStatus.LOADING -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    PinStatus.SETUP,
                    PinStatus.CONFIRM,
                    PinStatus.LOCKED -> {
                        PinScreen(
                            status = pinStatus,
                            error = pinError,
                            onSubmit = pinViewModel::submitPin,
                            lockoutUntil = lockoutUntil,
                            biometricAvailable =
                                securitySettings.biometricEnabled &&
                                        biometricAuthManager.isAvailable(),
                            onBiometricClick = {
                                biometricAuthManager.authenticate(
                                    onSuccess = {
                                        pinViewModel.unlockWithBiometric()
                                    },
                                    onError = {
                                        // 취소 또는 실패 시 PIN 입력 화면 유지
                                    }
                                )
                            }
                        )

                    }

                    PinStatus.UNLOCKED -> {
                        var screen by rememberSaveable {
                            mutableStateOf("MAIN")
                        }

                        var selectedTab by rememberSaveable {
                            mutableIntStateOf(0)
                        }

                        var selectedTransactionId by rememberSaveable {
                            mutableLongStateOf(-1L)
                        }

                        val categories by remember(app) {
                            app.categoryRepository.getAll()
                        }.collectAsState(initial = emptyList())

                        val categoryNames = remember(categories) {
                            categories.associate { it.id to it.name }
                        }

                        BackHandler(enabled = screen != "MAIN") {
                            screen = "MAIN"
                        }

                        when (screen) {
                            "MAIN" -> {
                                Scaffold(
                                    bottomBar = {
                                        NavigationBar {
                                            val tabs = listOf(
                                                "달력",
                                                "통계",
                                                "예산",
                                                "카테고리",
                                                "백업",
                                                "설정"
                                            )

                                            val symbols = listOf(
                                                "▦", "▥", "₩", "☷", "⇩", "⚙"
                                            )

                                            tabs.forEachIndexed { index, title ->
                                                NavigationBarItem(
                                                    selected = selectedTab == index,
                                                    onClick = { selectedTab = index },
                                                    icon = { Text(symbols[index]) },
                                                    label = { Text(title) }
                                                )
                                            }
                                        }
                                    },
                                    floatingActionButton = {
                                        if (selectedTab == 0) {
                                            FloatingActionButton(
                                                onClick = { screen = "ADD" }
                                            ) {
                                                Text("+")
                                            }
                                        }
                                    }
                                ) { padding ->
                                    when (selectedTab) {

                                        0 -> ResponsiveLayout(
                                            modifier = Modifier.padding(padding),
                                            compactContent = {
                                                CalendarScreen(
                                                    viewModel = calendarVM,
                                                    categoryNames = categoryNames,
                                                    onTransactionClick = { id ->
                                                        selectedTransactionId = id
                                                        screen = "EDIT"
                                                    }
                                                )
                                            },
                                            expandedContent = {
                                                Row(
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        CalendarScreen(
                                                            viewModel = calendarVM,
                                                            categoryNames = categoryNames,
                                                            onTransactionClick = { id ->
                                                                selectedTransactionId = id
                                                                screen = "EDIT"
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        )



                                        1 -> StatisticsScreen(
                                            viewModel = statisticsVM,
                                            modifier = Modifier.padding(padding)
                                        )

                                        2 -> Box(
                                            modifier = Modifier.padding(padding)
                                        ) {
                                            BudgetScreen(
                                                viewModel = budgetVM
                                            )
                                        }

                                        3 -> CategoryScreen(
                                            viewModel = categoryVM,
                                            modifier = Modifier.padding(padding)
                                        )

                                        4 -> BackupScreen(
                                            viewModel = backupVM,
                                            modifier = Modifier.padding(padding)
                                        )

                                        5 -> SettingsScreen(
                                            themeMode = themeMode,
                                            onThemeChange = themeViewModel::setThemeMode,
                                            securitySettings = securitySettings,
                                            onLockChange = pinViewModel::setLockEnabled,
                                            onBiometricChange = pinViewModel::setBiometricEnabled,
                                            biometricAvailable = biometricAuthManager.isAvailable(),
                                            onPinChange = pinViewModel::updatePin,
                                            securityMessage = securityMessage,
                                            pinChangeSuccess = pinChangeSuccess,
                                            onResetPinChangeSuccess =
                                                pinViewModel::resetPinChangeSuccess
                                        )

                                    }
                                }
                            }

                            "ADD" -> {
                                Scaffold(
                                    topBar = {
                                        TextButton(
                                            onClick = { screen = "MAIN" }
                                        ) {
                                            Text("〈 돌아가기")
                                        }
                                    }
                                ) { padding ->
                                    Box(modifier = Modifier.padding(padding)) {
                                        TransactionFormScreen(
                                            viewModel = transactionVM
                                        )
                                    }
                                }
                            }

                            "EDIT" -> {
                                if (selectedTransactionId > 0L) {
                                    TransactionEditScreen(
                                        viewModel = editVM,
                                        transactionId = selectedTransactionId,
                                        onBack = { screen = "MAIN" },
                                        onFinished = { date ->
                                            calendarVM.selectDate(date)
                                            screen = "MAIN"
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
                    }
                }
            }

