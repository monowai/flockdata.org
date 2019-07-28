/*
 *
 *  Copyright (c) 2012-2016 "FlockData LLC"
 *
 *  This file is part of FlockData.
 *
 *  FlockData is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  FlockData is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.helper;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author mholdsworth
 * @since 15/02/2016
 */
public class ExecutorHelper {
  public static Executor getExecutor(String name, String poolSize, int qCapacity) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    String vals[] = StringUtils.split(poolSize, "-");

    executor.setCorePoolSize(Integer.parseInt(vals[0]));
    if (vals.length == 2) {
      executor.setMaxPoolSize(Integer.parseInt(vals[1]));
    } else {
      executor.setMaxPoolSize(Integer.parseInt(vals[0]));
    }
    executor.setQueueCapacity(qCapacity);
    executor.setThreadNamePrefix(name + "-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor.getThreadPoolExecutor();
  }

}
