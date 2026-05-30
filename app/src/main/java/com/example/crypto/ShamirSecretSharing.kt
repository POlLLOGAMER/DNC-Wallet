package com.example.crypto

import java.security.SecureRandom

object ShamirSecretSharing {

    private val random = SecureRandom()

    private fun gf256Add(a: Int, b: Int): Int {
        return (a xor b) and 0xFF
    }

    private fun gf256Mul(a: Int, b: Int): Int {
        var res = 0
        var tempA = a and 0xFF
        var tempB = b and 0xFF
        while (tempB > 0) {
            if ((tempB and 1) != 0) {
                res = res xor tempA
            }
            tempA = tempA shl 1
            if ((tempA and 0x100) != 0) {
                tempA = tempA xor 0x11b // Irreducible polynomial x^8 + x^4 + x^3 + x + 1
            }
            tempB = tempB ushr 1
        }
        return res and 0xFF
    }

    private fun gf256Inverse(a: Int): Int {
        if (a == 0) throw IllegalArgumentException("Cannot compute inverse of 0")
        var result = 1
        var power = a and 0xFF
        var exponent = 254 // 256 - 2

        while (exponent > 0) {
            if ((exponent and 1) != 0) {
                result = gf256Mul(result, power)
            }
            power = gf256Mul(power, power)
            exponent = exponent ushr 1
        }
        return result and 0xFF
    }

    private fun gf256Div(a: Int, b: Int): Int {
        return gf256Mul(a, gf256Inverse(b))
    }

    private fun lagrangeInterpolateAtZero(points: List<Pair<Int, Int>>): Int {
        var secret = 0
        for (i in points.indices) {
            val (xi, yi) = points[i]
            var numerator = 1
            var denominator = 1

            for (j in points.indices) {
                if (i != j) {
                    val xj = points[j].first
                    numerator = gf256Mul(numerator, xj)
                    denominator = gf256Mul(denominator, gf256Add(xi, xj))
                }
            }

            if (denominator == 0) continue
            val lagrangeCoef = gf256Div(numerator, denominator)
            secret = gf256Add(secret, gf256Mul(yi, lagrangeCoef))
        }
        return secret
    }

    /**
     * Splits a multi-byte secret into n shares with threshold k.
     * Prepend each returned share's first byte with the index x (1-based index).
     */
    fun splitSecret(secret: ByteArray, k: Int, n: Int): List<ByteArray> {
        require(k in 1..n && n <= 255) { "Invalid k ($k) and n ($n) parameters" }
        require(secret.isNotEmpty()) { "Secret cannot be empty" }

        val shares = List(n) { ByteArray(secret.size + 1) }
        
        // Populate share indices (1-based)
        for (i in 0 until n) {
            shares[i][0] = (i + 1).toByte()
        }

        for (byteIdx in secret.indices) {
            val secretByte = secret[byteIdx].toInt() and 0xFF
            
            // Polynomial coefficients coefficients[0] = secretByte, and rest are random integers in GF(256)
            val coefficients = IntArray(k)
            coefficients[0] = secretByte
            for (j in 1 until k) {
                coefficients[j] = random.nextInt(256)
            }

            // Evaluate polynomial at points 1..n
            for (i in 1..n) {
                var value = 0
                var xPower = 1
                for (coef in coefficients) {
                    value = gf256Add(value, gf256Mul(coef, xPower))
                    xPower = gf256Mul(xPower, i)
                }
                shares[i - 1][byteIdx + 1] = value.toByte()
            }
        }
        return shares
    }

    /**
     * Reconstructs the multi-byte secret from a list of valid shares.
     */
    fun reconstructSecret(shares: List<ByteArray>): ByteArray {
        require(shares.isNotEmpty()) { "No shares available for reconstruction" }
        val secretLength = shares[0].size - 1
        require(secretLength >= 0) { "Invalid empty share length" }

        val secret = ByteArray(secretLength)
        for (byteIdx in 0 until secretLength) {
            val points = shares.map { share ->
                val x = share[0].toInt() and 0xFF
                val y = share[byteIdx + 1].toInt() and 0xFF
                Pair(x, y)
            }
            secret[byteIdx] = lagrangeInterpolateAtZero(points).toByte()
        }
        return secret
    }
}
