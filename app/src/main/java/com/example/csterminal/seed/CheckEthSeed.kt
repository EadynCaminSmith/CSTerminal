package com.example.csterminal.seed

import android.content.Context
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.MnemonicUtils
import org.web3j.utils.Numeric

class CheckEthSeed(private val context: Context) {
    var receiveAddress = ""; companion object {var receiveAddress = ""}
    fun checkEthereumWallet() {
        try {
            val seedPhrase = generateSeedPhrase.phrase
            val seed = MnemonicUtils.generateSeed(seedPhrase, null)
            val masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed)
            val path = intArrayOf(44 or Bip32ECKeyPair.HARDENED_BIT, 60 or Bip32ECKeyPair.HARDENED_BIT, 0 or Bip32ECKeyPair.HARDENED_BIT, 0, 0)
            val ethereumKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path)
            val credentials = org.web3j.crypto.Credentials.create(ethereumKeyPair)
            receiveAddress = credentials.address
            CheckEthSeed.receiveAddress = receiveAddress
        } catch (e: Exception) {
            println("Error creating wallet: ${e.message}")
            e.printStackTrace()
        }
    }
}
