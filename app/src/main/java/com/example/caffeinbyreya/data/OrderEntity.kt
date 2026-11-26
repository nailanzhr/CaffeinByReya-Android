package com.example.caffeinbyreya.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemsSummary: String,
    val totalHarga: Int,
    val waktuPesan: String,
    val status: String // "Sedang Dibuat" atau "Selesai"
)