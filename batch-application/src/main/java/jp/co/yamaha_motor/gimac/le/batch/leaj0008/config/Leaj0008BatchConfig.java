package jp.co.yamaha_motor.gimac.le.batch.leaj0008.config;

import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0008.service.Leaj0008Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@Slf4j
public class Leaj0008BatchConfig {

    public static final String JOB_NAME = "jobLeaj0008";

    @Inject
    private JobRepository jobRepository;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private Leaj0008Service leaj0008Service;

    @Bean(name = JOB_NAME)
    public Job jobLeaj0008() {
        return new JobBuilder(JOB_NAME, jobRepository)
                  .start(stepLeaj0008())
                  .build();
    }

    @Bean(name = JOB_NAME + "stepLeaj0008")
    @JobScope
    public Step stepLeaj0008() {
        return new StepBuilder(JOB_NAME + "stepLeaj0008", jobRepository)
                  .tasklet((contribution, chunkContext) -> {
                      Map<String, Object> map = chunkContext.getStepContext().getJobParameters();
                      leaj0008Service.leaj0008Executing(map);
                      return RepeatStatus.FINISHED;
                  }, transactionManager)
                  .build();
    }
}
