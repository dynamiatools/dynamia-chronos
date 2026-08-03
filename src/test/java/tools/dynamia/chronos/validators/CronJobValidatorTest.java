package tools.dynamia.chronos.validators;

import org.junit.jupiter.api.Test;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.domain.ValidationError;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CronJobValidatorTest {

    private final CronJobValidator validator = new CronJobValidator();

    @Test
    void acceptsValidCronExpression() {
        CronJob cronJob = new CronJob();
        cronJob.setCronExpression("0 0 * * * *");

        assertDoesNotThrow(() -> validator.validate(cronJob));
    }

    @Test
    void rejectsEmptyCronExpression() {
        CronJob cronJob = new CronJob();
        cronJob.setCronExpression("");

        assertThrows(ValidationError.class, () -> validator.validate(cronJob));
    }

    @Test
    void rejectsNullCronExpression() {
        CronJob cronJob = new CronJob();

        assertThrows(ValidationError.class, () -> validator.validate(cronJob));
    }

    @Test
    void rejectsInvalidCronExpression() {
        CronJob cronJob = new CronJob();
        cronJob.setCronExpression("not a cron expression");

        assertThrows(ValidationError.class, () -> validator.validate(cronJob));
    }
}
