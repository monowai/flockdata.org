/*
 *  Copyright 2012-2016 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.spring.annotations;

import java.util.Arrays;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
