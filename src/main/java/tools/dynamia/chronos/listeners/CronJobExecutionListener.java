package tools.dynamia.chronos.listeners;

import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.chronos.domain.CronJobLog;


public interface CronJobExecutionListener {

    default void beforeExecution(CronJob cronJob) {
    }

    default void afterExecution(CronJob cronJob, CronJobLog log) {
    }

}
