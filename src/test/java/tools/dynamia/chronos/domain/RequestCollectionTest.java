package tools.dynamia.chronos.domain;

import org.junit.jupiter.api.Test;
import tools.dynamia.chronos.HeadersProvider;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestCollectionTest {

    @Test
    void implementsHeadersProvider() {
        assertTrue(new RequestCollection() instanceof HeadersProvider);
    }

    @Test
    void headersRoundTripThroughJson() {
        RequestCollection collection = new RequestCollection();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", "abc123");

        collection.setHeaders(headers);

        assertEquals(headers, collection.getHeaders());
    }

    @Test
    void settingEmptyHeadersMapClearsPersistedHeaders() {
        RequestCollection collection = new RequestCollection();
        collection.setHeaders(Map.of("X-Api-Key", "abc123"));
        assertFalse(collection.getHeaders().isEmpty());

        collection.setHeaders(Map.of());

        assertEquals(Map.of(), collection.getHeaders());
    }
}
