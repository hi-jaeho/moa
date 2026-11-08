package com.example.moa.data.local.converter

import androidx.room.TypeConverter
import com.example.moa.data.local.entity.TransactionType
import java.time.LocalDate

class DateConverter {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let(LocalDate::parse)
    }

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? {
        return value?.let(TransactionType::valueOf)
    }
}
