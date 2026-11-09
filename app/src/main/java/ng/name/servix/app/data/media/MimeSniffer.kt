package ng.name.servix.app.data.media

/**
 * What a file actually is, decided from its first bytes.
 *
 * Android hands a picked file over with `null`, `""` or `application/octet-stream` surprisingly often
 * (WhatsApp downloads, some scanners, some OEM pickers), and the API rejects an upload whose
 * `Content-Type` is not on the per-kind allow-list (`uploads.ts:56-62`). The web client hit exactly
 * this bug, so the type comes from the bytes and the file name is only used to pick a sensible
 * extension.
 */
object MimeSniffer {

    data class Detected(val mime: String, val extension: String)

    fun detect(bytes: ByteArray, fallbackName: String = ""): Detected? {
        magic(bytes)?.let { return it }
        // No magic number matched: a text file is the only remaining thing we can prove.
        if (looksLikeText(bytes)) {
            val name = fallbackName.lowercase()
            return when {
                name.endsWith(".csv") -> Detected("text/csv", "csv")
                else -> Detected("text/plain", "txt")
            }
        }
        return null
    }

    private fun magic(bytes: ByteArray): Detected? {
        if (bytes.size >= 3 && u(bytes, 0) == 0xFF && u(bytes, 1) == 0xD8 && u(bytes, 2) == 0xFF) {
            return Detected("image/jpeg", "jpg")
        }
        if (bytes.size >= 8 && u(bytes, 0) == 0x89 && ascii(bytes, 1, 3) == "PNG") {
            return Detected("image/png", "png")
        }
        if (bytes.size >= 6 && ascii(bytes, 0, 3) == "GIF") return Detected("image/gif", "gif")
        if (bytes.size >= 12 && ascii(bytes, 0, 4) == "RIFF" && ascii(bytes, 8, 4) == "WEBP") {
            return Detected("image/webp", "webp")
        }
        if (bytes.size >= 12 && ascii(bytes, 0, 4) == "RIFF" && ascii(bytes, 8, 4) == "WAVE") {
            return Detected("audio/wav", "wav")
        }
        if (bytes.size >= 5 && ascii(bytes, 0, 4) == "%PDF") return Detected("application/pdf", "pdf")
        if (bytes.size >= 4 && ascii(bytes, 0, 4) == "OggS") return Detected("audio/ogg", "ogg")
        // EBML — what MediaRecorder and WhatsApp voice notes produce.
        if (bytes.size >= 4 && u(bytes, 0) == 0x1A && u(bytes, 1) == 0x45 && u(bytes, 2) == 0xDF && u(bytes, 3) == 0xA3) {
            return Detected("audio/webm", "webm")
        }
        if (bytes.size >= 12 && ascii(bytes, 4, 4) == "ftyp") return isoBmff(bytes)
        if (bytes.size >= 3 && ascii(bytes, 0, 3) == "ID3") return Detected("audio/mpeg", "mp3")
        if (bytes.size >= 2 && u(bytes, 0) == 0xFF && (u(bytes, 1) and 0xE0) == 0xE0) {
            return Detected("audio/mpeg", "mp3")
        }
        return null
    }

    /**
     * MP4 family. `ftyp` brands: `M4A `/`mp42`+`audio` are voice notes, `isom`/`mp42` with a video
     * track is a video we do not accept for chat, and `qt  ` is QuickTime.
     */
    private fun isoBmff(bytes: ByteArray): Detected {
        val brand = ascii(bytes, 8, 4).trim()
        val compatible = if (bytes.size >= 24) ascii(bytes, 16, 4).trim() else ""
        return when {
            brand.startsWith("M4A") || compatible.startsWith("M4A") -> Detected("audio/mp4", "m4a")
            brand == "qt" -> Detected("audio/mp4", "m4a")
            else -> Detected("audio/mp4", "m4a")
        }
    }

    /** Printable ASCII/UTF-8 with no control bytes: a text or CSV file. */
    private fun looksLikeText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val sample = bytes.take(1024)
        var printable = 0
        for (b in sample) {
            val v = b.toInt() and 0xFF
            when {
                v == 0x09 || v == 0x0A || v == 0x0D -> printable++
                v in 0x20..0x7E -> printable++
                v >= 0xC2 -> printable++ // multi-byte UTF-8 lead/continuation
            }
        }
        return printable >= sample.size * 95 / 100
    }

    private fun u(bytes: ByteArray, index: Int): Int = bytes[index].toInt() and 0xFF

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String {
        if (offset + length > bytes.size) return ""
        return String(bytes, offset, length, Charsets.US_ASCII)
    }
}
