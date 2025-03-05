package jp.co.yamaha_motor.gimac.le.batch.leaj0015.config;

import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0015.service.Leaj0015Service;
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
public class Leaj0015BatchConfig {

    public static final String JOB_NAME = "jobLeaj0015";

    @Inject
    private Leaj0015Service leaj0015Service;
    @Inject
    private PlatformTransactionManager transactionManager;
    @Inject
    private JobRepository jobRepository;

    @Bean(JOB_NAME)
    public Job jobLeaj0015() {
        return new JobBuilder(JOB_NAME, jobRepository)
                  .start(stepLeaj0015())
                  .build();
    }

    @Bean(JOB_NAME + "stepLeaj0015")
    @JobScope
    public Step stepLeaj0015() {
        return new StepBuilder(JOB_NAME + "stepLeaj0015", jobRepository)
                  .tasklet((contribution, chunkContext)->{
                      Map<String, Object> map = chunkContext.getStepContext().getJobParameters();
                      leaj0015Service.leaj0015Executing(map);
                      return RepeatStatus.FINISHED;
                  }, transactionManager)
                  .build();
    }
}