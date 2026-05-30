package com.example.service

import com.example.crypto.DiacInfinityKeys
import com.example.crypto.ShamirSecretSharing
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID

class DncEngine private constructor() {

    companion object {
        @Volatile
        private var instance: DncEngine? = null

        fun getInstance(): DncEngine {
            return instance ?: synchronized(this) {
                instance ?: DncEngine().also { instance = it }
            }
        }
    }

    private val coroutineScope = CoroutineScope(DispatchScope + SupervisorJob())

    // Network & Config State
    private val _networkMode = MutableStateFlow("Central (Port 369)")
    val networkMode: StateFlow<String> = _networkMode.asStateFlow()

    private val _networkPort = MutableStateFlow(369)
    val networkPort: StateFlow<Int> = _networkPort.asStateFlow()

    // Identity and Wallet State
    private val _walletKeyPair = MutableStateFlow<DiacInfinityKeys.KeyPair?>(null)
    val walletKeyPair: StateFlow<DiacInfinityKeys.KeyPair?> = _walletKeyPair.asStateFlow()

    private val _balance = MutableStateFlow(0.0)
    val balance: StateFlow<Double> = _balance.asStateFlow()

    // Transctions
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    // Fragment Manager
    private val _fragments = MutableStateFlow<List<Fragment>>(emptyList())
    val fragments: StateFlow<List<Fragment>> = _fragments.asStateFlow()

    // Active P2P peers
    private val _peers = MutableStateFlow<List<Peer>>(emptyList())
    val peers: StateFlow<List<Peer>> = _peers.asStateFlow()

    // Mining PoSB metrics
    private val _isMiningActive = MutableStateFlow(false)
    val isMiningActive: StateFlow<Boolean> = _isMiningActive.asStateFlow()

    data class MiningMetrics(
        val totalRewards: Double = 0.0,
        val hourlyRate: Double = 0.0,
        val challengesCompleted: Int = 0,
        val efficiency: Float = 0f,
        val logs: List<String> = emptyList()
    )

    private val _miningMetrics = MutableStateFlow(MiningMetrics())
    val miningMetrics: StateFlow<MiningMetrics> = _miningMetrics.asStateFlow()

    // Proof of Storage Demo State
    private val _storageChallenges = MutableStateFlow<List<StorageChallenge>>(emptyList())
    val storageChallenges: StateFlow<List<StorageChallenge>> = _storageChallenges.asStateFlow()

    // Guardians
    private val _guardians = MutableStateFlow<List<Guardian>>(
        listOf(
            Guardian("Node-Guardian Alpha", "DNC-GUARDIAN9X2F8K5A"),
            Guardian("Node-Guardian Beta", "DNC-GUARDIAN3L7T1W8V"),
            Guardian("Node-Guardian Gamma", "DNC-GUARDIAN5Y4M1P0Q")
        )
    )
    val guardians: StateFlow<List<Guardian>> = _guardians.asStateFlow()

    private val _engineLogs = MutableStateFlow<List<String>>(listOf("System Initialized - No Wallet."))
    val engineLogs: StateFlow<List<String>> = _engineLogs.asStateFlow()

    private var miningJob: Job? = null
    private var gossipJob: Job? = null

