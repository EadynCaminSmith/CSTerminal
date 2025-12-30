package com.example.csterminal.seed

import android.content.Context
import android.util.Log
import org.xrpl.xrpl4j.wallet.DefaultWalletFactory
import org.web3j.crypto.MnemonicUtils
import org.xrpl.xrpl4j.codec.addresses.AddressCodec
import org.xrpl.xrpl4j.codec.addresses.VersionType
import org.xrpl.xrpl4j.codec.addresses.UnsignedByteArray
import org.xrpl.xrpl4j.wallet.Wallet

class CheckXrpSeed(private val context: Context) {
    companion object {
        var receiveAddress: String = ""
        var privateKey: String = ""
        var xrpWallet: Wallet? = null
    }

    fun checkXrpWallet(seedPhrase: String) {
        if (seedPhrase.isNotEmpty()) {
            Log.d("CheckXrpSeed", "Seed phrase found: $seedPhrase")
            try {
                
                
                val entropy = MnemonicUtils.generateEntropy(seedPhrase)
                
                val addressCodec = AddressCodec.getInstance()
                val rippleSeed = addressCodec.encodeSeed(UnsignedByteArray.of(entropy), VersionType.ED25519)
                
                val walletFactory = DefaultWalletFactory.getInstance()
                
                val wallet = walletFactory.fromSeed(
                    rippleSeed,
                    false
                )

                xrpWallet = wallet
                receiveAddress = wallet.classicAddress().value()
                privateKey = wallet.privateKey().orElse("")
                
                Log.d("CheckXrpSeed", "XRP Address: $receiveAddress")
            } catch (e: Exception) {
                Log.e("CheckXrpSeed", "Error generating XRP wallet", e)
            }
        } else {
            Log.e("CheckXrpSeed", "Seed phrase not found.")
        }
    }
}
