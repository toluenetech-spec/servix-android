package ng.name.servix.app.media

import ng.name.servix.app.data.media.MimeSniffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The type of an upload is decided from its bytes, never from what the picker claims
 * (owner: this was a real bug on the web — files arrive as `null` or `application/octet-stream`).
 */
class MimeSnifferTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private fun prefix(text: String) = text.toByteArray(Charsets.US_ASCII)

    @Test
    fun `jpeg is recognised by its marker`() {
        assertEquals("image/jpeg", MimeSniffer.detect(bytes(0xFF, 0xD8, 0xFF, 0xE0, 0x00, 0x10))?.mime)
    }

    @Test
    fun `png is recognised even when the name says jpg`() {
        val png = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(8)
        assertEquals("image/png", MimeSniffer.detect(png, fallbackName = "photo.jpg")?.mime)
        assertEquals("png", MimeSniffer.detect(png)?.extension)
    }

    @Test
    fun `gif and webp are distinguished`() {
        assertEquals("image/gif", MimeSniffer.detect(prefix("GIF89a") + ByteArray(4))?.mime)
        val webp = prefix("RIFF") + bytes(0, 0, 0, 0) + prefix("WEBP")
        assertEquals("image/webp", MimeSniffer.detect(webp)?.mime)
    }

    @Test
    fun `wave is audio while webp is an image even though both are RIFF`() {
        val wav = prefix("RIFF") + bytes(0, 0, 0, 0) + prefix("WAVE")
        assertEquals("audio/wav", MimeSniffer.detect(wav)?.mime)
    }

    @Test
    fun `pdf and ogg are recognised`() {
        assertEquals("application/pdf", MimeSniffer.detect(prefix("%PDF-1.7"))?.mime)
        assertEquals("audio/ogg", MimeSniffer.detect(prefix("OggS") + ByteArray(8))?.mime)
    }

    @Test
    fun `the EBML header used by MediaRecorder is a webm voice note`() {
        assertEquals("audio/webm", MimeSniffer.detect(bytes(0x1A, 0x45, 0xDF, 0xA3, 0x01))?.mime)
    }

    @Test
    fun `an mp4 container with an M4A brand is audio, not video`() {
        val m4a = bytes(0, 0, 0, 0x20) + prefix("ftypM4A ") + ByteArray(4) + prefix("isomM4A ")
        assertEquals("audio/mp4", MimeSniffer.detect(m4a)?.mime)
        assertEquals("m4a", MimeSniffer.detect(m4a)?.extension)
    }

    @Test
    fun `mp3 is recognised by an ID3 tag or a frame sync`() {
        assertEquals("audio/mpeg", MimeSniffer.detect(prefix("ID3") + ByteArray(5))?.mime)
        assertEquals("audio/mpeg", MimeSniffer.detect(bytes(0xFF, 0xFB, 0x90, 0x64))?.mime)
    }

    @Test
    fun `plain text is text and a dot-csv name makes it csv`() {
        assertEquals("text/plain", MimeSniffer.detect(prefix("hello there, this is a note"))?.mime)
        assertEquals("text/csv", MimeSniffer.detect(prefix("name,price\nlogo,350000"), fallbackName = "quote.csv")?.mime)
    }

    @Test
    fun `random binary with no signature is refused rather than mislabelled`() {
        assertNull(MimeSniffer.detect(bytes(0x00, 0x01, 0x02, 0x03, 0x7F, 0x00, 0x11)))
    }

    @Test
    fun `an empty file is not a text file`() {
        assertNull(MimeSniffer.detect(ByteArray(0)))
    }

    @Test
    fun `every detected chat image type is on the API allow-list`() {
        val allowed = ng.name.servix.app.data.model.ChatUploadKind.IMAGE_TYPES
        val samples = listOf(
            bytes(0xFF, 0xD8, 0xFF, 0xE0) to "image/jpeg",
            bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) to "image/png",
            prefix("GIF87a").toByteArray() to "image/gif",
        )
        samples.forEach { (sample, expected) ->
            val detected = MimeSniffer.detect(sample)
            assertEquals(expected, detected?.mime)
            assertTrue("$expected must be accepted by /uploads", detected!!.mime in allowed)
        }
    }
}
