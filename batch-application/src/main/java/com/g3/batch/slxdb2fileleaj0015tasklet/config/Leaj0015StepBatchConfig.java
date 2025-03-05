package com.g3.batch.slxdb2fileleaj0015tasklet.config;

import com.g3.batch.slxdb2fileleaj0015tasklet.service.Leaj0015Service;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

@Configuration
@Slf4j
public class Leaj0015StepBatchConfig {

    public static final String JOB_NAME = "slxdb2fileleaj0015tasklet";

    @Inject
    private Leaj0015Service leaj0015Service;

    @Bean(JOB_NAME)
    public Job leaj0015Job(JobRepository jobRepository,
                              PlatformTransactionManager transactionManager,
                              @Qualifier(JOB_NAME + "leaj0015Step") Step procStep
    ) {
        return new JobBuilder(JOB_NAME, jobRepository)
                  .start(procStep)
                  .build();
    }

    @Bean(JOB_NAME + "leaj0015Step")
    @JobScope
    public Step leaj0015Step(JobRepository jobRepository,
                         PlatformTransactionManager transactionManager) {
        return new StepBuilder(JOB_NAME + "leaj0015Step", jobRepository)
                  .tasklet((contribution, chunkContext)->{
                      Map<String, Object> map = chunkContext.getStepContext().getJobParameters();
                      try {
                          leaj0015Service.startLeaj0015Tasklet(map);
                      } catch (Exception e) {
                          throw new RuntimeException(e);
                      }
                      return RepeatStatus.FINISHED;
                  }, transactionManager)
                  .build();
    }
}