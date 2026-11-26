package com.example.caffeinbyreya.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: User)

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    suspend fun loginUser(email: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenu(menu: Menu)

    @Update
    suspend fun updateMenu(menu: Menu)

    @Delete
    suspend fun deleteMenu(menu: Menu)

    @Query("SELECT * FROM menus ORDER BY id DESC")
    fun getAllMenus(): LiveData<List<Menu>>

    @Query("SELECT * FROM menus WHERE id = :id LIMIT 1")
    suspend fun getMenuById(id: Int): Menu?

    @Query("SELECT COUNT(*) FROM menus")
    suspend fun getMenuCount(): Int

    @Query("SELECT * FROM menus WHERE fotoUri LIKE :prefix")
    suspend fun getMenusByPhotoPrefix(prefix: String): List<Menu>

    @Query("UPDATE menus SET fotoUri = :newFotoUri WHERE id = :menuId")
    suspend fun updateMenuPhotoUri(menuId: Int, newFotoUri: String)

    // --- ORDERS / PESANAN ---
    @Insert
    suspend fun insertOrder(order: OrderEntity)

    @Query("UPDATE orders SET status = :newStatus WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Int, newStatus: String)

    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getAllOrders(): LiveData<List<OrderEntity>>

    // --- KERANJANG PERSISTEN (CART ITEMS) ---
    @Query("SELECT * FROM cart_items")
    fun getAllCartItems(): LiveData<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items")
    suspend fun getAllCartItemsSync(): List<CartItemEntity>

    @Query("SELECT * FROM cart_items WHERE menuId = :menuId LIMIT 1")
    suspend fun getCartItemByMenuId(menuId: Int): CartItemEntity?

    @Query("SELECT COALESCE(SUM(qty), 0) FROM cart_items")
    suspend fun getTotalCartCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCartItem(item: CartItemEntity)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}