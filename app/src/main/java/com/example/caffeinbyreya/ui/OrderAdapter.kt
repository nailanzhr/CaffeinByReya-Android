package com.example.caffeinbyreya.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.caffeinbyreya.R
import com.example.caffeinbyreya.data.OrderEntity
import com.example.caffeinbyreya.databinding.ItemOrderBinding
import java.text.NumberFormat
import java.util.Locale

class OrderAdapter(
    private val onMarkDone: (OrderEntity) -> Unit
) : ListAdapter<OrderEntity, OrderAdapter.ViewHolder>(OrderDiffCallback()) {

    inner class ViewHolder(val binding: ItemOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val ctx = holder.itemView.context
        val formatRp = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
            .format(item.totalHarga).replace(",00", "")

        holder.binding.apply {
            tvOrderTitle.text = ctx.getString(R.string.order_title_format, item.id)
            tvOrderTime.text = item.waktuPesan
            tvOrderItems.text = item.itemsSummary
            tvOrderTotal.text = ctx.getString(R.string.order_total_format, formatRp)
            tvOrderStatus.text = item.status

            if (item.status == ctx.getString(R.string.status_completed)) {
                tvOrderStatus.setTextColor(Color.parseColor("#2E7D32"))
                tvOrderStatus.setBackgroundColor(Color.parseColor("#E8F5E9"))
                btnMarkDone.visibility = View.GONE
            } else {
                tvOrderStatus.setTextColor(Color.parseColor("#6A1B9A"))
                tvOrderStatus.setBackgroundColor(Color.parseColor("#EFE5F5"))
                btnMarkDone.visibility = View.VISIBLE
                btnMarkDone.setOnClickListener { onMarkDone(item) }
            }
        }
    }

    class OrderDiffCallback : DiffUtil.ItemCallback<OrderEntity>() {
        override fun areItemsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: OrderEntity, newItem: OrderEntity): Boolean = oldItem == newItem
    }
}