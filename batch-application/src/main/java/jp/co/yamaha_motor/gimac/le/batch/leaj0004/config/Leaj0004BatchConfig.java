package jp.co.yamaha_motor.gimac.le.batch.leaj0004.config;

import jakarta.inject.Inject;
import jp.co.yamaha_motor.gimac.le.batch.leaj0004.service.Leaj0004Service;
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
public class Leaj0004BatchConfig {

    public static final String JOB_NAME = "jobLeaj0004";

    @Inject
    private JobRepository jobRepository;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private Leaj0004Service Leaj0004Service;

    @Bean(name = JOB_NAME)
    public Job jobLeaj0004() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(stepLeaj0004())
                .build();
    }

    @Bean(name = JOB_NAME + "stepLeaj0004")
    @JobScope
    public Step stepLeaj0004() {
        return new StepBuilder(JOB_NAME + "stepLeaj0004", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Map<String, Object> map = chunkContext.getStepContext().getJobParameters();
                    Leaj0004Service.start(map);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
