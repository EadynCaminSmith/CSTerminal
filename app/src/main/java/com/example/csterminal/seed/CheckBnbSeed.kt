package com.example.csterminal.seed

import android.content.Context
import android.util.Log
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.utils.Numeric

class CheckBnbSeed(private val context: Context) {
    companion object {
        var receiveAddress: String = ""
        var privateKey: String = ""
    }

    fun checkBNBWallet(seedPhrase: String) {
        if (seedPhrase.isNotEmpty()) {
            Log.d("CheckBnbSeed", "Seed phrase found: $seedPhrase")
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seedPhrase.toByteArray())
            val path = intArrayOf(44 or 0x80000000.toInt(), 60 or 0x80000000.toInt(), 0 or 0x80000000.toInt(), 0, 0)
            val childKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
            val address = Keys.getAddress(childKeyPair.publicKey)
            receiveAddress = "0x$address"
            privateKey = Numeric.toHexStringWithPrefix(childKeyPair.privateKey)
            Log.d("CheckBnbSeed", "BNB Address: $receiveAddress")
            Log.d("CheckBnbSeed", "BNB Private Key: $privateKey")
        } else {
            Log.e("CheckBnbSeed", "Seed phrase not found.")
        }
    }
}
