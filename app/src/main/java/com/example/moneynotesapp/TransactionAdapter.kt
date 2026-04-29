package com.example.moneynotesapp

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private val transactions: List<Transaction>,
    // Tambahkan 2 aksi ini untuk menangani klik dan klik-tahan
    private val onItemClick: (Transaction) -> Unit,
    private val onItemLongClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategory: TextView = view.findViewById(R.id.tvCategory)
        val tvType: TextView = view.findViewById(R.id.tvType)
        val tvAmount: TextView = view.findViewById(R.id.tvAmount)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.tvCategory.text = transaction.category
        holder.tvType.text = transaction.type
        holder.tvDate.text = transaction.date

        if (transaction.type == "Pemasukan") {
            holder.tvAmount.text = "+ Rp ${transaction.amount}"
            holder.tvAmount.setTextColor("#4CAF50".toColorInt())
        } else {
            holder.tvAmount.text = "- Rp ${transaction.amount}"
            holder.tvAmount.setTextColor("#F44336".toColorInt())
        }

        // --- TAMBAHKAN BAGIAN INI UNTUK UPDATE & DELETE ---

        // Klik biasa untuk Edit
        holder.itemView.setOnClickListener {
            onItemClick(transaction)
        }

        // Klik tahan untuk Hapus
        holder.itemView.setOnLongClickListener {
            onItemLongClick(transaction)
            true // 'true' menandakan kita sudah menangani aksi ini
        }
    }

    override fun getItemCount() = transactions.size
}