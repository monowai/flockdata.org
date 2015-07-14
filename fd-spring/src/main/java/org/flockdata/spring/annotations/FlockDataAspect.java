/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.spring.annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
public class FlockDataAspect {
    private static Logger logger = LoggerFactory.getLogger(FlockDataAspect.class);


    @Around(value = "@annotation(annotation)")
    public void createEntity(final ProceedingJoinPoint joinPoint, final FlockEntity annotation) throws Throwable {
        try {
            logger.debug("createEntity() is running!");
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
    public void createEntityLog(final ProceedingJoinPoint joinPoint, final FlockLog annotation) throws Throwable {
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
