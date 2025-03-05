package com.g3.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.g3.batch.base.AddUniqParameterConverter;
import com.ymsl.solid.jpa.repository.JpaExtensionRepositoryFactoryBean;

@SpringBootApplication
@EntityScan("com.g3.batch")
@EnableJpaRepositories(basePackages = {"com.g3.batch"}, repositoryFactoryBeanClass = JpaExtensionRepositoryFactoryBean.class)
public class BatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(BatchApplication.class, args)));
    }
    
    @Bean
    AddUniqParameterConverter addUniqParameterConverter() {
        return new AddUniqParameterConverter();
    }
}
