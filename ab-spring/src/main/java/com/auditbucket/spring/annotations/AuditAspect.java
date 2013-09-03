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
            logger.debug("createAuditHeader() is running!");
            logger.debug("hijacked method : {}", joinPoint.getSignature().getName());
            logger.debug("hijacked arguments : {}", Arrays.toString(joinPoint.getArgs()));
            joinPoint.proceed();
            logger.debug("Around before is running!\r\n");
            joinPoint.proceed(); //continue on the intercepted method
        } finally {
            logger.info("Around after is running!");
        }
    }

    @Around(value = "@annotation(annotation)")
    public void createAuditLog(final ProceedingJoinPoint joinPoint, final AuditLog annotation) throws Throwable {
        try {
            logger.debug("createAuditLog() is running!");
            logger.debug("hijacked method : {}", joinPoint.getSignature().getName());
            logger.debug("hijacked arguments : {}", Arrays.toString(joinPoint.getArgs()));
            joinPoint.proceed();
            logger.debug("Around before is running!\r\n");
            joinPoint.proceed(); //continue on the intercepted method
        } finally {
            logger.debug("Around after is running!");
        }
    }
}
