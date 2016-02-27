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

package org.flockdata.helper;

import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by mike on 15/02/16.
 */
public class ExecutorHelper {
    public static Executor getExecutor(String name, String poolSize, int  qCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        String vals[] = StringUtils.split(poolSize, "-");

        executor.setCorePoolSize(Integer.parseInt(vals[0]));
        if ( vals.length ==2 )
            executor.setMaxPoolSize(Integer.parseInt(vals[1]));
        else
            executor.setMaxPoolSize(Integer.parseInt(vals[0]));
        executor.setQueueCapacity(qCapacity);
        executor.setThreadNamePrefix(name + "-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor.getThreadPoolExecutor();
    }

}
