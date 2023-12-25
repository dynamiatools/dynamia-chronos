package tools.dynamia.chronos.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import org.springframework.scheduling.support.CronExpression;
import tools.dynamia.commons.DateTimeUtils;
import tools.dynamia.domain.contraints.NotEmpty;
import tools.dynamia.domain.jpa.SimpleEntity;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "crn_jobs")
public class CronJob extends SimpleEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;
    @NotEmpty
    private String name;
    private String description;

    @NotEmpty
    @NotNull
    private String serverHost;
    private Integer serverPort;
    private String serverAuthorization;
    @NotNull
    private Duration serverTimeout = Duration.ofSeconds(30);
    @Lob
    private String requestBody; //optional
    private String contentType = "application/json";
    @NotEmpty
    private String cronExpression;

    private boolean active = true;
    private boolean notifyFails = true;
    private boolean notifyExecutions = false;
    private LocalDateTime lastExecution;
    private LocalDateTime createdAt = LocalDateTime.now();
    private long executionsCount;
    private String status;

    @Transient
    public String getNextExecution() {
        if (!isActive()) {
            return "none";
        } else {
            var next = CronExpression.parse(cronExpression).next(lastExecution != null ? lastExecution : LocalDateTime.now());
            return DateTimeUtils.format(next, "yyyy-MM-dd HH:mm:ss");
        }
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverPath) {
        this.serverHost = serverPath;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }

    public String getServerAuthorization() {
        return serverAuthorization;
    }

    public void setServerAuthorization(String serverAuthorization) {
        this.serverAuthorization = serverAuthorization;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isNotifyFails() {
        return notifyFails;
    }

    public void setNotifyFails(boolean notifyFails) {
        this.notifyFails = notifyFails;
    }

    public boolean isNotifyExecutions() {
        return notifyExecutions;
    }

    public void setNotifyExecutions(boolean notifyExecutions) {
        this.notifyExecutions = notifyExecutions;
    }

    public LocalDateTime getLastExecution() {
        return lastExecution;
    }

    public void setLastExecution(LocalDateTime lastExecution) {
        this.lastExecution = lastExecution;
    }

    public long getExecutionsCount() {
        return executionsCount;
    }

    public void setExecutionsCount(long executionsCount) {
        this.executionsCount = executionsCount;
    }


    public Duration getServerTimeout() {
        return serverTimeout;
    }

    public void setServerTimeout(Duration serverTimeout) {
        this.serverTimeout = serverTimeout;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
