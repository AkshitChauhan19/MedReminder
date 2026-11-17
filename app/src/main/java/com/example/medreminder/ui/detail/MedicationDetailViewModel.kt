package com.example.medreminder.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.medreminder.domain.model.Medication
import com.example.medreminder.domain.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicationDetailViewModel @Inject constructor(
    private val repository: MedicationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val medId: Long = checkNotNull(savedStateHandle["medId"])

    val medication: StateFlow<Medication?> = repository.getMedicationById(medId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _networkInfo = MutableStateFlow<String?>(null)
    val networkInfo: StateFlow<String?> = _networkInfo.asStateFlow()

    fun fetchDrugInfo(drugName: String) {
        viewModelScope.launch {
            val result = repository.fetchDrugInfoFromNetwork(drugName)
            _networkInfo.value = result.getOrNull()
        }
    }

    fun updateMedication(med: Medication) {
        viewModelScope.launch {
            repository.insertMedication(med)
        }
    }

    fun refillMedication(med: Medication) {
        viewModelScope.launch {
            repository.insertMedication(med.copy(remainingPills = med.totalPills))
        }
    }

    fun deleteMedication(med: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(med)
        }
    }
}