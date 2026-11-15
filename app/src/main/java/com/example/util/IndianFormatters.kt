package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object IndianFormatters {
    private val indianLocale = Locale("en", "IN")

    fun formatCurrency(amount: Double): String {
        val rounded = kotlin.math.round(amount * 100.0) / 100.0
        val formatted = formatIndianNumber(abs(rounded))
        return if (rounded < 0) "-₹$formatted" else "₹$formatted"
    }

    fun formatCurrencyPlain(amount: Double): String {
        val rounded = kotlin.math.round(amount * 100.0) / 100.0
        val formatted = formatIndianNumber(abs(rounded))
        return if (rounded < 0) "-Rs.$formatted" else "Rs.$formatted"
    }

    private fun formatIndianNumber(value: Double): String {
        val totalPaise = kotlin.math.round(value * 100.0).toLong()
        val integerPart = totalPaise / 100L
        val decimalPart = (totalPaise % 100L).toString().padStart(2, '0')
        val intStr = integerPart.toString()
        if (intStr.length <= 3) return "$intStr.$decimalPart"

        val lastThree = intStr.takeLast(3)
        var rest = intStr.dropLast(3)
        val groups = mutableListOf<String>()
        while (rest.length > 2) {
            groups.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) groups.add(0, rest)
        return "${groups.joinToString(",")},$lastThree.$decimalPart"
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", indianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", indianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", indianLocale)
        return sdf.format(Date(timestamp))
    }

    fun formatFileTimestamp(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatInvoiceDateCode(timestamp: Long = System.currentTimeMillis()): String {
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun startOfTodayMillis(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfDayMillis(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }

    fun startOfWeekMillis(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfMonthMillis(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun startOfYearMillis(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
