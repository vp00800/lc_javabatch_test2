# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).


## [2025-01-20]

### Fixes
- Add new sample of call store procedure in DB (#12)

## [2025-01-14]

### Fixes
- Fix Problem of continuing to use previous settings even if job parameters are not set. [#10]]
- Fix error 'org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException: A job instance already exists ' [#10]

## [2025-01-12]

### Fixes
- Fix the ClassNotFound exception with '**SizeAndTimeBasedFNATP**'
- Add output different log files with different jobs.




## [2025-01-10]

### Fixes
- Fix the issue that run 'addAndDelete' job automatically on start up. 
- Fix "JobInstanceAlreadyCompleteException: A job instance already exists" error in repeating test with non-embeded DB.
- Fix syntax error "with '`' character" in non-embeded DB.

## [2025-01-03]

### Fixes

- Resolve unsuccessful test in local development environment and refactor code.