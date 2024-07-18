package tools.dynamia.chronos;

import org.springframework.http.HttpStatus;
import tools.dynamia.chronos.domain.Variable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNullElse;

public class ChronosHttpRequestExecutor {

    protected final ChronosHttpRequest request;
    protected final Consumer<String> logger;
    protected final List<Variable> variables;
    private Consumer<HttpRequest> onRequest;
    private Consumer<HttpResponse<String>> onResponse;


    public ChronosHttpRequestExecutor(ChronosHttpRequest request, List<Variable> variables, Consumer<String> logger) {
        this.request = request;
        this.variables = requireNonNullElse(variables, Collections.emptyList());
        this.logger = logger;
    }

    protected ChronosHttpResponse newResponse() {
        return new ChronosHttpResponse();
    }

    public ChronosHttpResponse execute() {
        final String url = parseParameters(parseVariables(request.getServerHost()));
        final ChronosHttpResponse response = newResponse();
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            Builder internalRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(request.getServerTimeout());


            addHeader(internalRequest, "Authorization", request.getServerAuthorization());
            addHeader(internalRequest, "Content-Type", request.getContentType());
            if (request instanceof HeadersProvider provider) {
                requireNonNullElse(provider.getHeaders(),
                        new HashMap<String, String>())
                        .forEach((k, v) -> addHeader(internalRequest, k, v));
            }

            if (request.getRequestBody() != null && !request.getRequestBody().isBlank()) {
                internalRequest.method(request.getHttpMethod().toString(), HttpRequest.BodyPublishers.ofString(request.getRequestBody(), StandardCharsets.UTF_8));
            } else {
                internalRequest.method(request.getHttpMethod().toString(), HttpRequest.BodyPublishers.noBody());
            }


            try {
                if (onRequest != null) onRequest.accept(internalRequest.build());
                logger.accept("Executing http request " + url);
                response.setStartDate(LocalDateTime.now());
                HttpResponse<String> internalResponse = httpClient.send(internalRequest.build(), HttpResponse.BodyHandlers.ofString());
                HttpStatus status = HttpStatus.resolve(internalResponse.statusCode());
                if (status == null) {
                    status = HttpStatus.NO_CONTENT;
                }

                response.setStatusCode(status.value());
                response.setStatus(status.value() + " - " + status.getReasonPhrase());
                response.setFail(status != HttpStatus.OK);
                response.setExecuted(status == HttpStatus.OK);
                response.setResponse(internalResponse.body());
                response.setEndDate(LocalDateTime.now());
                response.setDuration(Duration.between(response.getStartDate(), response.getEndDate()));
                logger.accept("HTTP Response " + response.getResponse());
                if (onResponse != null) onResponse.accept(internalResponse);
            } catch (Exception e) {
                response.setFail(true);
                response.setResponse(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
        return response;
    }

    protected void addHeader(Builder request, String header, final String value) {
        if (header != null && value != null && !value.isBlank()) {
            var parsedValue = parseVariables(value);
            request.header(header, parsedValue);
        }
    }

    /**
     * Simple 'mustache like' template parser that replace all {{variable}} expression with variable value.
     *
     * @param template
     * @return
     */
    protected String parseVariables(final String template) {
        String result = template;
        for (var variable : variables) {
            result = result.replace("{{" + variable.getName() + "}}", variable.getValue());
        }
        return result;
    }

    protected String parseParameters(final String template) {
        String result = template;
        if (request instanceof ParametersProvider provider) {
            var params = requireNonNullElse(provider.getParameters(), new HashMap<String, String>());
            for (var entry : params.entrySet()) {
                String paramValue = entry.getValue();
                paramValue = parseVariables(paramValue);
                result = result.replace(":" + entry.getKey(), paramValue);
            }
        }
        return result;
    }


    public void setOnRequest(Consumer<HttpRequest> onRequest) {
        this.onRequest = onRequest;
    }

    public void setOnResponse(Consumer<HttpResponse<String>> onResponse) {
        this.onResponse = onResponse;
    }
}
