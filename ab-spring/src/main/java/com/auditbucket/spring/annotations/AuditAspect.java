package com.auditbucket.spring.annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
public class AuditAspect {
    public static Logger logger = LoggerFactory.getLogger(AuditAspect.class);


    @Around(value = "@annotation(annotation)")
    public void createAuditHeader(final ProceedingJoinPoint joinPoint, final AuditHeader annotation) throws Throwable {
        try {
            logger.info("createAuditHeader() is running!");
            logger.info("hijacked method : " + joinPoint.getSignature().getName());
            logger.info("hijacked arguments : " + Arrays.toString(joinPoint.getArgs()));
            joinPoint.proceed();
            logger.info("Around before is running!");
            joinPoint.proceed(); //continue on the intercepted method
            System.out.println();
        } finally {
            logger.info("Around after is running!");
        }
    }

    @Around(value = "@annotation(annotation)")
    public void createAuditLog(final ProceedingJoinPoint joinPoint, final AuditLog annotation) throws Throwable {
        try {
            logger.info("createAuditLog() is running!");
            logger.info("hijacked method : " + joinPoint.getSignature().getName());
            logger.info("hijacked arguments : " + Arrays.toString(joinPoint.getArgs()));
            joinPoint.proceed();
            logger.info("Around before is running!");
            joinPoint.proceed(); //continue on the intercepted method
            System.out.println();
        } finally {
            logger.info("Around after is running!");
        }
    }
}
