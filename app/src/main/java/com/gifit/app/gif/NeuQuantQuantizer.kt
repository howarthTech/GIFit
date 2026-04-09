package com.gifit.app.gif

/**
 * NeuQuant Neural-Net Quantization Algorithm
 * Based on Anthony Dekker's 1994 algorithm for reducing 24-bit images to 256 colors.
 * Trains a Kohonen self-organizing neural network on pixel data.
 */
class NeuQuantQuantizer(
    private val pixels: ByteArray,
    private val pixelCount: Int,
    private val sampleFactor: Int = 10
) {
    companion object {
        private const val NET_SIZE = 256
        private const val PRIME1 = 499
        private const val PRIME2 = 491
        private const val PRIME3 = 487
        private const val PRIME4 = 503
        private const val MIN_PICTURE_BYTES = 3 * PRIME4

        private const val MAX_NET_POS = NET_SIZE - 1
        private const val NET_BIAS_SHIFT = 4
        private const val N_CYCLES = 100

        private const val INT_BIAS_SHIFT = 16
        private const val INT_BIAS = 1 shl INT_BIAS_SHIFT
        private const val GAMMA_SHIFT = 10
        private const val BETA_SHIFT = 10
        private const val BETA = INT_BIAS shr BETA_SHIFT
        private const val BETA_GAMMA = INT_BIAS shl (GAMMA_SHIFT - BETA_SHIFT)

        private const val INIT_RAD = NET_SIZE shr 3
        private const val RADIUS_BIAS_SHIFT = 6
        private const val RADIUS_BIAS = 1 shl RADIUS_BIAS_SHIFT
        private const val INIT_RADIUS = INIT_RAD * RADIUS_BIAS
        private const val RADIUS_DEC = 30

        private const val ALPHA_BIAS_SHIFT = 10
        private const val INIT_ALPHA = 1 shl ALPHA_BIAS_SHIFT
    }

    private val network = Array(NET_SIZE) { i ->
        val v = (i shl (NET_BIAS_SHIFT + 8)) / NET_SIZE
        intArrayOf(v, v, v, 0)
    }

    private val netIndex = IntArray(256)
    private val bias = IntArray(NET_SIZE)
    private val freq = IntArray(NET_SIZE) { INT_BIAS / NET_SIZE }

    fun process(): ByteArray {
        if (pixelCount < MIN_PICTURE_BYTES) {
            // Too few pixels - just use first pixel repeated
            val palette = ByteArray(NET_SIZE * 3)
            for (i in 0 until NET_SIZE) {
                palette[i * 3] = if (pixels.size >= 3) pixels[2] else 0
                palette[i * 3 + 1] = if (pixels.size >= 3) pixels[1] else 0
                palette[i * 3 + 2] = if (pixels.size >= 3) pixels[0] else 0
            }
            return palette
        }
        learn()
        unbiasNet()
        buildIndex()
        return colorMap()
    }

    private fun learn() {
        val alphadec = 30 + ((sampleFactor - 1) / 3)
        val lengthCount = pixelCount / 3
        val samplePixels = lengthCount / sampleFactor
        var alpha = INIT_ALPHA
        var radius = INIT_RADIUS

        var rad = radius shr RADIUS_BIAS_SHIFT
        if (rad <= 1) rad = 0
        val radSq = IntArray(rad) { it * it }
        val radPower = IntArray(rad) { i ->
            alpha * (radSq[rad - 1] - radSq[i]) / radSq[rad - 1]
        }

        val step = when {
            lengthCount % PRIME1 != 0 -> PRIME1 * 3
            lengthCount % PRIME2 != 0 -> PRIME2 * 3
            lengthCount % PRIME3 != 0 -> PRIME3 * 3
            else -> PRIME4 * 3
        }

        var pixelIndex = 0
        for (i in 0 until samplePixels) {
            val b = (pixels[pixelIndex].toInt() and 0xFF) shl NET_BIAS_SHIFT
            val g = (pixels[pixelIndex + 1].toInt() and 0xFF) shl NET_BIAS_SHIFT
            val r = (pixels[pixelIndex + 2].toInt() and 0xFF) shl NET_BIAS_SHIFT

            val bestBiasPos = contest(b, g, r)
            alterSingle(alpha, bestBiasPos, b, g, r)
            if (rad != 0) alterNeighbors(rad, radPower, bestBiasPos, b, g, r)

            pixelIndex += step
            while (pixelIndex >= pixelCount) pixelIndex -= pixelCount

            if (i % (samplePixels / N_CYCLES) == 0) {
                alpha -= alpha / alphadec
                radius -= radius / RADIUS_DEC
                rad = radius shr RADIUS_BIAS_SHIFT
                if (rad <= 1) rad = 0
                for (j in 0 until rad) {
                    radPower[j] = alpha * (rad * rad - j * j) / (rad * rad)
                }
            }
        }
    }

    private fun contest(b: Int, g: Int, r: Int): Int {
        var bestDist = Int.MAX_VALUE
        var bestBiasDist = Int.MAX_VALUE
        var bestPos = 0
        var bestBiasPos = 0

        for (i in 0 until NET_SIZE) {
            val n = network[i]
            var dist = Math.abs(n[0] - b) + Math.abs(n[1] - g) + Math.abs(n[2] - r)
            if (dist < bestDist) {
                bestDist = dist
                bestPos = i
            }
            val biasDist = dist - (bias[i] shr (INT_BIAS_SHIFT - NET_BIAS_SHIFT))
            if (biasDist < bestBiasDist) {
                bestBiasDist = biasDist
                bestBiasPos = i
            }
            val betaFreq = freq[i] shr BETA_SHIFT
            freq[i] -= betaFreq
            bias[i] += betaFreq shl GAMMA_SHIFT
        }
        freq[bestPos] += BETA
        bias[bestPos] -= BETA_GAMMA
        return bestBiasPos
    }

    private fun alterSingle(alpha: Int, i: Int, b: Int, g: Int, r: Int) {
        val n = network[i]
        n[0] -= (alpha * (n[0] - b)) / INIT_ALPHA
        n[1] -= (alpha * (n[1] - g)) / INIT_ALPHA
        n[2] -= (alpha * (n[2] - r)) / INIT_ALPHA
    }

    private fun alterNeighbors(rad: Int, radPower: IntArray, i: Int, b: Int, g: Int, r: Int) {
        var lo = i - rad
        if (lo < -1) lo = -1
        var hi = i + rad
        if (hi > NET_SIZE) hi = NET_SIZE

        var j = i + 1
        var k = i - 1
        var m = 1
        while (j < hi || k > lo) {
            val a = radPower[m++]
            if (j < hi) {
                val n = network[j++]
                n[0] -= (a * (n[0] - b)) / INIT_ALPHA
                n[1] -= (a * (n[1] - g)) / INIT_ALPHA
                n[2] -= (a * (n[2] - r)) / INIT_ALPHA
            }
            if (k > lo) {
                val n = network[k--]
                n[0] -= (a * (n[0] - b)) / INIT_ALPHA
                n[1] -= (a * (n[1] - g)) / INIT_ALPHA
                n[2] -= (a * (n[2] - r)) / INIT_ALPHA
            }
        }
    }

    private fun unbiasNet() {
        for (i in 0 until NET_SIZE) {
            network[i][0] = (network[i][0] + (1 shl (NET_BIAS_SHIFT - 1))) shr NET_BIAS_SHIFT
            network[i][1] = (network[i][1] + (1 shl (NET_BIAS_SHIFT - 1))) shr NET_BIAS_SHIFT
            network[i][2] = (network[i][2] + (1 shl (NET_BIAS_SHIFT - 1))) shr NET_BIAS_SHIFT
            network[i][0] = network[i][0].coerceIn(0, 255)
            network[i][1] = network[i][1].coerceIn(0, 255)
            network[i][2] = network[i][2].coerceIn(0, 255)
            network[i][3] = i // Record original index
        }
    }

    private fun buildIndex() {
        var previousColor = 0
        var startPos = 0
        for (i in 0 until NET_SIZE) {
            var smallPos = i
            var smallVal = network[i][1] // Green channel
            for (j in i + 1 until NET_SIZE) {
                if (network[j][1] < smallVal) {
                    smallPos = j
                    smallVal = network[j][1]
                }
            }
            if (i != smallPos) {
                val temp = network[i]
                network[i] = network[smallPos]
                network[smallPos] = temp
            }
            if (smallVal != previousColor) {
                netIndex[previousColor] = (startPos + i) shr 1
                for (j in previousColor + 1 until smallVal) {
                    netIndex[j] = i
                }
                previousColor = smallVal
                startPos = i
            }
        }
        netIndex[previousColor] = (startPos + MAX_NET_POS) shr 1
        for (j in previousColor + 1..255) {
            netIndex[j] = MAX_NET_POS
        }
    }

    fun map(b: Int, g: Int, r: Int): Int {
        var bestDist = 1000
        var best = -1
        var i = netIndex[g]
        var j = i - 1

        while (i < NET_SIZE || j >= 0) {
            if (i < NET_SIZE) {
                val n = network[i]
                var dist = n[1] - g
                if (dist >= bestDist) {
                    i = NET_SIZE // Stop searching forward
                } else {
                    i++
                    if (dist < 0) dist = -dist
                    dist += Math.abs(n[0] - b)
                    if (dist < bestDist) {
                        dist += Math.abs(n[2] - r)
                        if (dist < bestDist) {
                            bestDist = dist
                            best = i - 1
                        }
                    }
                }
            }
            if (j >= 0) {
                val n = network[j]
                var dist = g - n[1]
                if (dist >= bestDist) {
                    j = -1 // Stop searching backward
                } else {
                    j--
                    if (dist < 0) dist = -dist
                    dist += Math.abs(n[0] - b)
                    if (dist < bestDist) {
                        dist += Math.abs(n[2] - r)
                        if (dist < bestDist) {
                            bestDist = dist
                            best = j + 1
                        }
                    }
                }
            }
        }
        return if (best < 0) 0 else best
    }

    fun colorMap(): ByteArray {
        val palette = ByteArray(NET_SIZE * 3)
        for (i in 0 until NET_SIZE) {
            palette[i * 3] = network[i][2].toByte()     // R
            palette[i * 3 + 1] = network[i][1].toByte() // G
            palette[i * 3 + 2] = network[i][0].toByte() // B
        }
        return palette
    }
}
