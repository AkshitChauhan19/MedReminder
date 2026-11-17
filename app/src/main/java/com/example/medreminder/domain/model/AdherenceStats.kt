package com.example.medreminder.domain.model

/**
 * Represents individual day adherence for the weekly calendar bubbles.
 */
data class DayAdherence(
    val dayName: String,
    val dayNumber: Int,
    val isComplete: Boolean,
    val isMissed: Boolean,
    val isToday: Boolean = false
)

/**
 * Represents overall weekly adherence statistics for the Health Logs screen.
 */
data class AdherenceStats(
    val weeklyPercentage: Int = 80,
    val perfectDaysCount: Int = 4,
    val totalDaysCount: Int = 5,
    val goalPercentage: Int = 95,
    val weeklyRecord: List<DayAdherence> = emptyList()
)
