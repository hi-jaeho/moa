
package com.example.moa.data.backup

import androidx.room.withTransaction
import com.example.moa.data.local.AppDatabase
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.FinancialProfileEntity
import com.example.moa.data.local.entity.FixedExpenseEntity
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import com.example.moa.data.repository.FixedExpenseRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth

data class BackupData(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val financialProfile: FinancialProfileEntity? = null,
    val fixedExpenses: List<FixedExpenseEntity> = emptyList()
)

class BackupManager(
    private val database: AppDatabase
) {
    suspend fun exportJson(): String {
        val categories = database.categoryDao().getAllForBackup()
        val transactions = database.transactionDao().getAllForBackup()
        val financialProfile = database.financialProfileDao().getForBackup()
        val fixedExpenses = database.fixedExpenseDao().getAll()

        val root = JSONObject()
        root.put("format", "moa-backup")
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val categoryArray = JSONArray()

        categories.forEach { category ->
            categoryArray.put(
                JSONObject().apply {
                    put("id", category.id)
                    put("name", category.name)
                    put("type", category.type.name)
                    put("sortOrder", category.sortOrder)
                    put("iconName", category.iconName ?: JSONObject.NULL)
                }
            )
        }

        val transactionArray = JSONArray()

        transactions.forEach { transaction ->
            transactionArray.put(
                JSONObject().apply {
                    put("id", transaction.id)
                    put("type", transaction.type.name)
                    put("amount", transaction.amount)
                    put("date", transaction.date.toString())
                    put("categoryId", transaction.categoryId)
                    put("memo", transaction.memo ?: JSONObject.NULL)
                    put(
                        "fixedExpenseId",
                        transaction.fixedExpenseId ?: JSONObject.NULL
                    )
                    put(
                        "fixedExpenseYearMonth",
                        transaction.fixedExpenseYearMonth ?: JSONObject.NULL
                    )
                    put("createdAt", transaction.createdAt)
                    put("updatedAt", transaction.updatedAt)
                }
            )
        }

        root.put("categories", categoryArray)
        root.put("transactions", transactionArray)
        root.put(
            "financialProfile",
            financialProfile?.let { profile ->
                JSONObject().apply {
                    put("initialBalance", profile.initialBalance)
                    put("updatedAt", profile.updatedAt)
                }
            } ?: JSONObject.NULL
        )

        val fixedExpenseArray = JSONArray()
        fixedExpenses.forEach { fixedExpense ->
            fixedExpenseArray.put(
                JSONObject().apply {
                    put("id", fixedExpense.id)
                    put("name", fixedExpense.name)
                    put("dayOfMonth", fixedExpense.dayOfMonth)
                    put("amount", fixedExpense.amount)
                    put("createdAt", fixedExpense.createdAt)
                }
            )
        }
        root.put("fixedExpenses", fixedExpenseArray)

        return root.toString(2)
    }

    fun parseAndValidate(json: String): BackupData {
        val root = JSONObject(json)

        require(root.getString("format") == "moa-backup") {
            "Moa 백업 파일이 아닙니다."
        }

        require(root.getInt("version") == 1) {
            "지원하지 않는 백업 버전입니다."
        }

        val categoryArray = root.getJSONArray("categories")
        val transactionArray = root.getJSONArray("transactions")

        val categories = mutableListOf<CategoryEntity>()
        val transactions = mutableListOf<TransactionEntity>()


        for (i in 0 until categoryArray.length()) {
            val item = categoryArray.getJSONObject(i)

            val category = CategoryEntity(
                id = item.getLong("id"),
                name = requireNotNull(item.getString("name")) {
                    "카테고리 이름이 없습니다."
                },
                type = TransactionType.valueOf(
                    requireNotNull(item.getString("type")) {
                        "카테고리 유형이 없습니다."
                    }
                ),

                iconName = if (item.isNull("iconName")) {
                    "category"
                } else {
                    item.getString("iconName") ?: "category"
                },

                sortOrder = item.getInt("sortOrder")
            )

            require(category.id > 0L && category.name.isNotBlank()) {
                "잘못된 카테고리 데이터입니다."
            }

            categories.add(category)
        }


        val categoryIds = categories.map { it.id }.toSet()

        require(categoryIds.size == categories.size) {
            "중복된 카테고리 ID가 있습니다."
        }

        for (i in 0 until transactionArray.length()) {
            val item = transactionArray.getJSONObject(i)

            val transaction = TransactionEntity(
                id = item.getLong("id"),
                type = TransactionType.valueOf(item.getString("type")),
                amount = item.getLong("amount"),
                date = LocalDate.parse(item.getString("date")),
                categoryId = item.getLong("categoryId"),
                memo = item.optString("memo")
                    .takeIf { !item.isNull("memo") },
                fixedExpenseId = item.optLong("fixedExpenseId")
                    .takeIf {
                        item.has("fixedExpenseId") &&
                            !item.isNull("fixedExpenseId")
                    },
                fixedExpenseYearMonth = item.optString("fixedExpenseYearMonth")
                    .takeIf {
                        item.has("fixedExpenseYearMonth") &&
                            !item.isNull("fixedExpenseYearMonth")
                    },
                createdAt = item.getLong("createdAt"),
                updatedAt = item.getLong("updatedAt")
            )

            require(transaction.id > 0L && transaction.amount > 0L) {
                "잘못된 거래 데이터입니다."
            }

            require(
                (transaction.fixedExpenseId == null) ==
                    (transaction.fixedExpenseYearMonth == null)
            ) {
                "잘못된 고정비 적용 정보입니다."
            }

            transaction.fixedExpenseId?.let { fixedExpenseId ->
                require(fixedExpenseId > 0L) {
                    "잘못된 고정비 ID입니다."
                }
                val appliedMonth = requireNotNull(
                    transaction.fixedExpenseYearMonth
                )
                runCatching {
                    YearMonth.parse(appliedMonth)
                }.getOrElse {
                    throw IllegalArgumentException("잘못된 고정비 적용 월입니다.")
                }
            }

            require(transaction.categoryId in categoryIds) {
                "존재하지 않는 카테고리를 참조하는 거래가 있습니다."
            }

            val category = categories.first {
                it.id == transaction.categoryId
            }

            require(category.type == transaction.type) {
                "거래 유형과 카테고리 유형이 일치하지 않습니다."
            }

            transactions.add(transaction)
        }

        require(transactions.map { it.id }.toSet().size == transactions.size) {
            "중복된 거래 ID가 있습니다."
        }

        val fixedExpenseApplications = transactions.mapNotNull { transaction ->
            transaction.fixedExpenseId?.let {
                it to transaction.fixedExpenseYearMonth
            }
        }
        require(
            fixedExpenseApplications.toSet().size == fixedExpenseApplications.size
        ) {
            "같은 달에 중복 적용된 고정비가 있습니다."
        }

        val financialProfile = if (
            root.has("financialProfile") &&
            !root.isNull("financialProfile")
        ) {
            val item = root.getJSONObject("financialProfile")
            val initialBalance = item.getLong("initialBalance")

            require(initialBalance >= 0L) {
                "잘못된 시작 보유금액입니다."
            }

            FinancialProfileEntity(
                initialBalance = initialBalance,
                updatedAt = item.optLong(
                    "updatedAt",
                    System.currentTimeMillis()
                )
            )
        } else {
            null
        }

        val fixedExpenses = mutableListOf<FixedExpenseEntity>()
        val fixedExpenseArray = root.optJSONArray("fixedExpenses")

        if (fixedExpenseArray != null) {
            for (i in 0 until fixedExpenseArray.length()) {
                val item = fixedExpenseArray.getJSONObject(i)
                val fixedExpense = FixedExpenseEntity(
                    id = item.getLong("id"),
                    name = item.getString("name"),
                    dayOfMonth = item.getInt("dayOfMonth"),
                    amount = item.getLong("amount"),
                    createdAt = item.getLong("createdAt")
                )

                require(
                    fixedExpense.id > 0L &&
                        fixedExpense.name.isNotBlank() &&
                        fixedExpense.dayOfMonth in 1..31 &&
                        fixedExpense.amount > 0L
                ) {
                    "잘못된 고정비 데이터입니다."
                }

                fixedExpenses.add(fixedExpense)
            }
        }

        require(fixedExpenses.map { it.id }.toSet().size == fixedExpenses.size) {
            "중복된 고정비 ID가 있습니다."
        }

        return BackupData(
            categories = categories,
            transactions = transactions,
            financialProfile = financialProfile,
            fixedExpenses = fixedExpenses
        )
    }

    suspend fun restore(data: BackupData) {
        database.withTransaction {
            val transactionDao = database.transactionDao()
            val categoryDao = database.categoryDao()
            val financialProfileDao = database.financialProfileDao()
            val fixedExpenseDao = database.fixedExpenseDao()

            // 외래 키 관계를 고려해 거래를 먼저 삭제
            transactionDao.deleteAll()
            fixedExpenseDao.deleteAll()
            categoryDao.deleteAll()
            financialProfileDao.deleteAll()

            // 기존 ID를 유지해 카테고리 참조 관계 복원
            data.categories.forEach {
                categoryDao.insert(it)
            }

            if (
                categoryDao.getByNameAndType(
                    FixedExpenseRepository.FIXED_EXPENSE_CATEGORY_NAME,
                    TransactionType.EXPENSE
                ) == null
            ) {
                categoryDao.insert(
                    CategoryEntity(
                        name = FixedExpenseRepository.FIXED_EXPENSE_CATEGORY_NAME,
                        type = TransactionType.EXPENSE,
                        sortOrder = 8
                    )
                )
            }

            data.fixedExpenses.forEach {
                fixedExpenseDao.insert(it)
            }

            data.transactions.forEach {
                transactionDao.insert(it)
            }

            data.financialProfile?.let {
                financialProfileDao.save(it)
            }
        }
    }
}
