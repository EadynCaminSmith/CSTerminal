package com.example.csterminal.transaction

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject

data class Transaction(
    val fiatAmount: String,
    val currency: String,
    val cryptoAmount: String,
    val coinSymbol: String,
    val txHash: String,
    val txUrl: String,
    val feeFiat: String = "0.00",
    val feePassedToCustomer: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("csterminal_transactions", Context.MODE_PRIVATE)
    private val _transactions = mutableStateListOf<Transaction>()
    val transactions: List<Transaction> = _transactions

    init {
        loadTransactions()
    }

    fun addTransaction(transaction: Transaction) {
        if (_transactions.none { it.txHash == transaction.txHash }) {
            _transactions.add(0, transaction)
            saveTransactions()
        }
    }

    private fun saveTransactions() {
        val jsonArray = JSONArray()
        _transactions.forEach { transaction ->
            val jsonObject = JSONObject().apply {
                put("fiatAmount", transaction.fiatAmount)
                put("currency", transaction.currency)
                put("cryptoAmount", transaction.cryptoAmount)
                put("coinSymbol", transaction.coinSymbol)
                put("txHash", transaction.txHash)
                put("txUrl", transaction.txUrl)
                put("feeFiat", transaction.feeFiat)
                put("feePassedToCustomer", transaction.feePassedToCustomer)
                put("timestamp", transaction.timestamp)
            }
            jsonArray.put(jsonObject)
        }
        prefs.edit().putString("transactions_list", jsonArray.toString()).apply()
    }

    private fun loadTransactions() {
        val jsonString = prefs.getString("transactions_list", null)
        if (jsonString != null) {
            try {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val transaction = Transaction(
                        fiatAmount = jsonObject.getString("fiatAmount"),
                        currency = jsonObject.optString("currency", "AUD"),
                        cryptoAmount = jsonObject.getString("cryptoAmount"),
                        coinSymbol = jsonObject.getString("coinSymbol"),
                        txHash = jsonObject.getString("txHash"),
                        txUrl = jsonObject.getString("txUrl"),
                        feeFiat = jsonObject.optString("feeFiat", "0.00"),
                        feePassedToCustomer = jsonObject.optBoolean("feePassedToCustomer", false),
                        timestamp = jsonObject.getLong("timestamp")
                    )
                    _transactions.add(transaction)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
