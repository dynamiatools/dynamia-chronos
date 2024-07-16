package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import tools.dynamia.domain.jpa.SimpleEntityUuid;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crn_projects")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project extends SimpleEntityUuid {

    private String name;
    @Column(length = 1000)
    private String description;
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variable> variables = new ArrayList<>();
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CronJob> cronjobs = new ArrayList<>();
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestCollection> collections = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notificator> notificators = new ArrayList<>();

    public Project() {
    }

    public Project(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<Variable> getVariables() {
        return variables;
    }

    public void setVariables(List<Variable> variables) {
        this.variables = variables;
    }

    public List<CronJob> getCronjobs() {
        return cronjobs;
    }

    public void setCronjobs(List<CronJob> cronjobs) {
        this.cronjobs = cronjobs;
    }

    @Override
    public String toString() {
        return name;
    }

    public List<Notificator> getNotificators() {
        return notificators;
    }

    public void setNotificators(List<Notificator> notificators) {
        this.notificators = notificators;
    }

    public List<RequestCollection> getCollections() {
        return collections;
    }

    public void setCollections(List<RequestCollection> collections) {
        this.collections = collections;
    }
}
