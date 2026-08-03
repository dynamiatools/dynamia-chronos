package tools.dynamia.chronos;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.dynamia.chronos.domain.RequestItem;
import tools.dynamia.chronos.domain.Variable;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChronosHttpRequestExecutorTest {

    private HttpServer server;
    private int port;
    private volatile Headers lastRequestHeaders;
    private volatile String lastRequestBody;
    private volatile String lastRequestPath;
    private volatile int responseStatus = 200;
    private volatile String responseBody = "{\"ok\":true}";

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/", exchange -> {
            lastRequestHeaders = exchange.getRequestHeaders();
            lastRequestPath = exchange.getRequestURI().toString();
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseStatus, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + port;
    }

    @Test
    void executesSuccessfulGetRequest() {
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl() + "/status");

        var response = new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertEquals(200, response.getStatusCode());
        assertTrue(response.isExecuted());
        assertFalse(response.isFail());
        assertEquals("{\"ok\":true}", response.getResponse());
    }

    @Test
    void treatsCreatedStatusAsSuccess() {
        responseStatus = 201;
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl());

        var response = new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertEquals(201, response.getStatusCode());
        assertTrue(response.isExecuted(), "2xx responses other than 200 should be treated as success");
        assertFalse(response.isFail());
    }

    @Test
    void treatsServerErrorAsFailure() {
        responseStatus = 500;
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl());

        var response = new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertTrue(response.isFail());
        assertFalse(response.isExecuted());
    }

    @Test
    void sendsCustomHeadersFromHeadersProvider() {
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl());
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Custom-Header", "custom-value");
        request.setHeaders(headers);

        new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertEquals("custom-value", lastRequestHeaders.getFirst("X-Custom-Header"));
    }

    @Test
    void sendsAuthorizationAndContentTypeHeaders() {
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl());
        request.setServerAuthorization("Bearer abc123");
        request.setContentType("application/xml");

        new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertEquals("Bearer abc123", lastRequestHeaders.getFirst("Authorization"));
        assertEquals("application/xml", lastRequestHeaders.getFirst("Content-Type"));
    }

    @Test
    void substitutesVariablesInUrlAndHeaders() {
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl() + "/{{path}}");
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Token", "{{token}}");
        request.setHeaders(headers);

        List<Variable> variables = List.of(new Variable("path", "api/orders"), new Variable("token", "secret-token"));

        new ChronosHttpRequestExecutor(request, variables, msg -> {}).execute();

        assertEquals("/api/orders", lastRequestPath);
        assertEquals("secret-token", lastRequestHeaders.getFirst("X-Token"));
    }

    @Test
    void substitutesParametersInUrl() {
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl() + "/users/:id");
        Map<String, String> params = new HashMap<>();
        params.put("id", "42");
        request.setParameters(params);

        new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertEquals("/users/42", lastRequestPath);
    }

    @Test
    void sendsRequestBodyForPost() {
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost(baseUrl());
        request.setHttpMethod(ChronosHttpMethod.POST);
        request.setRequestBody("{\"name\":\"chronos\"}");

        new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertEquals("{\"name\":\"chronos\"}", lastRequestBody);
    }

    @Test
    void reportsFailureWhenServerUnreachable() {
        RequestItem request = new RequestItem();
        request.setName("test");
        request.setServerHost("http://127.0.0.1:1");

        var response = new ChronosHttpRequestExecutor(request, null, msg -> {}).execute();

        assertTrue(response.isFail());
    }
}
