package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import tools.dynamia.chronos.ProjectAware;
import tools.dynamia.domain.contraints.NotEmpty;
import tools.dynamia.domain.jpa.SimpleEntity;
import tools.dynamia.domain.jpa.SimpleEntityUuid;

@Entity
@Table(name = "crn_variables")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Variable extends SimpleEntityUuid implements ProjectAware {

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private RequestCollection collection;

    @NotEmpty
    private String name;
    @Column(length = 1000, name = "var_value")
    @NotEmpty
    private String value;
    private boolean secret;

    public Variable() {
    }

    public Variable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Variable(Project project, String name, String value) {
        this.project = project;
        this.name = name;
        this.value = value;
    }


    @Override
    public String toString() {
        return name;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public RequestCollection getCollection() {
        return collection;
    }

    public void setCollection(RequestCollection collection) {
        this.collection = collection;
    }
}
