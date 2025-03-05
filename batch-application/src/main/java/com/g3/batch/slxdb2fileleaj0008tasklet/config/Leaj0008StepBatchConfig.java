package com.g3.batch.slxdb2fileleaj0008tasklet.config;

import com.g3.batch.slxdb2fileleaj0008tasklet.model.Leaj0008ParameterModel;
import com.g3.batch.slxdb2fileleaj0008tasklet.service.Leaj0008Service;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@Slf4j
public class Leaj0008StepBatchConfig {

    public static final String JOB_NAME = "slxdb2fileleaj0008tasklet";

    @Inject
    private JobRepository jobRepository;

    @Inject
    private PlatformTransactionManager transactionManager;

    @Inject
    private Leaj0008Service leaj0008Service;

    @Bean(name = JOB_NAME)
    public Job dbDataDownload() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(dbDataDownloadStep())
                .build();
    }

    @Bean(name = JOB_NAME + "DbDataDownloadStep")
    @JobScope
    public Step dbDataDownloadStep() {
        return new StepBuilder(JOB_NAME + "DbDataDownloadStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Map<String, Object> map = chunkContext.getStepContext().getJobParameters();
                    leaj0008Service.start(map);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
