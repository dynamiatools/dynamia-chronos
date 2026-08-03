package tools.dynamia.chronos;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChronosHttpResponseTest {

    @Test
    void sizeIsZeroWhenResponseIsNull() {
        ChronosHttpResponse response = new ChronosHttpResponse();
        assertEquals(0, response.getSize());
    }

    @Test
    void sizeMatchesUtf8ByteLength() {
        ChronosHttpResponse response = new ChronosHttpResponse();
        String body = "café éè"; // includes multi-byte UTF-8 chars
        response.setResponse(body);

        assertEquals(body.getBytes(StandardCharsets.UTF_8).length, response.getSize());
    }
}
