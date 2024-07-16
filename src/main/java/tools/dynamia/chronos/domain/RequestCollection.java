package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import tools.dynamia.chronos.HeadersProvider;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.domain.jpa.SimpleEntityUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "crn_collections")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestCollection extends SimpleEntityUuid implements HeadersProvider {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;
    private String title;
    private String description;
    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestItem> requests = new ArrayList<>();

    @ManyToOne
    @JsonIgnore
    private RequestCollection parentCollection;

    @OneToMany(mappedBy = "parentCollection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestCollection> collections = new ArrayList<>();

    @OneToMany(mappedBy = "collection", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variable> variables = new ArrayList<>();

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


    public List<RequestItem> getRequests() {
        return requests;
    }

    public void setRequests(List<RequestItem> requests) {
        this.requests = requests;
    }



    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestCollection getParentCollection() {
        return parentCollection;
    }

    public void setParentCollection(RequestCollection parentCollection) {
        this.parentCollection = parentCollection;
    }

    public List<RequestCollection> getCollections() {
        return collections;
    }

    public void setCollections(List<RequestCollection> collections) {
        this.collections = collections;
    }
}
