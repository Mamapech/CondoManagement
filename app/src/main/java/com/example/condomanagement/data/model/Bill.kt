
package com.example.condomanagement.data.model

data class Bill(
    val billId: String = "",
    val uid: String = "",
    val roomId: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val dueDate: String = "",
    val status: String = "pending",
    val createdAt: Long = 0L
)
