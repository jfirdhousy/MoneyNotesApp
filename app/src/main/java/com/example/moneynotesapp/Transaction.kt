package com.example.moneynotesapp

data class Transaction(
    val id: String,
    val type: String,
    val category: String,
    val amount: Double,
    val date: String
)