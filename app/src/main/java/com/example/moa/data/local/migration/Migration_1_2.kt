
package com.example.moa.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `budgets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `yearMonth` TEXT NOT NULL,
                `categoryId` INTEGER NOT NULL,
                `amount` INTEGER NOT NULL,
                `createdAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_budgets_yearMonth_categoryId`
            ON `budgets` (`yearMonth`, `categoryId`)
            """.trimIndent()
        )
    }
}
