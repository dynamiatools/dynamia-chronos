package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import tools.dynamia.chronos.ChronosHttpRequest;
import tools.dynamia.chronos.HeadersProvider;
import tools.dynamia.commons.StringPojoParser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "crn_collections_items")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestItem extends ChronosHttpRequest implements HeadersProvider {

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private RequestCollection collection;
    @Column(columnDefinition = "json")
    private String headers;


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
        if (headers != null) {
            this.headers = StringPojoParser.convertMapToJson(headers);
        }
    }

    public RequestCollection getCollection() {
        return collection;
    }

    public void setCollection(RequestCollection collection) {
        this.collection = collection;
    }
}