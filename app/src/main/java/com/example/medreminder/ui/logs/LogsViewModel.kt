package com.example.medreminder.ui.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.domain.model.AdherenceStats
import com.example.medreminder.domain.model.DoseLog
import com.example.medreminder.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    val adherenceStats: StateFlow<AdherenceStats> = repository.getAdherenceStats()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdherenceStats()
        )

    val doseLogs: StateFlow<List<DoseLog>> = repository.getRecentDoseLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}