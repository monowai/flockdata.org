package com.auditbucket.transform;

import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Lancher of FD Import using spring batch
 * User: Nabil
 * Date: 09/10/2014
 */
@Component
public class FileBatchProcessor {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FileBatchProcessor.class);

    //@Autowired
    JobLauncher jobLauncher;

    //@Autowired
    Job importCSVToFDJob;

    //@Autowired
    Job importJSONToFDJob;

    //@Autowired
    Job importXMLToFDJob;


    public void start(String fileName, Object parameters) {
        // Depending on what parameters we recieve
        // We will lanch the right Job (importCSVToFDJob , importJSONToFDJob ,  importXMLToFDJob )
        // We can imagine also that we have only One Job that decide XML OR JSON OR XML
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName",fileName)
                .addDate("date", new Date())
                .toJobParameters();
        try {
            JobExecution jobExecution = jobLauncher.run(importCSVToFDJob, jobParameters);
        } catch (JobExecutionAlreadyRunningException e) {
            LOGGER.error("Job Already Running : ", e);
        } catch (JobRestartException e) {
            LOGGER.error("Job Restart !!!! ", e);
        } catch (JobInstanceAlreadyCompleteException e) {
            LOGGER.error("Job Already Running : ", e);
        } catch (JobParametersInvalidException e) {
            LOGGER.error("Job Invalid Parameters : ", e);
        }
    }

}
