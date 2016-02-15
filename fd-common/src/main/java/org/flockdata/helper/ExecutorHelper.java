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
