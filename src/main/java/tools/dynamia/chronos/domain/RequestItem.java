package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import tools.dynamia.chronos.ChronosHttpRequest;
import tools.dynamia.chronos.HeadersProvider;
import tools.dynamia.chronos.ParametersProvider;
import tools.dynamia.commons.StringPojoParser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "crn_collections_items")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestItem extends ChronosHttpRequest implements HeadersProvider, ParametersProvider {

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private RequestCollection collection;
    @Column(columnDefinition = "json")
    private String headers;
    @Column(columnDefinition = "json")
    private String parameters;


    @Override
    public Map<String, String> getHeaders() {
        if (headers != null && !headers.isEmpty()) {
            return StringPojoParser.parseJsonToMap(headers)
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        } else {
            return Map.of();
        }
    }

    public void setHeaders(Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            this.headers = StringPojoParser.convertMapToJson(headers);
        }
    }

    @Override
    public Map<String, String> getParameters() {
        if (parameters != null && !parameters.isEmpty()) {
            return StringPojoParser.parseJsonToMap(parameters)
                    .entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        } else {
            return Map.of();
        }
    }

    public void setParameters(Map<String, String> parameters) {
        if (parameters != null && !parameters.isEmpty()) {
            this.parameters = StringPojoParser.convertMapToJson(parameters);
        }
    }

    public RequestCollection getCollection() {
        return collection;
    }

    public void setCollection(RequestCollection collection) {
        this.collection = collection;
    }

    public void checkHeaders() {
        if (headers != null && headers.isBlank()) {
            this.headers = null;
        }
    }

    public void checkParams() {
        if (parameters != null && parameters.isBlank()) {
            this.parameters = null;
        }
    }
}