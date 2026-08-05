package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import org.springframework.scheduling.support.CronExpression;
import tools.dynamia.chronos.ChronosHttpRequest;
import tools.dynamia.chronos.HeadersProvider;
import tools.dynamia.chronos.ProjectAware;
import tools.dynamia.commons.DateTimeUtils;
import tools.dynamia.commons.StringPojoParser;
import tools.dynamia.domain.contraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "crn_jobs")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CronJob extends ChronosHttpRequest implements ProjectAware, HeadersProvider {

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Project project;


    private Integer serverPort;
    @NotEmpty
    private String cronExpression;

    private boolean notifyFails = true;
    private boolean notifyExecutions = false;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastExecution;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();
    private long executionsCount;
    private String status;
    @Column(columnDefinition = "json")
    private String headers;

    public CronJob() {
    }

    public CronJob(Project project) {
        this.project = project;
    }

    @Transient
    public String getNextExecution() {
        var next = getNextExecutionDateTime();
        return next == null ? "none" : DateTimeUtils.format(next, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Same as {@link #getNextExecution()} but as a raw {@link LocalDateTime}
     * (null when inactive), for callers that need to compute with it instead
     * of displaying it — e.g. the scheduler's "next execution" gauge.
     */
    @Transient
    @JsonIgnore
    public LocalDateTime getNextExecutionDateTime() {
        if (!isActive() || cronExpression == null) {
            return null;
        }
        return CronExpression.parse(cronExpression).next(lastExecution != null ? lastExecution : LocalDateTime.now());
    }


    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }


    public Integer getServerPort() {
        return serverPort;
    }

    public void setServerPort(Integer serverPort) {
        this.serverPort = serverPort;
    }


    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
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
        } else {
            this.headers = null;
        }
    }

    public void checkHeaders() {
        if (headers != null && headers.isBlank()) {
            this.headers = null;
        }
    }
}
