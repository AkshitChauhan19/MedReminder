package com.example.medreminder.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    val scheduleMeds: StateFlow<List<Medication>> = repository.getTodaySchedule()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleDoseTaken(medId: Long, isTaken: Boolean) {
        viewModelScope.launch {
            repository.toggleDoseTaken(medId, isTaken)
        }
    }

    fun insertMedication(medication: Medication) {
        viewModelScope.launch {
            repository.insertMedication(medication)
        }
    }
}
