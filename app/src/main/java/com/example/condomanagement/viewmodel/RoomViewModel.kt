
package com.example.condomanagement.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.condomanagement.data.model.Room
import com.example.condomanagement.data.repository.RoomRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RoomUiState(
    val loading: Boolean = false,
    val rooms: List<Room> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    val assigning: Boolean = false
)

class RoomViewModel(
    private val repository: RoomRepository = RoomRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState.asStateFlow()

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loading = true,
                error = null
            )

            try {
                val rooms = repository.getAllRooms()
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    rooms = rooms
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "โหลดข้อมูลห้องไม่สำเร็จ"
                )
            }
        }
    }

    fun addRoom(
        roomId: String,
        monthlyRent: Double,
        floor: Int,
        status: String,
        username: String = "",
        tower: String = "A"
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                assigning = true,
                error = null,
                message = null
            )

            try {
                repository.addRoom(
                    Room(
                        roomId = roomId.trim(),
                        roomNumber = roomId.trim(),
                        tower = tower.trim().ifBlank { "A" },
                        floor = floor,
                        monthlyRent = monthlyRent,
                        username = "",
                        tenantUid = "",
                        status = status
                    )
                )

                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    message = "เพิ่มห้อง $roomId สำเร็จ"
                )
                loadRooms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    error = e.message ?: "เพิ่มห้องไม่สำเร็จ"
                )
            }
        }
    }

    fun setRoomUsername(roomId: String, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                assigning = true,
                error = null,
                message = null
            )

            try {
                repository.setRoomUsername(roomId, username)

                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    message = "บันทึก Username ของห้อง $roomId สำเร็จ"
                )
                loadRooms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    error = e.message ?: "บันทึก Username ไม่สำเร็จ"
                )
            }
        }
    }

    fun deleteRoom(roomId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                assigning = true,
                error = null,
                message = null
            )

            try {
                repository.deleteRoom(roomId)

                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    message = "ลบห้อง $roomId สำเร็จ"
                )
                loadRooms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    error = e.message ?: "ลบห้องไม่สำเร็จ"
                )
            }
        }
    }

    fun assignTenant(roomId: String, username: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                assigning = true,
                error = null,
                message = null
            )

            try {
                repository.assignTenant(roomId, username)

                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    message = "ผูกบัญชีผู้เช่ากับห้อง $roomId สำเร็จ"
                )
                loadRooms()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    assigning = false,
                    error = e.message ?: "กำหนดผู้เช่าไม่สำเร็จ"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            message = null
        )
    }
}