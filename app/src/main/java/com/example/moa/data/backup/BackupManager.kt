
package com.example.moa.data.backup

import androidx.room.withTransaction
import com.example.moa.data.local.AppDatabase
import com.example.moa.data.local.entity.CategoryEntity
import com.example.moa.data.local.entity.TransactionEntity
import com.example.moa.data.local.entity.TransactionType
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class BackupData(
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>
)

class BackupManager(
    private val database: AppDatabase
) {
    suspend fun exportJson(): String {
        val categories = database.categoryDao().getAllForBackup()
        val transactions = database.transactionDao().getAllForBackup()

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
                    put("createdAt", transaction.createdAt)
                    put("updatedAt", transaction.updatedAt)
                }
            )
        }

        root.put("categories", categoryArray)
        root.put("transactions", transactionArray)

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
                createdAt = item.getLong("createdAt"),
                updatedAt = item.getLong("updatedAt")
            )

            require(transaction.id > 0L && transaction.amount > 0L) {
                "잘못된 거래 데이터입니다."
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

        return BackupData(categories, transactions)
    }

    suspend fun restore(data: BackupData) {
        database.withTransaction {
            val transactionDao = database.transactionDao()
            val categoryDao = database.categoryDao()

            // 외래 키 관계를 고려해 거래를 먼저 삭제
            transactionDao.deleteAll()
            categoryDao.deleteAll()

            // 기존 ID를 유지해 카테고리 참조 관계 복원
            data.categories.forEach {
                categoryDao.insert(it)
            }

            data.transactions.forEach {
                transactionDao.insert(it)
            }
        }
    }
}
