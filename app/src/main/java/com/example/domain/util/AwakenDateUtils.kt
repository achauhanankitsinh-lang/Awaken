package com.example.domain.util

import com.example.domain.model.ReportPeriod
import com.google.firebase.Timestamp
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DateRange(val startDate: String, val endDate: String)

data class CalendarDayCell(
  val dateKey: String,
  val dayOfMonth: Int,
  val isCurrentMonth: Boolean
)

object AwakenDateUtils {
  const val TIMEZONE_ID = "Asia/Kolkata"
  private val IST_TIMEZONE: TimeZone = TimeZone.getTimeZone(TIMEZONE_ID)
  private val DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

  private fun newIsoFormat(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
      timeZone = IST_TIMEZONE
      isLenient = false
    }

  private fun newMonthFormat(): SimpleDateFormat =
    SimpleDateFormat("yyyy-MM", Locale.US).apply {
      timeZone = IST_TIMEZONE
      isLenient = false
    }

  fun isValidDateKey(value: String): Boolean {
    if (!DATE_REGEX.matches(value)) return false
    return try {
      newIsoFormat().parse(value) != null
    } catch (_: Exception) {
      false
    }
  }

  fun todayIst(nowMillis: Long = System.currentTimeMillis()): String {
    return newIsoFormat().format(Date(nowMillis))
  }

  fun currentMonthIst(nowMillis: Long = System.currentTimeMillis()): String {
    return newMonthFormat().format(Date(nowMillis))
  }

  fun toDateKeyIst(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    return newIsoFormat().format(date)
  }

  fun parseCalendarIst(dateKey: String): Calendar {
    val parsed = requireNotNull(newIsoFormat().parse(dateKey)) { "Invalid date key: $dateKey" }
    return Calendar.getInstance(IST_TIMEZONE, Locale.US).apply {
      time = parsed
      firstDayOfWeek = Calendar.MONDAY
    }
  }

  fun shiftDays(dateKey: String, deltaDays: Int): String {
    val cal = parseCalendarIst(dateKey)
    cal.add(Calendar.DAY_OF_MONTH, deltaDays)
    return newIsoFormat().format(cal.time)
  }

  fun shiftMonths(yearMonth: String, deltaMonths: Int): String {
    val cal = parseCalendarIst("$yearMonth-01")
    cal.add(Calendar.MONTH, deltaMonths)
    return newMonthFormat().format(cal.time)
  }

  /**
   * Computes Monday-start weekly boundaries and calendar month boundaries in Asia/Kolkata.
   */
  fun dateRangeForPeriod(anchorDate: String, period: ReportPeriod): DateRange {
    val cal = parseCalendarIst(anchorDate)
    return when (period) {
      ReportPeriod.DAILY -> DateRange(anchorDate, anchorDate)
      ReportPeriod.WEEKLY -> {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val offsetFromMonday = when (dayOfWeek) {
          Calendar.MONDAY -> 0
          Calendar.TUESDAY -> 1
          Calendar.WEDNESDAY -> 2
          Calendar.THURSDAY -> 3
          Calendar.FRIDAY -> 4
          Calendar.SATURDAY -> 5
          Calendar.SUNDAY -> 6
          else -> 0
        }
        val startCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -offsetFromMonday) }
        val endCal = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
        DateRange(
          startDate = newIsoFormat().format(startCal.time),
          endDate = newIsoFormat().format(endCal.time)
        )
      }
      ReportPeriod.MONTHLY -> {
        val startCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val lastDay = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val endCal = (startCal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, lastDay) }
        DateRange(
          startDate = newIsoFormat().format(startCal.time),
          endDate = newIsoFormat().format(endCal.time)
        )
      }
    }
  }

  fun monthGridMondayFirst(yearMonth: String): List<CalendarDayCell?> {
    val cal = parseCalendarIst("$yearMonth-01")
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val leadingBlanks = when (firstDayOfWeek) {
      Calendar.MONDAY -> 0
      Calendar.TUESDAY -> 1
      Calendar.WEDNESDAY -> 2
      Calendar.THURSDAY -> 3
      Calendar.FRIDAY -> 4
      Calendar.SATURDAY -> 5
      Calendar.SUNDAY -> 6
      else -> 0
    }
    val cells = mutableListOf<CalendarDayCell?>()
    repeat(leadingBlanks) { cells.add(null) }
    val fmt = newIsoFormat()
    for (day in 1..daysInMonth) {
      val dayCal = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, day) }
      cells.add(
        CalendarDayCell(
          dateKey = fmt.format(dayCal.time),
          dayOfMonth = day,
          isCurrentMonth = true
        )
      )
    }
    while (cells.size % 7 != 0) {
      cells.add(null)
    }
    return cells
  }

  fun formatDisplayDate(dateKey: String): String {
    return try {
      val date = newIsoFormat().parse(dateKey) ?: return dateKey
      SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).apply { timeZone = IST_TIMEZONE }.format(date)
    } catch (_: Exception) {
      dateKey
    }
  }

  fun formatShortDate(dateKey: String): String {
    return try {
      val date = newIsoFormat().parse(dateKey) ?: return dateKey
      SimpleDateFormat("dd MMM", Locale.US).apply { timeZone = IST_TIMEZONE }.format(date)
    } catch (_: Exception) {
      dateKey
    }
  }

  fun formatMonthTitle(yearMonth: String): String {
    return try {
      val date = newIsoFormat().parse("$yearMonth-01") ?: return yearMonth
      SimpleDateFormat("MMMM yyyy", Locale.US).apply { timeZone = IST_TIMEZONE }.format(date)
    } catch (_: Exception) {
      yearMonth
    }
  }

  fun formatTimestampTimeIst(timestamp: Timestamp?): String {
    if (timestamp == null) return ""
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.US).apply {
      timeZone = IST_TIMEZONE
    }.format(timestamp.toDate())
  }

  fun formatCurrency(amount: Double): String {
    val formatter = DecimalFormat("#,##,##0.00", DecimalFormatSymbols(Locale.US))
    val absFormatted = formatter.format(kotlin.math.abs(amount))
    return if (amount < 0) "-₹$absFormatted" else "₹$absFormatted"
  }

  fun formatMetric(value: Double): String {
    return if (value % 1.0 == 0.0) {
      value.toLong().toString()
    } else {
      DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)).format(value)
    }
  }
}
