package com.example.caffeinbyreya.data

import androidx.lifecycle.LiveData

class AppRepository(private val dao: AppDao) {
    val allMenus: LiveData<List<Menu>> = dao.getAllMenus()
    val allOrders: LiveData<List<OrderEntity>> = dao.getAllOrders()
    val allCartItems: LiveData<List<CartItemEntity>> = dao.getAllCartItems()

    suspend fun getMenuCount(): Int = dao.getMenuCount()
    suspend fun getMenuById(id: Int): Menu? = dao.getMenuById(id)
    suspend fun insertMenu(menu: Menu) = dao.insertMenu(menu)
    suspend fun updateMenu(menu: Menu) = dao.updateMenu(menu)
    suspend fun deleteMenu(menu: Menu) = dao.deleteMenu(menu)

    suspend fun migrateUnsplashMenusToLocal(nameToLocalUriMap: Map<String, String>) {
        val legacyMenus = dao.getMenusByPhotoPrefix("https://images.unsplash.com/%")
        for (menu in legacyMenus) {
            val localUri = nameToLocalUriMap[menu.namaMenu]
            if (localUri != null) {
                dao.updateMenuPhotoUri(menu.id, localUri)
            }
        }
    }

    suspend fun getUserByEmail(email: String): User? = dao.getUserByEmail(email)
    suspend fun registerUser(user: User) = dao.registerUser(user)
    suspend fun loginUser(email: String, hashedPass: String): User? = dao.loginUser(email, hashedPass)

    suspend fun insertOrder(order: OrderEntity) = dao.insertOrder(order)
    suspend fun updateOrderStatus(orderId: Int, status: String) = dao.updateOrderStatus(orderId, status)

    // --- KERANJANG PERSISTEN ---
    suspend fun getAllCartItemsSync(): List<CartItemEntity> = dao.getAllCartItemsSync()

    suspend fun addOrUpdateCartItem(menu: Menu, qtyToAdd: Int): Int {
        val existing = dao.getCartItemByMenuId(menu.id)
        val newQty = (existing?.qty ?: 0) + qtyToAdd
        dao.upsertCartItem(
            CartItemEntity(
                menuId = menu.id,
                namaMenu = menu.namaMenu,
                kategori = menu.kategori,
                harga = menu.harga,
                deskripsi = menu.deskripsi,
                fotoUri = menu.fotoUri,
                qty = newQty
            )
        )
        return dao.getTotalCartCount()
    }

    suspend fun clearCart() = dao.clearCart()
}