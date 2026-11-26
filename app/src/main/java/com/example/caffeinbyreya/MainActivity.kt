package com.example.caffeinbyreya

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.caffeinbyreya.data.Menu
import com.example.caffeinbyreya.data.SessionManager
import com.example.caffeinbyreya.databinding.ActivityMainBinding
import com.example.caffeinbyreya.ui.DetailMenuActivity
import com.example.caffeinbyreya.ui.FormMenuActivity
import com.example.caffeinbyreya.ui.LoginActivity
import com.example.caffeinbyreya.ui.MenuAdapter
import com.example.caffeinbyreya.ui.OrderAdapter
import com.example.caffeinbyreya.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var session: SessionManager
    private lateinit var menuAdapter: MenuAdapter
    private lateinit var orderAdapter: OrderAdapter

    private var fullList: List<Menu> = emptyList()

    private val pickProfilePhotoLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
            session.saveProfilePhoto(uri.toString())
            Glide.with(this).load(uri).circleCrop().into(binding.ivProfilePhoto)
        } else {
            Toast.makeText(this, getString(R.string.msg_no_photo_selected), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        session = SessionManager(this)
        binding.tvGreeting.text = getString(R.string.greeting_format, session.getUserName())
        binding.tvProfileName.text = session.getUserName()
        binding.tvProfileEmail.text = session.getUserEmail()

        val savedPhoto = session.getProfilePhoto()
        if (savedPhoto.isNotEmpty()) {
            Glide.with(this).load(Uri.parse(savedPhoto)).circleCrop().into(binding.ivProfilePhoto)
        } else {
            Glide.with(this).load(R.drawable.foto_naila).circleCrop().into(binding.ivProfilePhoto)
        }

        binding.btnChangeProfilePhoto.setOnClickListener {
            pickProfilePhotoLauncher.launch(arrayOf("image/*"))
        }

        menuAdapter = MenuAdapter { menu ->
            val intent = Intent(this, DetailMenuActivity::class.java)
            intent.putExtra("MENU_ID", menu.id)
            startActivity(intent)
        }
        binding.rvMenu.layoutManager = LinearLayoutManager(this)
        binding.rvMenu.adapter = menuAdapter

        orderAdapter = OrderAdapter { order ->
            viewModel.completeOrder(order.id)
            Toast.makeText(this, getString(R.string.msg_order_completed, order.id), Toast.LENGTH_SHORT).show()
        }
        binding.rvOrders.layoutManager = LinearLayoutManager(this)
        binding.rvOrders.adapter = orderAdapter

        binding.progressBar.visibility = View.VISIBLE
        viewModel.allMenus.observe(this) { list ->
            binding.progressBar.visibility = View.GONE
            fullList = list
            applyFilter()
        }

        viewModel.allOrders.observe(this) { orders ->
            orderAdapter.submitList(orders)
            binding.tvEmptyOrders.visibility = if (orders.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.cartVersion.observe(this) {
            refreshCartUI()
        }

        binding.btnClearCart.setOnClickListener {
            viewModel.clearCart()
            Toast.makeText(this, getString(R.string.msg_cart_cleared), Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckoutCart.setOnClickListener {
            val cartItems = viewModel.getCartItems()
            if (cartItems.isEmpty()) {
                Toast.makeText(this, getString(R.string.msg_cart_empty_warn), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val summary = cartItems.joinToString("\n") {
                getString(R.string.order_summary_line_format, it.qty, it.menu.namaMenu, it.menu.kategori)
            }
            val timeNow = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("in", "ID")).format(Date())

            viewModel.checkoutCart(summary, timeNow, getString(R.string.status_preparing)) { success ->
                if (success) {
                    Toast.makeText(this, getString(R.string.msg_order_created), Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Pulihkan state pencarian, chip kategori, dan sort dari ViewModel setelah rotasi
        if (viewModel.currentQuery.isNotEmpty()) {
            binding.searchView.setQuery(viewModel.currentQuery, false)
        }
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.currentQuery = query.orEmpty()
                applyFilter()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.currentQuery = newText.orEmpty()
                applyFilter()
                return true
            }
        })

        updateSortButtonText()
        binding.btnSort.setOnClickListener {
            val options = arrayOf(
                getString(R.string.sort_newest),
                getString(R.string.sort_cheapest),
                getString(R.string.sort_expensive),
                getString(R.string.sort_az)
            )
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.sort_dialog_title))
                .setItems(options) { _, which ->
                    viewModel.currentSortIndex = which
                    updateSortButtonText()
                    applyFilter()
                }
                .show()
        }

        when (viewModel.currentCategory) {
            "Coffee" -> binding.chipCoffee.isChecked = true
            "Non-Coffee" -> binding.chipNonCoffee.isChecked = true
            "Pastry" -> binding.chipPastry.isChecked = true
            else -> binding.chipAll.isChecked = true
        }
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            viewModel.currentCategory = when (checkedIds.firstOrNull()) {
                R.id.chipCoffee -> "Coffee"
                R.id.chipNonCoffee -> "Non-Coffee"
                R.id.chipPastry -> "Pastry"
                else -> "Semua"
            }
            applyFilter()
        }

        setupOutletMap()
        binding.btnRetryMap.setOnClickListener { loadOutletMap() }

        binding.btnOpenMaps.setOnClickListener {
            try {
                val gmmIntentUri = Uri.parse("geo:5.1801,97.1507?q=5.1801,97.1507(Caffein+by+Reya)")
                startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, getString(R.string.err_no_maps_app), Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            session.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    viewModel.activeTabId = R.id.nav_home
                    showPage(catalog = true)
                    true
                }
                R.id.nav_add -> {
                    startActivity(Intent(this, FormMenuActivity::class.java))
                    false
                }
                R.id.nav_orders -> {
                    viewModel.activeTabId = R.id.nav_orders
                    refreshCartUI()
                    showPage(orders = true)
                    true
                }
                R.id.nav_outlet -> {
                    viewModel.activeTabId = R.id.nav_outlet
                    loadOutletMap()
                    showPage(outlet = true)
                    true
                }
                R.id.nav_profile -> {
                    viewModel.activeTabId = R.id.nav_profile
                    showPage(profile = true)
                    true
                }
                else -> false
            }
        }

        binding.bottomNav.selectedItemId = viewModel.activeTabId
    }

    private fun setupOutletMap() {
        binding.webViewMap.settings.javaScriptEnabled = true
        binding.webViewMap.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.pbMapLoading.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    binding.pbMapLoading.visibility = View.GONE
                    binding.webViewMap.visibility = View.GONE
                    binding.layoutMapError.visibility = View.VISIBLE
                }
            }
        }
        loadOutletMap()
    }

    private fun loadOutletMap() {
        if (!isNetworkAvailable()) {
            binding.pbMapLoading.visibility = View.GONE
            binding.webViewMap.visibility = View.GONE
            binding.layoutMapError.visibility = View.VISIBLE
            return
        }
        binding.layoutMapError.visibility = View.GONE
        binding.webViewMap.visibility = View.VISIBLE
        binding.pbMapLoading.visibility = View.VISIBLE
        binding.webViewMap.loadUrl("https://www.openstreetmap.org/export/embed.html?bbox=97.1300%2C5.1700%2C97.1600%2C5.1900&layer=mapnik&marker=5.1801%2C97.1507")
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun updateSortButtonText() {
        binding.btnSort.text = when (viewModel.currentSortIndex) {
            1 -> getString(R.string.sort_cheapest)
            2 -> getString(R.string.sort_expensive)
            3 -> getString(R.string.sort_az)
            else -> getString(R.string.btn_sort)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCartUI()
    }

    private fun showPage(
        catalog: Boolean = false,
        orders: Boolean = false,
        outlet: Boolean = false,
        profile: Boolean = false
    ) {
        binding.layoutCatalog.visibility = if (catalog) View.VISIBLE else View.GONE
        binding.layoutOrders.visibility = if (orders) View.VISIBLE else View.GONE
        binding.layoutOutlet.visibility = if (outlet) View.VISIBLE else View.GONE
        binding.layoutProfile.visibility = if (profile) View.VISIBLE else View.GONE
    }

    private fun refreshCartUI() {
        val items = viewModel.getCartItems()
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            .format(viewModel.getCartTotalPrice()).replace(",00", "")

        if (items.isEmpty()) {
            binding.tvCartList.text = getString(R.string.cart_empty)
        } else {
            binding.tvCartList.text = items.joinToString("\n") {
                val sub = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
                    .format(it.menu.harga * it.qty).replace(",00", "")
                getString(R.string.cart_item_line_format, it.qty, it.menu.namaMenu, it.menu.kategori, sub)
            }
        }
        binding.tvCartTotal.text = getString(R.string.cart_total_format, formatRp)
    }

    private fun applyFilter() {
        val filtered = fullList.filter { menu ->
            val matchCat = viewModel.currentCategory == "Semua" || menu.kategori.equals(viewModel.currentCategory, ignoreCase = true)
            val matchQuery = menu.namaMenu.contains(viewModel.currentQuery, ignoreCase = true)
            matchCat && matchQuery
        }
        val sorted = when (viewModel.currentSortIndex) {
            1 -> filtered.sortedBy { it.harga }
            2 -> filtered.sortedByDescending { it.harga }
            3 -> filtered.sortedBy { it.namaMenu.lowercase() }
            else -> filtered
        }
        menuAdapter.submitList(sorted)
        binding.tvEmptyState.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
    }
}