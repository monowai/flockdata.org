package com.auditbucket.test.utils;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import redis.embedded.RedisServer;

import java.io.File;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: nabil
 * Date: 22/10/13
 * Time: 00:02
 * To change this template use File | Settings | File Templates.
 */
public class AbstractRedisSupport {
    private static RedisServer redisServer;

    @BeforeClass
    public static void setup() throws Exception {
        if(redisServer == null){
            // If you are on Winodws
            if (System.getProperty("os.arch").equals("amd64") && System.getProperty("os.name").startsWith("Windows")) {
                URL url = AbstractRedisSupport.class.getResource("/redis/redis-server.exe");
                File redisServerExe = new File(url.getFile());
                redisServer = new RedisServer(redisServerExe, 6379); // or new RedisServer("/path/to/your/redis", 6379);
            } else {
                redisServer = new RedisServer(6379);
            }
        }
        redisServer.start();
    }



    @AfterClass
    public static void tearDown() throws Exception {
        //redisServer.stop();
    }
}
