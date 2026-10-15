
package com.example.condomanagement.data.model

data class Room(
    val roomId: String = "",
    val roomNumber: String = "",
    val tower: String = "A",
    val floor: Int = 0,
    val monthlyRent: Double = 0.0,
    val username: String = "",
    val tenantUid: String = "",
    val status: String = "available"
)
