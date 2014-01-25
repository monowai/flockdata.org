#!/bin/bash

## Test
#java -jar ab.jar -s=http://localhost/ab-engine "./cow.txt,com.auditbucket.health.Countries,-1" "./cervcan.xml,com.auditbucket.health.medline.study.CervicalCancer,-1" "./GraftVsHost.xml,com.auditbucket.health.medline.study.GraftVsHost,-1" "./hqi_hosp.csv,com.auditbucket.health.medicare.Hospital,-1" "./hcahps.csv,com.auditbucket.health.medicare.Hcahps,-1" "./inpatient_claims.csv,com.auditbucket.health.medicare.InpatientClaims,-1"

## Pubmed
java -jar ab.jar -s=http://localhost:8080/ab-engine "./cow.txt,com.auditbucket.health.Countries,100" "./cervcan.xml,com.auditbucket.health.medline.study.CervicalCancer,200" "./GraftVsHost.xml,com.auditbucket.health.medline.study.GraftVsHost,200"

## Medicare
java -jar ab.jar -s=http://localhost:8080/ab-engine "./hqi_hosp.csv,com.auditbucket.health.medicare.Hospital,200" "./hcahps.csv,com.auditbucket.health.medicare.Hcahps,200" "./inpatient_claims.csv,com.auditbucket.health.medicare.InpatientClaims,600"

