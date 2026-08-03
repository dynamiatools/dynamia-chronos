package tools.dynamia.chronos.domain;

import org.junit.jupiter.api.Test;
import tools.dynamia.chronos.HeadersProvider;
import tools.dynamia.chronos.ParametersProvider;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestItemTest {

    @Test
    void implementsHeadersAndParametersProvider() {
        RequestItem item = new RequestItem();
        assertTrue(item instanceof HeadersProvider);
        assertTrue(item instanceof ParametersProvider);
    }

    @Test
    void headersRoundTripThroughJson() {
        RequestItem item = new RequestItem();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Api-Key", "abc123");

        item.setHeaders(headers);

        assertEquals(headers, item.getHeaders());
    }

    @Test
    void settingEmptyHeadersMapClearsPersistedHeaders() {
        RequestItem item = new RequestItem();
        item.setHeaders(Map.of("X-Api-Key", "abc123"));
        assertFalse(item.getHeaders().isEmpty());

        item.setHeaders(Map.of());

        assertEquals(Map.of(), item.getHeaders());
    }

    @Test
    void parametersRoundTripThroughJson() {
        RequestItem item = new RequestItem();
        Map<String, String> params = new HashMap<>();
        params.put("id", "42");

        item.setParameters(params);

        assertEquals(params, item.getParameters());
    }

    @Test
    void settingEmptyParametersMapClearsPersistedParameters() {
        RequestItem item = new RequestItem();
        item.setParameters(Map.of("id", "42"));
        assertFalse(item.getParameters().isEmpty());

        item.setParameters(Map.of());

        assertEquals(Map.of(), item.getParameters());
    }
}
