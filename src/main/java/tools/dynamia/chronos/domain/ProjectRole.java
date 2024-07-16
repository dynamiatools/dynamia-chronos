package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import tools.dynamia.domain.jpa.SimpleEntityUuid;
import tools.dynamia.modules.security.domain.User;

@Entity
@Table(name = "crn_roles")
public class ProjectRole extends SimpleEntityUuid {

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Project project;
    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull(message = "Select user")
    private User user;
    private UserRole role = UserRole.Writer;

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}
