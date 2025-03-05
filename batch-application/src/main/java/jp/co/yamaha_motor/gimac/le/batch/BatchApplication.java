package jp.co.yamaha_motor.gimac.le.batch;

import com.ymsl.solid.jpa.repository.JpaExtensionRepositoryFactoryBean;
import jp.co.yamaha_motor.gimac.le.batch.common.service.AddUniqParameterConverter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("jp.co.yamaha_motor.gimac.le.batch")
@EnableJpaRepositories(basePackages = {"jp.co.yamaha_motor.gimac.le.batch"}, repositoryFactoryBeanClass = JpaExtensionRepositoryFactoryBean.class)
public class BatchApplication {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(BatchApplication.class, args)));
    }
    
    @Bean
    AddUniqParameterConverter addUniqParameterConverter() {
        return new AddUniqParameterConverter();
    }
}
