package com.auditbucket.test.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * User: mike
 * Date: 10/08/14
 * Time: 7:56 PM
 */
public class TestHelper {
    public static Map<String, Object> getSimpleMap(String key, Object value){
        Map<String, Object> result = new HashMap<>();
        result.put(key, value);
        return result;
    }

    public static Map<String, Object> getRandomMap(){
        return getSimpleMap("Key", "Test"+System.currentTimeMillis());
    }

    public static Map<String, Object> getBigJsonText(int i) {
        Map<String, Object> map = getSimpleMap("Key", "Random");
        int count = 0;
        do {
            map.put("Key"+count, "Now is the time for all good men to come to the aid of the party");
            count++;
        } while ( count < i);
        return map;
    }
}
