package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.crypto.DiacInfinityKeys
import com.example.model.*
import com.example.service.DncEngine
import kotlinx.coroutines.flow.StateFlow

class DncViewModel : ViewModel() {

    private val engine = DncEngine.getInstance()

    val walletKeyPair: StateFlow<DiacInfinityKeys.KeyPair?> = engine.walletKeyPair
    val balance: StateFlow<Double> = engine.balance
    val transactions: StateFlow<List<Transaction>> = engine.transactions
    val fragments: StateFlow<List<Fragment>> = engine.fragments
    val peers: StateFlow<List<Peer>> = engine.peers
    val isMiningActive: StateFlow<Boolean> = engine.isMiningActive
    val miningMetrics: StateFlow<DncEngine.MiningMetrics> = engine.miningMetrics
    val storageChallenges: StateFlow<List<StorageChallenge>> = engine.storageChallenges
    val guardians: StateFlow<List<Guardian>> = engine.guardians
    val engineLogs: StateFlow<List<String>> = engine.engineLogs
    val networkMode: StateFlow<String> = engine.networkMode
    val networkPort: StateFlow<Int> = engine.networkPort

    fun createAccount() {
        engine.createAccount()
    }

    fun loginWithKeys(publicKey: String, privateKey: String): Boolean {
        return engine.loginWithKeys(publicKey, privateKey)
    }

    fun logout() {
        engine.logout()
    }

    fun sendDiac(recipient: String, amount: Double): Pair<Boolean, String> {
        return engine.sendDiac(recipient, amount)
    }

    fun toggleMining() {
        if (isMiningActive.value) {
            engine.stopMining()
        } else {
            engine.startMining()
        }
    }

    fun addPeer(ip: String, port: Int): Boolean {
        return engine.addPeer(ip, port)
    }

    fun pingPeer(peer: Peer) {
        engine.pingPeer(peer)
    }

    fun changeNetwork(mode: String, port: Int) {
        engine.setNetworkMode(mode, port)
    }

    fun triggerCustomAudit(fragId: String, textData: String): StorageChallenge {
        return engine.triggerCustomAudit(fragId, textData)
    }

    fun triggerRecovery(gName1: String, gName2: String): String {
        return engine.triggerGuardianRecovery(gName1, gName2)
    }
}
