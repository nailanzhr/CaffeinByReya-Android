package com.example.caffeinbyreya.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.caffeinbyreya.R
import com.example.caffeinbyreya.data.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppRepository(AppDatabase.getDatabase(application).appDao())
    val allMenus: LiveData<List<Menu>> = repository.allMenus
    val allOrders: LiveData<List<OrderEntity>> = repository.allOrders
    val allCartItems: LiveData<List<CartItemEntity>> = repository.allCartItems

    // State UI bertahan saat rotasi layar
    var activeTabId: Int = R.id.nav_home
    var currentCategory: String = "Semua"
    var currentQuery: String = ""
    var currentSortIndex: Int = 0

    // MediatorLiveData otomatis memicu observer cartVersion di MainActivity setiap kali tabel cart_items berubah
    val cartVersion: MediatorLiveData<Int> = MediatorLiveData<Int>().apply {
        value = 0
        addSource(allCartItems) {
            value = (value ?: 0) + 1
        }
    }

    init {
        initializeAndMigrateMenus()
    }

    private fun localDrawableUri(drawableName: String): String {
        val pkg = getApplication<Application>().packageName
        return "android.resource://$pkg/drawable/$drawableName"
    }

    private fun defaultSeedPhotoMap(): Map<String, String> = mapOf(
        "Signature Purple Velvet Latte" to localDrawableUri("purple_velvet_latte"),
        "Kopi Susu Gula Aren Reya" to localDrawableUri("kopi_susu_gula_aren"),
        "Caramel Macchiato Iced" to localDrawableUri("caramel_macchiato"),
        "Espresso Tonic Berry" to localDrawableUri("espresso_tonic_berry"),
        "Biscoff Mocha Frappe" to localDrawableUri("biscoff_mocha_frappe"),
        "Matcha Berry Cloud" to localDrawableUri("matcha_berry_cloud"),
        "Dark Choco Hazelnut" to localDrawableUri("dark_choco_hazelnut"),
        "Almond Butter Croissant" to localDrawableUri("almond_butter_croissant"),
        "Pain au Chocolat" to localDrawableUri("pain_au_chocolat"),
        "Tiramisu Mille Crepe" to localDrawableUri("tiramisu_mille_crepe")
    )

    private fun initializeAndMigrateMenus() {
        viewModelScope.launch {
            val photoMap = defaultSeedPhotoMap()
            if (repository.getMenuCount() == 0) {
                val initialMenus = listOf(
                    Menu(
                        namaMenu = "Signature Purple Velvet Latte",
                        kategori = "Coffee",
                        harga = 28000,
                        deskripsi = "Perpaduan espresso house blend Caffein by Reya dengan krim taro velvet ungu lembut dan susu segar.",
                        fotoUri = photoMap.getValue("Signature Purple Velvet Latte")
                    ),
                    Menu(
                        namaMenu = "Kopi Susu Gula Aren Reya",
                        kategori = "Coffee",
                        harga = 20000,
                        deskripsi = "Kopi susu klasik favorit pelanggan dengan espresso Arabika Gayo dan gula aren organik pilihan.",
                        fotoUri = photoMap.getValue("Kopi Susu Gula Aren Reya")
                    ),
                    Menu(
                        namaMenu = "Caramel Macchiato Iced",
                        kategori = "Coffee",
                        harga = 26000,
                        deskripsi = "Espresso double shot dituang di atas susu dingin beraroma vanila dengan saus karamel.",
                        fotoUri = photoMap.getValue("Caramel Macchiato Iced")
                    ),
                    Menu(
                        namaMenu = "Espresso Tonic Berry",
                        kategori = "Coffee",
                        harga = 27000,
                        deskripsi = "Kesegaran tonic water berpadu dengan double shot espresso dan sirup wild berry ungu.",
                        fotoUri = photoMap.getValue("Espresso Tonic Berry")
                    ),
                    Menu(
                        namaMenu = "Biscoff Mocha Frappe",
                        kategori = "Coffee",
                        harga = 32000,
                        deskripsi = "Minuman kopi coklat dingin blended dengan selai Lotus Biscoff dan remahan biskuit karamel.",
                        fotoUri = photoMap.getValue("Biscoff Mocha Frappe")
                    ),
                    Menu(
                        namaMenu = "Matcha Berry Cloud",
                        kategori = "Non-Coffee",
                        harga = 25000,
                        deskripsi = "Pure Uji Matcha Jepang berpadu dengan selai stroberi homemade dan cold foam lembut.",
                        fotoUri = photoMap.getValue("Matcha Berry Cloud")
                    ),
                    Menu(
                        namaMenu = "Dark Choco Hazelnut",
                        kategori = "Non-Coffee",
                        harga = 24000,
                        deskripsi = "Coklat Belgia pekat dengan aroma kacang hazelnut panggang yang creamy dan hangat.",
                        fotoUri = photoMap.getValue("Dark Choco Hazelnut")
                    ),
                    Menu(
                        namaMenu = "Almond Butter Croissant",
                        kategori = "Pastry",
                        harga = 22000,
                        deskripsi = "Croissant renyah berlapis mentega Prancis dengan isian krim almond panggang.",
                        fotoUri = photoMap.getValue("Almond Butter Croissant")
                    ),
                    Menu(
                        namaMenu = "Pain au Chocolat",
                        kategori = "Pastry",
                        harga = 21000,
                        deskripsi = "Pastry klasik bertekstur flaky dengan batangan dark chocolate meleleh di bagian dalam.",
                        fotoUri = photoMap.getValue("Pain au Chocolat")
                    ),
                    Menu(
                        namaMenu = "Tiramisu Mille Crepe",
                        kategori = "Pastry",
                        harga = 30000,
                        deskripsi = "20 lapis crepe lembut dengan krim mascarpone kopi dan taburan bubuk kakao.",
                        fotoUri = photoMap.getValue("Tiramisu Mille Crepe")
                    )
                )
                initialMenus.forEach { repository.insertMenu(it) }
            } else {
                // Perbarui foto menu bawaan versi lama yang masih memakai link Unsplash
                repository.migrateUnsplashMenusToLocal(photoMap)
            }
        }
    }

    // --- AUTENTIKASI (SHA-256 + SALT) ---
    fun register(nama: String, email: String, pass: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            if (repository.getUserByEmail(email) != null) {
                onResult(false)
            } else {
                val hashed = SecurityUtils.hashPasswordWithSalt(pass, email)
                repository.registerUser(User(nama = nama, email = email, password = hashed))
                onResult(true)
            }
        }
    }

    fun login(email: String, pass: String, onResult: (User?) -> Unit) {
        viewModelScope.launch {
            val hashed = SecurityUtils.hashPasswordWithSalt(pass, email)
            onResult(repository.loginUser(email, hashed))
        }
    }

    // --- CRUD MENU ---
    fun insertMenu(menu: Menu, onDone: () -> Unit) {
        viewModelScope.launch { repository.insertMenu(menu); onDone() }
    }

    fun updateMenu(menu: Menu, onDone: () -> Unit) {
        viewModelScope.launch { repository.updateMenu(menu); onDone() }
    }

    fun deleteMenu(menu: Menu, onDone: () -> Unit) {
        viewModelScope.launch { repository.deleteMenu(menu); onDone() }
    }

    suspend fun getMenuById(id: Int): Menu? = repository.getMenuById(id)

    // --- KERANJANG PERSISTEN (ROOM) & CHECKOUT PESANAN ---
    fun addToCart(menu: Menu, qty: Int, onDone: (totalCount: Int) -> Unit = {}) {
        viewModelScope.launch {
            val totalCount = repository.addOrUpdateCartItem(menu, qty)
            onDone(totalCount)
        }
    }

    fun getCartItems(): List<CartItem> {
        return allCartItems.value.orEmpty().map { it.toCartItem() }
    }

    fun getCartTotalPrice(): Int {
        return allCartItems.value.orEmpty().sumOf { it.harga * it.qty }
    }

    fun getCartTotalCount(): Int {
        return allCartItems.value.orEmpty().sumOf { it.qty }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
        }
    }

    fun checkoutCart(summary: String, waktuPesan: String, statusAwal: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val items = repository.getAllCartItemsSync()
            if (items.isEmpty()) {
                onResult(false)
                return@launch
            }
            val total = items.sumOf { it.harga * it.qty }
            repository.insertOrder(
                OrderEntity(
                    itemsSummary = summary,
                    totalHarga = total,
                    waktuPesan = waktuPesan,
                    status = statusAwal
                )
            )
            repository.clearCart()
            onResult(true)
        }
    }

    fun completeOrder(orderId: Int) {
        viewModelScope.launch { repository.updateOrderStatus(orderId, "Selesai") }
    }
}