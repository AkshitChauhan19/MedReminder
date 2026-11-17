package com.example.medreminder.util

import com.example.medreminder.domain.model.Medication
import java.util.Calendar
import java.util.Locale

object TimeUtils {

    /**
     * Converts any raw time string (e.g. "13:00", "13:00 PM", "20:00", "20:00 PM",
     * "8:00", "08:00 AM", "12:00 PM", "0:30") into strict 12-hour AM/PM format ("hh:mm a").
     */
    fun formatTo12Hour(rawTime: String?): String {
        if (rawTime.isNullOrBlank()) return "08:00 AM"

        val cleaned = rawTime.trim().uppercase(Locale.getDefault())
        val regex = Regex("""^(\d{1,2}):(\d{2})(?:\s*([AP]M))?""")
        val match = regex.find(cleaned) ?: return "08:00 AM"

        val rawHour = match.groupValues[1].toIntOrNull() ?: 8
        val rawMinute = match.groupValues[2].toIntOrNull() ?: 0
        val amPm = match.groupValues[3] // "AM", "PM", or ""

        val hour24: Int
        val minute = rawMinute.coerceIn(0, 59)

        if (rawHour >= 13) {
            // Already 24-hour hour (e.g. 13:00 or 20:00 or 13:00 PM or 20:00 PM)
            hour24 = rawHour.coerceIn(0, 23)
        } else if (rawHour == 12) {
            hour24 = if (amPm == "AM") 0 else 12
        } else if (amPm == "PM") {
            hour24 = rawHour + 12
        } else if (amPm == "AM") {
            hour24 = rawHour
        } else {
            // No AM/PM provided and hour <= 12
            hour24 = if (rawHour in 1..11) rawHour else if (rawHour == 12) 12 else 0
        }

        return formatFromHourMinute(hour24, minute)
    }

    /**
     * Parses a time string into 24-hour (hourOfDay: 0..23, minute: 0..59).
     */
    fun getHourAndMinute(rawTime: String?): Pair<Int, Int> {
        if (rawTime.isNullOrBlank()) return Pair(8, 0)

        val cleaned = rawTime.trim().uppercase(Locale.getDefault())
        val regex = Regex("""^(\d{1,2}):(\d{2})(?:\s*([AP]M))?""")
        val match = regex.find(cleaned) ?: return Pair(8, 0)

        val rawHour = match.groupValues[1].toIntOrNull() ?: 8
        val rawMinute = (match.groupValues[2].toIntOrNull() ?: 0).coerceIn(0, 59)
        val amPm = match.groupValues[3]

        val hour24 = if (rawHour >= 13) {
            rawHour.coerceIn(0, 23)
        } else if (rawHour == 12) {
            if (amPm == "AM") 0 else 12
        } else if (amPm == "PM") {
            rawHour + 12
        } else {
            rawHour
        }

        return Pair(hour24.coerceIn(0, 23), rawMinute)
    }

    /**
     * Formats 24-hour (hourOfDay 0..23, minute 0..59) into 12-hour "hh:mm a".
     * Examples: (8, 0) -> "08:00 AM", (13, 0) -> "01:00 PM", (20, 0) -> "08:00 PM"
     */
    fun formatFromHourMinute(hourOfDay: Int, minute: Int): String {
        val clampedHour = hourOfDay.coerceIn(0, 23)
        val clampedMinute = minute.coerceIn(0, 59)

        val isPm = clampedHour >= 12
        val hour12 = when (clampedHour % 12) {
            0 -> 12
            else -> clampedHour % 12
        }
        val marker = if (isPm) "PM" else "AM"
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, clampedMinute, marker)
    }

    /**
     * Infers shift from hourOfDay (0..23):
     * 5..11 -> Morning
     * 12..16 -> Afternoon
     * 17..20 -> Evening
     * 21..23 or 0..4 -> Night
     */
    fun inferShiftFromHour(hourOfDay: Int): String {
        return when (hourOfDay) {
            in 5..11 -> "Morning"
            in 12..16 -> "Afternoon"
            in 17..20 -> "Evening"
            else -> "Night"
        }
    }

    /**
     * Infers shift from raw time string.
     */
    fun inferShiftFromTime(rawTime: String?): String {
        val (hour, _) = getHourAndMinute(rawTime)
        return inferShiftFromHour(hour)
    }

    /**
     * Resolves the shift for a given medication.
     * Guaranteed to return one of "Morning", "Afternoon", "Evening", "Night".
     */
    fun getShiftForMedication(med: Medication): String {
        val shift = med.timeOfDay.trim()
        if (shift.equals("Morning", ignoreCase = true)) return "Morning"
        if (shift.equals("Afternoon", ignoreCase = true)) return "Afternoon"
        if (shift.equals("Evening", ignoreCase = true)) return "Evening"
        if (shift.equals("Night", ignoreCase = true) || shift.equals("Bedtime", ignoreCase = true) || shift.equals("Nightly", ignoreCase = true)) return "Night"

        // Fallback: infer from scheduledTime
        return inferShiftFromTime(med.scheduledTime)
    }
}
