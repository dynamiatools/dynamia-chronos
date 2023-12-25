package tools.dynamia.chronos.validators;

import org.springframework.scheduling.support.CronExpression;
import tools.dynamia.chronos.domain.CronJob;
import tools.dynamia.domain.ValidationError;
import tools.dynamia.domain.Validator;
import tools.dynamia.domain.ValidatorUtil;
import tools.dynamia.integration.sterotypes.Provider;

@Provider
public class CronJobValidator implements Validator<CronJob> {
    @Override
    public void validate(CronJob cronJob) throws ValidationError {
        ValidatorUtil.validateEmpty(cronJob.getCronExpression(), "Cron expression required");

        if (!CronExpression.isValidExpression(cronJob.getCronExpression())) {
            throw new ValidationError("Invalid cron expression");
        }
    }
}
