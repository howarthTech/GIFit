package com.gifit.app.gif

import java.io.OutputStream

/**
 * Low-level GIF89a binary format writer.
 */
object GifWriter {

    fun writeHeader(out: OutputStream) {
        out.write("GIF89a".toByteArray(Charsets.US_ASCII))
    }

    fun writeLogicalScreenDescriptor(
        out: OutputStream,
        width: Int,
        height: Int,
        colorTableSize: Int = 256
    ) {
        writeShort(out, width)
        writeShort(out, height)

        val colorResolution = 7 // 8 bits per primary color
        val sortFlag = 0
        val sizeOfTable = log2(colorTableSize) - 1
        val packed = 0x80 or // Global color table flag
                (colorResolution shl 4) or
                (sortFlag shl 3) or
                sizeOfTable
        out.write(packed)
        out.write(0) // Background color index
        out.write(0) // Pixel aspect ratio
    }

    fun writeColorTable(out: OutputStream, palette: ByteArray, size: Int = 256) {
        out.write(palette, 0, minOf(palette.size, size * 3))
        // Pad if needed
        val written = minOf(palette.size, size * 3)
        for (i in written until size * 3) {
            out.write(0)
        }
    }

    fun writeNetscapeExtension(out: OutputStream, loops: Int = 0) {
        out.write(0x21) // Extension introducer
        out.write(0xFF) // Application extension
        out.write(11)   // Block size
        out.write("NETSCAPE2.0".toByteArray(Charsets.US_ASCII))
        out.write(3)    // Sub-block size
        out.write(1)    // Loop sub-block ID
        writeShort(out, loops) // 0 = infinite loop
        out.write(0)    // Block terminator
    }

    fun writeGraphicControlExtension(out: OutputStream, delayCs: Int, dispose: Int = 0) {
        out.write(0x21) // Extension introducer
        out.write(0xF9) // Graphic control label
        out.write(4)    // Block size
        val packed = (dispose and 0x07) shl 2
        out.write(packed)
        writeShort(out, delayCs)
        out.write(0) // Transparent color index
        out.write(0) // Block terminator
    }

    fun writeImageDescriptor(out: OutputStream, width: Int, height: Int) {
        out.write(0x2C) // Image separator
        writeShort(out, 0) // Left
        writeShort(out, 0) // Top
        writeShort(out, width)
        writeShort(out, height)
        out.write(0) // Packed field: no local color table, not interlaced
    }

    fun writeTrailer(out: OutputStream) {
        out.write(0x3B)
    }

    private fun writeShort(out: OutputStream, value: Int) {
        out.write(value and 0xFF)
        out.write((value shr 8) and 0xFF)
    }

    private fun log2(value: Int): Int {
        var v = value
        var log = 0
        while (v > 1) {
            v = v shr 1
            log++
        }
        return log
    }
}
