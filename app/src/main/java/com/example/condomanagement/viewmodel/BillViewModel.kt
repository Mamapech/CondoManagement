
package com.example.condomanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condomanagement.data.model.Bill
import com.example.condomanagement.data.repository.BillRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BillUiState(
    val loading: Boolean = false,
    val bills: List<Bill> = emptyList(),
    val error: String? = null
) {
    val outstandingBalance: Double
        get() = bills
            .filter {
                it.status.equals("pending", ignoreCase = true) ||
                        it.status.equals("overdue", ignoreCase = true)
            }
            .sumOf { it.amount }
}

class BillViewModel(
    private val repository: BillRepository = BillRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillUiState())
    val uiState: StateFlow<BillUiState> = _uiState.asStateFlow()

    fun loadBills() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                error = null
            )

            try {
                val bills = repository.getMyBills()

                _uiState.value = BillUiState(
                    loading = false,
                    bills = bills
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.localizedMessage
                        ?: "ไม่สามารถโหลดข้อมูลบิลได้"
                )
            }
        }
    }
}
