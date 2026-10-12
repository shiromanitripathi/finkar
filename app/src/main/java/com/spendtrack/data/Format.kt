package com.spendtrack.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object DateUtil {
    const val DAY = 86_400_000L

    /** Start (00:00) of the day that is [offsetDays] away from [now]. */
    fun dayStart(now: Long, offsetDays: Int): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        c.add(Calendar.DAY_OF_YEAR, offsetDays)
        return c.timeInMillis
    }

    fun monthStart(now: Long): Long {
        val c = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return c.timeInMillis
    }
}

object Fmt {
    /** 1234567 -> "12,34,567" (Indian grouping, independent of device locale data). */
    private fun indianGroup(n: Long): String {
        val s = n.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        var rest = s.dropLast(3)
        val parts = mutableListOf<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    fun money(v: Double): String {
        val cents = Math.round(abs(v) * 100.0)
        val whole = cents / 100
        val frac = (cents % 100).toInt()
        val body = indianGroup(whole) + if (frac == 0) "" else "." + frac.toString().padStart(2, '0')
        return (if (v < 0 && cents > 0) "-₹" else "₹") + body
    }

    fun round2(v: Double): Double = Math.round(v * 100.0) / 100.0

    fun shortDate(ts: Long): String =
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ts))

    fun dateTime(ts: Long): String =
        SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(Date(ts))

    fun dayLabel(ts: Long, now: Long = System.currentTimeMillis()): String {
        val today = DateUtil.dayStart(now, 0)
        return when {
            ts >= today -> "Today"
            ts >= today - DateUtil.DAY -> "Yesterday"
            else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(ts))
        }
    }
}
