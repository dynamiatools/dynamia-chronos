package tools.dynamia.chronos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import tools.dynamia.web.util.HtmlTableBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "crn_jobs_logs")
public class CronJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private LocalDateTime startDate;
    @NotNull
    private LocalDateTime endDate;

    @NotNull
    private Duration duration;
    @ManyToOne
    @NotNull
    private CronJob cronJob;
    private boolean executed;
    private boolean fail;

    private String status;

    @Column(columnDefinition = "text")
    private String response;

    @Column(length = 1000)
    private String details;
    private String serverHost;

    public CronJobLog() {
        this.startDate = LocalDateTime.now();
    }

    public CronJobLog(CronJob cronJob) {
        this();
        this.cronJob = cronJob;
    }

    public CronJobLog(CronJob cronJob, boolean executed, boolean fail) {
        this();
        this.cronJob = cronJob;
        this.executed = executed;
        this.fail = fail;
    }

    @Override
    public String toString() {
        return "CronJobLog{" +
                "date=" + startDate +
                ", cronJob=" + cronJob +
                ", executed=" + executed +
                ", fail=" + fail +
                ", status='" + status + '\'' +
                '}';
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime date) {
        this.startDate = date;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Duration getDuration() {
        return duration;
    }

    public void setDuration(Duration duration) {
        this.duration = duration;
    }

    public CronJob getCronJob() {
        return cronJob;
    }

    public void setCronJob(CronJob cronJob) {
        this.cronJob = cronJob;
    }

    public boolean isExecuted() {
        return executed;
    }

    public void setExecuted(boolean executed) {
        this.executed = executed;
    }

    public boolean isFail() {
        return fail;
    }

    public void setFail(boolean fail) {
        this.fail = fail;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CronJobLog that = (CronJobLog) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public String toHtml() {
        var htb = new HtmlTableBuilder();
        htb.addRowAndData("Status: ", getStatus());
        if (details != null) {
            htb.addRowAndData("Details: ", getDetails());
        }
        htb.addRowAndData("Executed: ", isExecuted());
        htb.addRowAndData("Failed: ", isFail());
        htb.addRowAndData("Duration: ", duration.toMillis() + "ms");
        return htb.render();
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }
}
