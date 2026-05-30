package com.example.model

/**
 * Peer status and reputation in DNC.
 */
data class Peer(
    val ip: String,
    val port: Int,
    val nodeId: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0,
    val isSelf: Boolean = false,
    val isFraud: Boolean = false,
    val reputation: Int = 100
) {
    val addressDisplay: String get() = if (nodeId.length > 20) "${nodeId.take(10)}...${nodeId.takeLast(10)}" else nodeId
    val ipDisplay: String get() = "$ip:$port"
}

/**
 * State snapshot description exchanged for MVS (Majority-Value Synchronisation).
 */
data class Synopsis(
    val txCount: Int,
    val balance: Double,
    val digest: String,
    val epoch: Int,
    val timestamp: Long,
    val nodeId: String
)

/**
 * DNC Transaction.
 */
data class Transaction(
    val txid: String,
    val from: String,
    val to: String,
    val amount: Double,
    val timestamp: Long,
    val signature: String,
    val status: String = "Confirmed",
    val passId: String = ""
) {
    val fromDisplay: String get() = if (from.length > 16) "${from.take(8)}...${from.takeLast(8)}" else from
    val toDisplay: String get() = if (to.length > 16) "${to.take(8)}...${to.takeLast(8)}" else to
}

/**
 * DNN (Distributive Network of Networks) representation where a node holds partial fragments.
 */
data class Fragment(
    val fragmentId: String,
    val addressRangeStart: String,
    val addressRangeEnd: String,
    val txCount: Int,
    val creationTime: Long,
    val lastUpdate: Long,
    val epoch: Int,
    val balances: Map<String, Double> = emptyMap(),
    val transactions: List<Transaction> = emptyList()
) {
    fun containsAddress(address: String): Boolean {
        // Simple hash check comparison
        val addressHash = address.hashCode().toString(16)
        return addressHash >= addressRangeStart && addressHash <= addressRangeEnd
    }
}

/**
 * Proof of Storage audits.
 */
data class StorageChallenge(
    val id: String,
    val fragmentId: String,
    val nonce: String,
    val hashResult: String,
    val timestamp: Long,
    val passed: Boolean,
    val byteSize: Int
)

/**
 * DNC Guardian for key recovery.
 */
data class Guardian(
    val name: String,
    val address: String,
    val isRegistered: Boolean = false
)
