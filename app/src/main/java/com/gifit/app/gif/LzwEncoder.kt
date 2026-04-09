package com.gifit.app.gif

import java.io.OutputStream

/**
 * LZW encoder for GIF image data.
 * Clean implementation following the GIF89a specification.
 */
class LzwEncoder(
    private val pixels: IntArray,
    private val colorDepth: Int
) {
    companion object {
        private const val MAX_CODE_SIZE = 12
        private const val MAX_TABLE_SIZE = 1 shl MAX_CODE_SIZE  // 4096
        private const val BLOCK_SIZE = 255
    }

    private val minCodeSize = maxOf(2, colorDepth)
    private val clearCode = 1 shl minCodeSize
    private val eoiCode = clearCode + 1

    // LZW dictionary: maps (prefix_code, pixel) -> new_code
    private val prefixTable = IntArray(MAX_TABLE_SIZE)
    private val suffixTable = IntArray(MAX_TABLE_SIZE)
    private val hashKeys = IntArray(MAX_TABLE_SIZE)

    private var nextCode = 0
    private var codeSize = 0

    // Bit packing state
    private var bitBuffer = 0
    private var bitsInBuffer = 0

    // Sub-block output buffer
    private val blockBuffer = ByteArray(256)
    private var blockIndex = 0

    fun encode(output: OutputStream) {
        // Write minimum code size
        output.write(minCodeSize)

        // Initialize
        initTable()
        emitCode(clearCode, output)

        if (pixels.isEmpty()) {
            emitCode(eoiCode, output)
            flushBits(output)
            flushBlock(output)
            output.write(0)
            return
        }

        var prefix = pixels[0]

        for (i in 1 until pixels.size) {
            val suffix = pixels[i]
            val existing = findEntry(prefix, suffix)

            if (existing >= 0) {
                // Found in table - extend the pattern
                prefix = existing
            } else {
                // Not found - emit current prefix and add new entry
                emitCode(prefix, output)

                if (nextCode < MAX_TABLE_SIZE) {
                    addEntry(prefix, suffix)
                    // Increase code size if needed BEFORE we might use the new code
                    if (nextCode > (1 shl codeSize)) {
                        codeSize++
                    }
                } else {
                    // Table full - clear and restart
                    emitCode(clearCode, output)
                    initTable()
                }

                prefix = suffix
            }
        }

        // Emit the final prefix
        emitCode(prefix, output)
        emitCode(eoiCode, output)
        flushBits(output)
        flushBlock(output)
        output.write(0)  // Block terminator
    }

    private fun initTable() {
        hashKeys.fill(-1)
        nextCode = eoiCode + 1
        codeSize = minCodeSize + 1

        // Initialize single-character entries (0..clearCode-1)
        // These are implicit - findEntry handles them by returning the pixel value directly
    }

    private fun findEntry(prefix: Int, suffix: Int): Int {
        val key = (prefix shl 12) or suffix
        var index = (key xor (key shr 5)) and (MAX_TABLE_SIZE - 1)

        while (true) {
            if (hashKeys[index] == -1) return -1
            if (hashKeys[index] == key) return prefixTable[index]
            index = (index + 1) and (MAX_TABLE_SIZE - 1)
        }
    }

    private fun addEntry(prefix: Int, suffix: Int) {
        val key = (prefix shl 12) or suffix
        var index = (key xor (key shr 5)) and (MAX_TABLE_SIZE - 1)

        while (hashKeys[index] != -1) {
            index = (index + 1) and (MAX_TABLE_SIZE - 1)
        }

        hashKeys[index] = key
        prefixTable[index] = nextCode
        suffixTable[index] = suffix
        nextCode++
    }

    private fun emitCode(code: Int, output: OutputStream) {
        bitBuffer = bitBuffer or (code shl bitsInBuffer)
        bitsInBuffer += codeSize

        while (bitsInBuffer >= 8) {
            writeByteToBlock((bitBuffer and 0xFF).toByte(), output)
            bitBuffer = bitBuffer ushr 8
            bitsInBuffer -= 8
        }
    }

    private fun flushBits(output: OutputStream) {
        if (bitsInBuffer > 0) {
            writeByteToBlock((bitBuffer and 0xFF).toByte(), output)
            bitBuffer = 0
            bitsInBuffer = 0
        }
    }

    private fun writeByteToBlock(b: Byte, output: OutputStream) {
        blockBuffer[blockIndex++] = b
        if (blockIndex >= BLOCK_SIZE) {
            flushBlock(output)
        }
    }

    private fun flushBlock(output: OutputStream) {
        if (blockIndex > 0) {
            output.write(blockIndex)
            output.write(blockBuffer, 0, blockIndex)
            blockIndex = 0
        }
    }
}
