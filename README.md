# Solid Java Batch  

## Prerequisites
- **JDK 17 or newer** - check with command 

  ```bash
  java -version
  ```

  

### Business logic implementation
Business logic is implemented via `Facade`, `Service` and `Repository` layers.  
These parts are the same as `Solid Web Application`.  

### Batch job orchestration
After implementing business logic, you can implement batch job orchestration.  
You need to define the batch steps, and orchestrate them in the batch jobs.  
`BatchConfiguration.java`
``` java
@Configuration
public class BatchConfiguration {

    private leaj0015Service leaj0015Service;

    @Autowired
    public void setleaj0015Service(leaj0015Service leaj0015Service) {
        this.leaj0015Service = leaj0015Service;
    }

    @Bean(name = "stepAdd")
    Step add(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("add", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    leaj0015Service.addOne("1", "1");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean(name = "stepDelete")
    Step delete(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("delete", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    leaj0015Service.deleteOne("1");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean(name = "addAndDelete")
    @Primary 
    Job addAndDelete(@Qualifier("stepAdd") Step stepAdd,@Qualifier("stepDelete") Step stepDelete, JobRepository jobRepository) {
        return new JobBuilder("addAndDelete", jobRepository)
                .start(stepAdd)
                .next(stepDelete)
                .build();
    }

    @Bean(name = "addOnly")
    Job addOnly(@Qualifier("stepAdd") Step stepAdd, JobRepository jobRepository) {
        return new JobBuilder("addOnly", jobRepository)
                .start(stepAdd)
                .build();
    }
     
}
```
- Define the business logic as steps
- Orchestrate the steps as jobs(start, next, etc.), and [control the flow](https://docs.spring.io/spring-batch/reference/step/controlling-flow.html)  
- Define the name for each step and job. The name would be used for orchestration or job trigger  

## Build the jar file  
Run command:  
```shell
./gradlew clean build --refresh-dependencies --stacktrace
```

When it's done, you will get 4 ".jar" files under `build\libs` directory in your project "`batch-application`".

```text
batch-application-1.0.0-boot.jar // <<--- the distribution jar.
batch-application-1.0.0-javadoc.jar
batch-application-1.0.0-plain.jar
batch-application-1.0.0-sources.jar
```

pick the '**batch-application-1.0.0.jar**' file as the deployment file.

#### Run in command line

#####  Run with gradle command

Command format:

```shell
./gradlew :<gradle module name>:bootRun --args="[--spring.profiles.active=<Running profile name>] --spring.batch.job.name=<Job name> [<param1>=<value1> <param2>=<value2> ...]"
```

for example:

```shell
./gradlew :batch-application:bootRun  --args="--spring.profiles.active=development --spring.batch.job.name=scores gender=F inputFile=classpath:/input_scores.csv outputFile=file:./out.csv"
```

#####   Run with jar file

Command format:

```java
java -jar <*-boot.jar file path> [--spring.profiles.active=<Running profile name>]  --spring.batch.job.name=<Job name> [<param1>=<value1> <param2>=<value2> ...]
```

for example:

```java
java -jar batch-application-1.0.0-boot.jar --spring.profiles.active=development --spring.batch.job.name=scores gender=F inputFile=classpath:/input_scores.csv outputFile=file:./out.csv
```

**📢Note:**
Running multiple batch jobs together is **no longer supported**.  
 If the auto-configuration detects a single job is, it will be executed on startup.
If multiple jobs are found in the context, a job name to execute on startup must be supplied by the user using the `spring.batch.job.name` property.

If disable running a `Job` found in the application context, set `spring.batch.job.enabled` property to value `false`.

## References
-  [YNA-G3 Solid Development Guide - Batch](https://globalymc.sharepoint.com/teams/yna-g3/SiteAssets/SitePages/WebDocument/SolidDevGuide/en/6.batch/6-1.all-purpose.aspx#6-1-all-purpose)

- [Spring Batch Document](https://docs.spring.io/spring-batch/reference)