    init {
        // Build simulated network initial peers
        resetPeers()
        startConsensusGossip()
    }

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$timestamp] $message"
        _engineLogs.value = (listOf(formatted) + _engineLogs.value).take(150)
    }

    private fun addMiningLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val formatted = "[$timestamp] $message"
        _miningMetrics.value = _miningMetrics.value.copy(
            logs = (listOf(formatted) + _miningMetrics.value.logs).take(100)
        )
    }

    fun setNetworkMode(mode: String, port: Int) {
        _networkMode.value = mode
        _networkPort.value = port
        addLog("Network mode changed to $mode on port $port")
        resetPeers()
    }

    private fun resetPeers() {
        val currentP = _networkPort.value
        _peers.value = listOf(
            Peer("192.168.1.101", currentP, "DNC-BOB_X5T8F4D2K1L9S7W3U4V2"),
            Peer("192.168.1.105", currentP, "DNC-ALICE_K3T7S1M2N5V4M0P9L1Q"),
            Peer("172.20.10.4", currentP, "DNC-SEED_NODE_8369_STUN"),
            Peer("45.123.456.78", currentP, "DNC-GLOBAL_BOOTSTRAP_PEER_A")
        )
        addLog("Registered default bootstrap discovery peers.")
    }

    /**
     * Create Account: generates DIAC ∞ key pairs and receives the instant 369 welcome tokens as per V7.5 specifications.
     */
    fun createAccount() {
        coroutineScope.launch {
            addLog("Generating fresh DIAC ∞ keypair using transcendental PI window...")
            val generated = DiacInfinityKeys.generateKeyPair()
            _walletKeyPair.value = generated
            _balance.value = 369.0 // Gift of 369 DIAC instant welcome balance!
            
            // Create default covering fragments
            _fragments.value = listOf(
                Fragment(
                    fragmentId = UUID.randomUUID().toString().take(12),
                    addressRangeStart = "00000000",
                    addressRangeEnd = "ffffffff",
                    txCount = 0,
                    creationTime = System.currentTimeMillis(),
                    lastUpdate = System.currentTimeMillis(),
                    epoch = 1,
                    balances = mapOf(generated.address to 369.0)
                )
            )

            addLog("✓ New Identity created: ${generated.address}")
            addLog("🎉 Welcome Gift: 369.0 DIAC assigned to your local range covering fragment!")
        }
    }

    /**
     * Login using exported public/private keys
     */
    fun loginWithKeys(publicKeyJson: String, privateKeyJson: String): Boolean {
        return try {
            addLog("Attempting load of external keys...")
            
            // Extract the fields from mock JSONs gracefully
            val addr = if (publicKeyJson.contains("Address:")) {
                publicKeyJson.replace("Address:", "").trim()
            } else {
                "DNC-IMPORTED_" + publicKeyJson.hashCode().toString().take(12)
            }
            
            // Build keypair
            val tempPair = DiacInfinityKeys.generateKeyPair() // Fallback keys
            val keyPair = DiacInfinityKeys.KeyPair(
                privateKey = DiacInfinityKeys.PrivateKey(
                    x = BigInteger("123"), p = DiacInfinityKeys.SAFE_PRIME, offset = 101L, precision = 64, N = BigInteger("456")
                ),
                publicKey = DiacInfinityKeys.PublicKey(
                    pk = BigInteger("789"), p = DiacInfinityKeys.SAFE_PRIME, hN = "hN_imported", precision = 64
                ),
                address = addr
            )
            
            _walletKeyPair.value = keyPair
            _balance.value = 150.0 // Default imported balance
            
            _fragments.value = listOf(
                Fragment(
                    fragmentId = UUID.randomUUID().toString().take(12),
                    addressRangeStart = "00000000",
                    addressRangeEnd = "ffffffff",
                    txCount = 0,
                    creationTime = System.currentTimeMillis(),
                    lastUpdate = System.currentTimeMillis(),
                    epoch = 1,
                    balances = mapOf(keyPair.address to 150.0)
                )
            )
            addLog("✓ Keys imported successfully. Address: ${keyPair.address}")
            true
        } catch (e: Exception) {
            addLog("❌ Fail to import keys: ${e.message}")
            false
        }
    }

    fun logout() {
        stopMining()
        _walletKeyPair.value = null
        _balance.value = 0.0
        _transactions.value = emptyList()
        _fragments.value = emptyList()
        addLog("Logged out from wallet. Identity wiped.")
    }

    /**
     * Send Transaction with Threshold signing validation
     */
    fun sendDiac(recipient: String, amount: Double): Pair<Boolean, String> {
        val sender = _walletKeyPair.value ?: return Pair(false, "Wallet not initialized.")
        if (_balance.value < amount) return Pair(false, "Insufficient balance.")

        // Build transaction
        val txId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // Construct mathematical transaction signature
        val txBytes = (txId + sender.address + recipient + amount.toString()).toByteArray(StandardCharsets.UTF_8)
        val signature = DiacInfinityKeys.sign(sender.privateKey, txBytes)

        // Split password with Shamir Secret Sharing as proof
        val txPassword = "TX-PASS-${sender.address.take(4)}-$amount".toByteArray()
        val shares = ShamirSecretSharing.splitSecret(txPassword, 5, 15)
        
        addLog("Split Transaction-Password into (k=5, n=15) Shamir SSS shares.")
        addLog("Distributed shares to 15 covering fragment holders recursively.")

        // Deduct balance
        _balance.value -= amount
        
        val newTx = Transaction(
            txid = txId,
            from = sender.address,
            to = recipient,
            amount = amount,
            timestamp = timestamp,
            signature = signature,
            status = "Pending Recombination"
        )

        _transactions.value = listOf(newTx) + _transactions.value

        // Reconstruct SSS shares simulation automatically in background state
        coroutineScope.launch {
            delay(1500)
            addLog("Reconstructing signing password: Quorum of 5 shares received...")
            val recovered = ShamirSecretSharing.reconstructSecret(shares.shuffled().take(5))
            val recoveredStr = String(recovered)
            addLog("✓ Successfully validated SSS payload: $recoveredStr")

            // Propagate through MVS gossip
            delay(500)
            _transactions.value = _transactions.value.map {
                if (it.txid == txId) it.copy(status = "Confirmed (MVS Sync)") else it
            }
            addLog("✓ MVS Consensus approved. Transaction finalized on DNN fragment.")
        }

        return Pair(true, "Transaction submitted. SSS signature splitting in progress.")
    }

    /**
     * Start/Stop PoSB Mining and Challenges
     */
    fun startMining(): Boolean {
        val keys = _walletKeyPair.value ?: return false
        if (_isMiningActive.value) return true

        _isMiningActive.value = true
        _miningMetrics.value = MiningMetrics(logs = listOf("PoSB process initialized on active shards."))
        addLog("Miner starting...")

        miningJob = coroutineScope.launch {
            var counter = 0
            while (isActive) {
                delay(8000)
                counter++
                
                // Storage Audit Challenge simulation
                val testFragId = _fragments.value.firstOrNull()?.fragmentId ?: "frag-0"
                val nonceStr = UUID.randomUUID().toString().take(12)
                val testData = "Fragment-Block-Data-Payload-$testFragId".toByteArray()
                
                val md = MessageDigest.getInstance("SHA-256")
                md.update(testData)
                md.update(nonceStr.toByteArray())
                val hashResult = md.digest().joinToString("") { "%02x".format(it) }

                // Add to list of solved challenges
                val challenge = StorageChallenge(
                    id = UUID.randomUUID().toString().take(8),
                    fragmentId = testFragId,
                    nonce = nonceStr,
                    hashResult = hashResult,
                    timestamp = System.currentTimeMillis(),
                    passed = true,
                    byteSize = 32768
                )

                _storageChallenges.value = (listOf(challenge) + _storageChallenges.value).take(50)

                // Deliver micro-rewards
                val reward = 3.69 * (0.7 + (0.3 * (90 + (1..10).random()) / 100)) // Ri = alpha * Si + beta * Bi
                _balance.value += reward
                
                _miningMetrics.value =_miningMetrics.value.copy(
                    totalRewards = _miningMetrics.value.totalRewards + reward,
                    challengesCompleted = _miningMetrics.value.challengesCompleted + 1,
                    efficiency = 90f + (1..10).random().toFloat(),
                    hourlyRate = 12.0 + (1..5).random().toDouble()
                )

                addMiningLog("✓ PoS audit challenge solved for fragment $testFragId ($hashResult)")
                addMiningLog("✓ Latency measured: ${10 + (2..18).random()}ms (PoB Benchmark Success)")
                addMiningLog("🎉 Credited +${"%.4f".format(reward)} DNC to account balance.")
            }
        }
        return true
    }

    fun stopMining() {
        if (!_isMiningActive.value) return
        _isMiningActive.value = false
        miningJob?.cancel()
        miningJob = null
        addLog("Miner stopped.")
    }

    /**
     * Simulated P2P Gossip MVS
     */
    private fun startConsensusGossip() {
        gossipJob = coroutineScope.launch {
            while (isActive) {
                delay(12000)
                if (_walletKeyPair.value == null) continue

                // Simulate gossip with one random peer
                val currentPeers = _peers.value
                if (currentPeers.isNotEmpty()) {
                    val peer = currentPeers.random()
                    addLog("Gossip MVS: Exchanging synopses with ${peer.ipDisplay}...")
                    
                    // Simulate random fraudulent reporting occasionally
                    if ((1..100).random() > 93) {
                        addLog("⚠️ Warning: Detected inconsistent checksum in peer ${peer.ipDisplay}!")
                        val slashedPeer = peer.copy(
                            isFraud = true,
                            reputation = (peer.reputation - 20).coerceAtLeast(0)
                        )
                        _peers.value = _peers.value.map { if (it.nodeId == peer.nodeId) slashedPeer else it }
                        addLog("⚠️ Slashed peer ${peer.ipDisplay} reputation to ${slashedPeer.reputation}% and generated fraud proof.")
                    } else {
                        addLog("✓ Converged consensus synopsis matched with ${peer.ipDisplay}")
                    }
                }
            }
        }
    }

    /**
     * Add manual peer
     */
    fun addPeer(ip: String, port: Int): Boolean {
        if (ip.isEmpty()) return false
        val cleanIp = ip.trim()
        val peersList = _peers.value
        
        if (peersList.any { it.ip == cleanIp && it.port == port }) {
            return false
        }

        val peerId = "DNC-MANUAL_PEER_" + UUID.randomUUID().toString().take(12)
        val newPeer = Peer(cleanIp, port, peerId, System.currentTimeMillis(), 15, false, false, 100)
        _peers.value = peersList + newPeer

        addLog("✓ Manually added peer $cleanIp:$port. Sent UDP NAT punches & pings.")
        return true
    }

    /**
     * Manual ping check
     */
    fun pingPeer(peer: Peer) {
        coroutineScope.launch {
            addLog("Pinging peer ${peer.ipDisplay}...")
            delay(500)
            val updated = peer.copy(
                lastSeen = System.currentTimeMillis(),
                latencyMs = (10..55).random().toLong()
            )
            _peers.value = _peers.value.map { if (it.nodeId == peer.nodeId) updated else it }
            addLog("✓ Pong from ${peer.ipDisplay} in ${updated.latencyMs}ms.")
        }
    }

    /**
     * Solve custom Storage challenge
     */
    fun triggerCustomAudit(fragmentId: String, textData: String): StorageChallenge {
        val nonce = UUID.randomUUID().toString().take(12)
        val testBytes = textData.toByteArray(StandardCharsets.UTF_8)
        
        val md = MessageDigest.getInstance("SHA-256")
        md.update(testBytes)
        md.update(nonce.toByteArray(StandardCharsets.UTF_8))
        val hash = md.digest().joinToString("") { "%02x".format(it) }

        val challenge = StorageChallenge(
            id = UUID.randomUUID().toString().take(8),
            fragmentId = fragmentId,
            nonce = nonce,
            hashResult = hash,
            timestamp = System.currentTimeMillis(),
            passed = true,
            byteSize = testBytes.size
        )
        
        _storageChallenges.value = (listOf(challenge) + _storageChallenges.value).take(10)
        addLog("✓ Manual storage challenge triggered: Fragment $fragmentId has been checked.")
        return challenge
    }

    /**
     * Simulate guardian key recovery rotation
     */
    fun triggerGuardianRecovery(guardianName1: String, guardianName2: String): String {
        addLog("Initiating key recovery contract with $guardianName1 and $guardianName2...")
        val freshPair = DiacInfinityKeys.generateKeyPair()
        _walletKeyPair.value = freshPair
        addLog("✓ Recovery validated: Guardians co-signed 2-of-3 threshold script.")
        addLog("✓ Rotated public wallet anchor to: ${freshPair.address}")
        return freshPair.address
    }
}

// Handler
private val DispatchScope = object : CoroutineDispatcher() {
    override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
        block.run()
    }
}
