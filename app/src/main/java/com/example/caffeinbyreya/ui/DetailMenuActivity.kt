package com.example.caffeinbyreya.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.caffeinbyreya.R
import com.example.caffeinbyreya.data.Menu
import com.example.caffeinbyreya.databinding.ActivityDetailMenuBinding
import com.example.caffeinbyreya.databinding.BottomSheetOrderBinding
import com.example.caffeinbyreya.viewmodel.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class DetailMenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDetailMenuBinding
    private val viewModel: MainViewModel by viewModels()
    private var currentMenu: Menu? = null
    private var menuId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        menuId = intent.getIntExtra("MENU_ID", -1)

        binding.btnEditMenu.setOnClickListener {
            val intent = Intent(this, FormMenuActivity::class.java)
            intent.putExtra("MENU_ID", menuId)
            startActivity(intent)
        }

        binding.btnDeleteMenu.setOnClickListener {
            currentMenu?.let { menu ->
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.dialog_delete_title))
                    .setMessage(getString(R.string.dialog_delete_msg, menu.namaMenu))
                    .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                        viewModel.deleteMenu(menu) {
                            Toast.makeText(this, getString(R.string.msg_menu_deleted), Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .setNegativeButton(getString(R.string.btn_cancel), null)
                    .show()
            }
        }

        binding.btnOrder.setOnClickListener {
            currentMenu?.let { menu -> showOrderBottomSheet(menu) }
        }
    }

    private fun showOrderBottomSheet(menu: Menu) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetOrderBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        var qty = 1
        fun formatRupiah(amount: Int): String =
            NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(amount).replace(",00", "")

        sheetBinding.tvSheetCategory.text = menu.kategori
        sheetBinding.tvSheetMenuName.text = menu.namaMenu
        sheetBinding.tvSheetUnitPrice.text = getString(R.string.sheet_unit_price_format, formatRupiah(menu.harga))
        sheetBinding.tvQty.text = qty.toString()
        sheetBinding.tvSheetSubtotal.text = formatRupiah(menu.harga * qty)

        sheetBinding.btnMinus.setOnClickListener {
            if (qty > 1) {
                qty--
                sheetBinding.tvQty.text = qty.toString()
                sheetBinding.tvSheetSubtotal.text = formatRupiah(menu.harga * qty)
            }
        }

        sheetBinding.btnPlus.setOnClickListener {
            qty++
            sheetBinding.tvQty.text = qty.toString()
            sheetBinding.tvSheetSubtotal.text = formatRupiah(menu.harga * qty)
        }

        sheetBinding.btnAddToCart.setOnClickListener {
            sheetBinding.btnAddToCart.isEnabled = false
            viewModel.addToCart(menu, qty) { totalCount ->
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
                Toast.makeText(
                    this,
                    getString(R.string.msg_added_to_cart, qty, menu.namaMenu, totalCount),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        if (menuId != -1) {
            lifecycleScope.launch {
                currentMenu = viewModel.getMenuById(menuId)
                currentMenu?.let { menu ->
                    val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(menu.harga)
                    binding.tvDetailName.text = menu.namaMenu
                    binding.tvDetailCategory.text = menu.kategori
                    binding.tvDetailPrice.text = formatRp.replace(",00", "")
                    binding.tvDetailDesc.text = menu.deskripsi
                    if (menu.fotoUri.isNotEmpty()) {
                        Glide.with(this@DetailMenuActivity)
                            .load(Uri.parse(menu.fotoUri))
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(binding.ivDetailPhoto)
                    } else {
                        binding.ivDetailPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                }
            }
        }
    }
}