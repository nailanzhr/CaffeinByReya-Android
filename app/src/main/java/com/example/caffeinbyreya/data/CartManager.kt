package com.example.caffeinbyreya.data

data class CartItem(
    val menu: Menu,
    var qty: Int
)

object CartManager {
    private val items = mutableListOf<CartItem>()

    fun addItem(menu: Menu, qty: Int) {
        val existing = items.find { it.menu.id == menu.id }
        if (existing != null) {
            existing.qty += qty
        } else {
            items.add(CartItem(menu, qty))
        }
    }

    fun getItems(): List<CartItem> = items.toList()

    fun removeItem(menuId: Int) {
        items.removeAll { it.menu.id == menuId }
    }

    fun clear() {
        items.clear()
    }

    fun getTotalPrice(): Int = items.sumOf { it.menu.harga * it.qty }
    fun getTotalCount(): Int = items.sumOf { it.qty }
}