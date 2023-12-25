package tools.dynamia.chronos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import tools.dynamia.domain.jpa.SimpleEntity;

@Entity
@Table(name = "crn_projects_notificators")
public class Notificator extends SimpleEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @NotNull
    private String sender;

    @Column(length = 1000)
    private String contact;

    private boolean active = true;


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
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
}
