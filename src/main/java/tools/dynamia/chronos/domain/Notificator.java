package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import tools.dynamia.chronos.ProjectAware;
import tools.dynamia.domain.jpa.SimpleEntity;

@Entity
@Table(name = "crn_projects_notificators")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Notificator extends SimpleEntity implements ProjectAware {

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Project project;

    private Long sender;

    @Column(length = 1000)
    private String contact;

    private boolean active = true;


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Long getSender() {
        return sender;
    }

    public void setSender(Long sender) {
        this.sender = sender;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return getSender() + " - " + getContact();
    }
}
