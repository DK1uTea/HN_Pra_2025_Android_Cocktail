package com.example.cocktaildb.data.model

data class Checkmark(
    val id: String = "",
    val uid: String = "",
    val cocktailId: String = "",
    val checkedAt: Long = System.currentTimeMillis()
)
