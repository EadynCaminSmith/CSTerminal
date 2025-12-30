package com.example.csterminal.seed

import android.content.Context
import org.bitcoindevkit.Descriptor
import org.bitcoindevkit.DescriptorSecretKey
import org.bitcoindevkit.KeychainKind
import org.bitcoindevkit.Mnemonic
import org.bitcoindevkit.Network
import org.bitcoindevkit.Wallet
import org.bitcoindevkit.DatabaseConfig

class CheckSeed(private val context: Context) {
    var receiveAddress = ""; companion object {var receiveAddress = ""}
    fun checkBitcoinWallet() {
        try {
            val seedPhrase = generateSeedPhrase.phrase
            val mnemonic = Mnemonic.fromString(seedPhrase)
            val descriptorSecretKey = DescriptorSecretKey(Network.BITCOIN, mnemonic, null)
            val externalDescriptor = Descriptor.newBip84(descriptorSecretKey, KeychainKind.EXTERNAL, Network.BITCOIN)
            val internalDescriptor = Descriptor.newBip84(descriptorSecretKey, KeychainKind.INTERNAL, Network.BITCOIN)
            val wallet = Wallet(
                externalDescriptor,
                internalDescriptor,
                Network.BITCOIN,
                DatabaseConfig.Memory
            )
            val btcReceiveAddress = wallet.getAddress(org.bitcoindevkit.AddressIndex.New).address.asString()
            val balance = wallet.getBalance().total
            receiveAddress = btcReceiveAddress
            CheckSeed.receiveAddress = receiveAddress
        } catch (e: Exception) {
            println("Error creating wallet: ${e.message}")
            e.printStackTrace()
        }
    }
}
