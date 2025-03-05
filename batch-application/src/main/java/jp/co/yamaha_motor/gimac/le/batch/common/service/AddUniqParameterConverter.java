package jp.co.yamaha_motor.gimac.le.batch.common.service;

import com.ymsl.solid.base.util.StringUtils;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;

import java.time.Instant;
import java.util.Properties;

/**
 * Add a unique parameter to the job parameters before the job is executed.
 */
public class AddUniqParameterConverter implements JobParametersConverter {

    private JobParametersConverter converter = new DefaultJobParametersConverter();
    private static final String UNIQ_PARAMETER_KEY = "_uid_";
    
    @Override
    public JobParameters getJobParameters(Properties properties) {
        JobParameters ps = converter.getJobParameters(properties);
        if(StringUtils.isBlankText(ps.getString(UNIQ_PARAMETER_KEY))) {
            String runId = String.format("%d-%d", Instant.now().toEpochMilli(), Instant.now().getNano());
            return new JobParametersBuilder(ps)
                      .addString(UNIQ_PARAMETER_KEY, runId)  // Append a unique parameter
                      .toJobParameters();
        }
        return ps;
    }

    @Override
    public Properties getProperties(JobParameters params) {
        return converter.getProperties(params);
    }

}
