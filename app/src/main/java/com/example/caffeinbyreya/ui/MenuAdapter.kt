package com.example.caffeinbyreya.ui

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.caffeinbyreya.data.Menu
import com.example.caffeinbyreya.databinding.ItemMenuBinding
import java.text.NumberFormat
import java.util.Locale

class MenuAdapter(
    private val onItemClick: (Menu) -> Unit
) : ListAdapter<Menu, MenuAdapter.MenuViewHolder>(MenuDiffCallback()) {

    inner class MenuViewHolder(val binding: ItemMenuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = getItem(position)
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID")).format(item.harga)
        holder.binding.apply {
            tvMenuName.text = item.namaMenu
            tvCategoryBadge.text = item.kategori
            tvMenuPrice.text = formatRp.replace(",00", "")
            if (item.fotoUri.isNotEmpty()) {
                Glide.with(ivMenuPhoto.context)
                    .load(Uri.parse(item.fotoUri))
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(ivMenuPhoto)
            } else {
                ivMenuPhoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }
            root.setOnClickListener { onItemClick(item) }
        }
    }

    class MenuDiffCallback : DiffUtil.ItemCallback<Menu>() {
        override fun areItemsTheSame(oldItem: Menu, newItem: Menu): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Menu, newItem: Menu): Boolean = oldItem == newItem
    }
}