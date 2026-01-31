package tools.dynamia.chronos.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import tools.dynamia.chronos.ChronosHttpResponse;
import tools.dynamia.web.util.HtmlTableBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "crn_jobs_logs")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CronJobLog extends ChronosHttpResponse {



    @ManyToOne
    @NotNull
    private CronJob cronJob;
    private boolean executed;


    @Column(length = 1000)
    private String details;
    private String serverHost;

    public CronJobLog() {
        setStartDate(LocalDateTime.now());
    }

    public CronJobLog(CronJob cronJob) {
        this();
        this.cronJob = cronJob;
    }

    public CronJobLog(CronJob cronJob, boolean executed, boolean fail) {
        this();
        this.cronJob = cronJob;
        this.executed = executed;
        setFail(fail);
    }

    @Override
    public String toString() {
        return "CronJobLog{" +
                "date=" + getStartDate() +
                ", cronJob=" + cronJob +
                ", executed=" + executed +
                ", fail=" + isFail() +
                ", status='" + getStatus() + '\'' +
                '}';
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



    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }



    public String toHtml() {
        var htb = new HtmlTableBuilder();
        htb.addRowAndData("Status: ", getStatus());
        if (details != null) {
            htb.addRowAndData("Details: ", getDetails());
        }
        htb.addRowAndData("Executed: ", isExecuted());
        htb.addRowAndData("Failed: ", isFail());
        htb.addRowAndData("Duration: ", getDuration().toMillis() + "ms");
        return htb.render();
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }
}
