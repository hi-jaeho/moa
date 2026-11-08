package com.example.moa.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `fixed_expenses` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `name` TEXT NOT NULL,
                `dayOfMonth` INTEGER NOT NULL,
                `amount` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "ALTER TABLE `transactions` ADD COLUMN `fixedExpenseId` INTEGER"
        )
        db.execSQL(
            "ALTER TABLE `transactions` ADD COLUMN `fixedExpenseYearMonth` TEXT"
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_transactions_fixedExpenseId_fixedExpenseYearMonth`
            ON `transactions` (`fixedExpenseId`, `fixedExpenseYearMonth`)
            """.trimIndent()
        )
    }
}
