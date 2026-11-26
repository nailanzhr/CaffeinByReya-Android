package com.example.caffeinbyreya.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val menuId: Int,
    val namaMenu: String,
    val kategori: String,
    val harga: Int,
    val deskripsi: String,
    val fotoUri: String,
    val qty: Int
) {
    fun toCartItem(): CartItem {
        return CartItem(
            menu = Menu(
                id = menuId,
                namaMenu = namaMenu,
                kategori = kategori,
                harga = harga,
                deskripsi = deskripsi,
                fotoUri = fotoUri
            ),
            qty = qty
        )
    }
}