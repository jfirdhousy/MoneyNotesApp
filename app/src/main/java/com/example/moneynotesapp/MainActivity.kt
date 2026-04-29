package com.example.moneynotesapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
//import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.app.AlertDialog
import android.widget.Toast
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry

class MainActivity : AppCompatActivity() {

    private lateinit var rvTransactions: RecyclerView
    private lateinit var tvBalance: TextView
    private lateinit var adapter: TransactionAdapter
    private var transactionList = mutableListOf<Transaction>()
    private lateinit var pieChart: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvBalance = findViewById(R.id.tvBalance)
        rvTransactions = findViewById(R.id.rvTransactions)
        pieChart = findViewById(R.id.pieChart)
        val btnAdd = findViewById<Button>(R.id.btnAddTransaction)

        rvTransactions.layoutManager = LinearLayoutManager(this)

        btnAdd.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
//         Muat ulang data setiap kali kembali ke halaman utama
        loadData()
    }

    private fun loadData() {
        val sharedPref = getSharedPreferences("MyMoneyNotes", Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonString = sharedPref.getString("transactions", "[]")

        val type = object : TypeToken<MutableList<Transaction>>() {}.type
        transactionList = gson.fromJson(jsonString, type)
        transactionList.sortByDescending { it.date } // Mengurutkan dari yang terbaru

        adapter = TransactionAdapter(
            transactionList,
            onItemClick = { tx ->
                // Jika diklik biasa: Pindah ke AddTransactionActivity dengan membawa data
                val intent = Intent(this, AddTransactionActivity::class.java)
                intent.putExtra("EXTRA_ID", tx.id)
                intent.putExtra("EXTRA_DATE", tx.date)
                intent.putExtra("EXTRA_TYPE", tx.type)
                intent.putExtra("EXTRA_CATEGORY", tx.category)
                intent.putExtra("EXTRA_AMOUNT", tx.amount)
                startActivity(intent)
            },
            onItemLongClick = { tx ->
                // Jika ditekan lama: Munculkan Dialog Hapus
                showDeleteDialog(tx)
            }
        )

        rvTransactions.adapter = adapter

        calculateBalance()
    }

    private fun showDeleteDialog(tx: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Transaksi")
            .setMessage("Yakin ingin menghapus ${tx.category} senilai Rp ${tx.amount}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteTransaction(tx)
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteTransaction(tx: Transaction) {
        // Hapus dari list
        transactionList.remove(tx)

        // Simpan list terbaru ke SharedPreferences
        val sharedPref = getSharedPreferences("MyMoneyNotes", Context.MODE_PRIVATE)
        sharedPref.edit().putString("transactions", Gson().toJson(transactionList)).apply()

        // Refresh UI
        loadData()
        Toast.makeText(this, "Transaksi dihapus", Toast.LENGTH_SHORT).show()
    }


    private fun calculateBalance() {
        var totalIncome = 0.0
        var totalExpense = 0.0

        for (tx in transactionList) {
            if (tx.type == "Pemasukan") {
                totalIncome += tx.amount
            } else {
                totalExpense += tx.amount
            }
        }

        val balance = totalIncome - totalExpense

        // Format Text Saldo
        val formatter = java.text.DecimalFormat("#,###")
        val symbols = java.text.DecimalFormatSymbols()
        symbols.groupingSeparator = '.'
        formatter.decimalFormatSymbols = symbols
        val formattedBalance = formatter.format(balance)

        tvBalance.text = "Rp $formattedBalance"

        // --- MULAI GAMBAR GRAFIK ---
        setupPieChart(totalIncome, totalExpense)
    }

    // Fungsi khusus untuk mengatur Grafik
    private fun setupPieChart(income: Double, expense: Double) {
        val entries = ArrayList<PieEntry>()

        // --- TETAP GUNAKAN LABEL AGAR MUNCUL DI LEGENDA BAWAH ---
        if (income > 0) entries.add(PieEntry(income.toFloat(), "Pemasukan"))
        if (expense > 0) entries.add(PieEntry(expense.toFloat(), "Pengeluaran"))

        if (entries.isEmpty()) {
            pieChart.clear()
            return
        }

        val dataSet = PieDataSet(entries, "")

        // --- 1. BARIS KODE INI UNTUK MENYEMBUNYIKAN ANGKA DI IRISAN ---
        dataSet.setDrawValues(false) // <-- Angka tidak akan digambar di pie
        // -------------------------------------------------------------

        // Set Warna
        val colors = ArrayList<Int>()
        if (income > 0) colors.add(Color.parseColor("#4CAF50"))
        if (expense > 0) colors.add(Color.parseColor("#F44336"))
        dataSet.colors = colors

        // Bagian ini (textColor/Size) tidak perlu dihapus,
        // tapi tidak akan ngefek karena values-nya dimatikan di atas.
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 14f

        val data = PieData(dataSet)

        // Konfigurasi tampilan PieChart
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = "Ringkasan"
        pieChart.setCenterTextSize(10f)

        // --- 2. PENGATURAN TAMBAHAN AGAR LEBIH RAPI ---
        // Tampilkan Label ("Pemasukan/Pengeluaran") di luar irisan, bukan di dalam
        pieChart.setDrawEntryLabels(false) // Matikan label di dalam irisan

        // Pastikan Legenda (kotak warna di bawah) muncul agar user tahu warna hijau/merah itu apa
        val legend = pieChart.legend
        legend.isEnabled = true
        legend.textSize = 12f
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        // ----------------------------------------------

        pieChart.animateY(1000)
        pieChart.invalidate()
    }


}