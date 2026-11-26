package com.example.caffeinbyreya.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menus")
data class Menu(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val namaMenu: String,
    val kategori: String,
    val harga: Int,
    val deskripsi: String,
    val fotoUri: String,
    val tersedia: Boolean = true
)