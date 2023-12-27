package tools.dynamia.chronos.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import tools.dynamia.domain.jpa.SimpleEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crn_projects")
public class Project extends SimpleEntity {

    private String name;
    @Column(length = 1000)
    private String description;
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Variable> variables = new ArrayList<>();
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CronJob> cronjobs = new ArrayList<>();

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
}
