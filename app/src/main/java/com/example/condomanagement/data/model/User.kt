package com.example.condomanagement.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val roomId: String = "",
    val role: String = "tenant"
)