package com.example.moneynotesapp

import android.content.Context
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    // Variable untuk menyimpan ID jika sedang dalam mode Edit
    private var editTransactionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        val rgType = findViewById<RadioGroup>(R.id.rgType)
        val rbIncome = findViewById<RadioButton>(R.id.rbIncome)
        val rbExpense = findViewById<RadioButton>(R.id.rbExpense)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val tvSelectedDate = findViewById<TextView>(R.id.tvSelectedDate)
        val btnPickDate = findViewById<Button>(R.id.btnPickDate)

        // --- 1. SIAPKAN DUA DAFTAR KATEGORI ---
        val incomeCategories = arrayOf("Gaji", "Investasi", "Misc", "Dana Pemasukan Lain")
        // Saya tambahkan "Tagihan" dan "Belanja" agar pengeluarannya lebih logis
        val expenseCategories = arrayOf("Makanan", "Transportasi", "Tagihan", "Belanja", "Hiburan", "Lainnya")

        // Fungsi kecil untuk memperbarui isi Spinner
        fun updateSpinnerAdapter(categories: Array<String>) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
            spinnerCategory.adapter = adapter
        }

        // --- 2. LOGIKA PERUBAHAN OTOMATIS SAAT RADIO BUTTON DIKLIK ---
        rgType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rbIncome) {
                updateSpinnerAdapter(incomeCategories)
            } else {
                updateSpinnerAdapter(expenseCategories)
            }
        }

        // Beri nilai awal (karena saat pertama buka form, Pemasukan yang terpilih)
        updateSpinnerAdapter(incomeCategories)

        // --- LOGIKA TANGGAL (DEFAULT HARI INI & POP-UP) ---
        val calendar = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

        // Set teks default ke tanggal hari ini
        tvSelectedDate.text = sdf.format(calendar.time)

        // Tampilkan pop-up kalender saat tombol ditekan
        btnPickDate.setOnClickListener {
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            android.app.DatePickerDialog(this, { _, y, m, d ->
                val pickedDate = java.util.Calendar.getInstance()
                pickedDate.set(y, m, d)
                // Update TextView dengan tanggal yang dipilih user
                tvSelectedDate.text = sdf.format(pickedDate.time)
            }, year, month, day).show()
        }
        // --- BATAS LOGIKA TANGGAL ---

        // --- 3. CEK APAKAH INI MODE EDIT ---
        editTransactionId = intent.getStringExtra("EXTRA_ID")

        if (editTransactionId != null) {
            btnSave.text = "Update Transaksi"

            val oldType = intent.getStringExtra("EXTRA_TYPE")
            val oldCategory = intent.getStringExtra("EXTRA_CATEGORY")

            // Sesuaikan Radio Button dan isi Spinner dengan data lama
            if (oldType == "Pemasukan") {
                rbIncome.isChecked = true
                updateSpinnerAdapter(incomeCategories)
            } else {
                rbExpense.isChecked = true
                updateSpinnerAdapter(expenseCategories)
            }

            // Set posisi Spinner sesuai kategori lama
            val currentAdapter = spinnerCategory.adapter as ArrayAdapter<String>
            val spinnerPosition = currentAdapter.getPosition(oldCategory)
            spinnerCategory.setSelection(spinnerPosition)

            val oldAmount = intent.getDoubleExtra("EXTRA_AMOUNT", 0.0)
            // Format nominal lama ke dalam EditText (tanpa desimal nol)
            val formatter = java.text.DecimalFormat("#,###")
            val symbols = java.text.DecimalFormatSymbols()
            symbols.groupingSeparator = '.'
            formatter.decimalFormatSymbols = symbols
            etAmount.setText(formatter.format(oldAmount))
            tvSelectedDate.text = intent.getStringExtra("EXTRA_DATE") ?: sdf.format(calendar.time)
        }

        // --- 4. FORMAT ANGKA RIBUAN SAAT DIKETIK ---
        etAmount.addTextChangedListener(object : android.text.TextWatcher {
            private var current = ""
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString() != current) {
                    etAmount.removeTextChangedListener(this)
                    val cleanString = s.toString().replace(".", "")
                    if (cleanString.isNotEmpty()) {
                        try {
                            val parsed = cleanString.toDouble()
                            val formatter = java.text.DecimalFormat("#,###")
                            val symbols = java.text.DecimalFormatSymbols()
                            symbols.groupingSeparator = '.'
                            formatter.decimalFormatSymbols = symbols
                            val formatted = formatter.format(parsed)

                            current = formatted
                            etAmount.setText(formatted)
                            etAmount.setSelection(formatted.length)
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                        }
                    }
                    etAmount.addTextChangedListener(this)
                }
            }
        })

        // --- 5. SIMPAN DATA ---

        btnSave.setOnClickListener {
            val type = if (rgType.checkedRadioButtonId == R.id.rbIncome) "Pemasukan" else "Pengeluaran"
            val category = spinnerCategory.selectedItem.toString()
            val amountText = etAmount.text.toString().replace(".", "")

            if (amountText.isNotEmpty()) {
                val transaction = Transaction(
                    id = editTransactionId ?: java.util.UUID.randomUUID().toString(),
                    type = type,
                    category = category,
                    amount = amountText.toDouble(),
                    date = tvSelectedDate.text.toString()
                )
                saveTransaction(transaction)
                finish()
            } else {
                Toast.makeText(this, "Isi nominal dulu!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveTransaction(newTx: Transaction) {
        val sharedPref = getSharedPreferences("MyMoneyNotes", Context.MODE_PRIVATE)
        val gson = Gson()
        val jsonString = sharedPref.getString("transactions", "[]")

        val type = object : TypeToken<MutableList<Transaction>>() {}.type
        val transactions: MutableList<Transaction> = gson.fromJson(jsonString, type)

        if (editTransactionId != null) {
            // Jika mode Edit, cari data lama lalu ganti (replace) dengan yang baru
            val index = transactions.indexOfFirst { it.id == editTransactionId }
            if (index != -1) {
                transactions[index] = newTx
            }
        } else {
            // Jika mode Tambah, masukkan ke urutan terakhir
            transactions.add(newTx)
        }

        sharedPref.edit().putString("transactions", gson.toJson(transactions)).apply()
    }
}