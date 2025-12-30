package com.example.csterminal.ui.theme

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel

class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("csterminal_settings", Context.MODE_PRIVATE)
    private val cryptoSymbols = listOf("BTC", "ETH", "LTC", "TRX", "ATOM", "XRP", "SOL", "BNB", "USDT", "USDC")

    val isDarkTheme = mutableStateOf(prefs.getBoolean("dark_theme", false))
    val currency = mutableStateOf(prefs.getString("currency", "AUD") ?: "AUD")

    // Surcharge/Discount percentages for each cryptocurrency
    val cryptoSurcharges = mutableStateMapOf<String, Double>().apply {
        cryptoSymbols.forEach { symbol ->
            put(symbol, prefs.getFloat("surcharge_$symbol", 0.0f).toDouble())
        }
    }

    // State for whether to pass fees on to the customer for each cryptocurrency
    val passFeesOn = mutableStateMapOf<String, Boolean>().apply {
        cryptoSymbols.forEach { symbol ->
            put(symbol, prefs.getBoolean("pass_fee_$symbol", false))
        }
    }

    // State for whether a cryptocurrency is enabled for payment
    val enabledCoins = mutableStateMapOf<String, Boolean>().apply {
        cryptoSymbols.forEach { symbol ->
            put(symbol, prefs.getBoolean("enabled_$symbol", true))
        }
    }


    fun toggleTheme() {
        val newValue = !isDarkTheme.value
        isDarkTheme.value = newValue
        prefs.edit().putBoolean("dark_theme", newValue).apply()
    }

    fun setCurrency(newCurrency: String) {
        currency.value = newCurrency
        prefs.edit().putString("currency", newCurrency).apply()
    }

    fun setSurcharge(symbol: String, value: Double) {
        cryptoSurcharges[symbol] = value
        prefs.edit().putFloat("surcharge_$symbol", value.toFloat()).apply()
    }

    fun setPassFee(symbol: String, isEnabled: Boolean) {
        passFeesOn[symbol] = isEnabled
        prefs.edit().putBoolean("pass_fee_$symbol", isEnabled).apply()
    }

    fun setCoinEnabled(symbol: String, isEnabled: Boolean) {
        enabledCoins[symbol] = isEnabled
        prefs.edit().putBoolean("enabled_$symbol", isEnabled).apply()
    }
}