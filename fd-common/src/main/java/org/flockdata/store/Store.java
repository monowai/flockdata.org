package org.flockdata.store;

/**
 * Created by mike on 17/02/16.
 */
public enum Store {
    REDIS, RIAK, MEMORY, NONE ;

//    static Store resolve(String kvStore){
//        if ( kvStore.equalsIgnoreCase(Store.REDIS.name()))
//            return Store.REDIS;
//        else if (kvStore.equalsIgnoreCase(RIAK.name()))
//            return Store.RIAK;
//        else if (kvStore.equalsIgnoreCase(NONE.name()))
//            return ( Store.NONE);
//        else if (kvStore.equalsIgnoreCase(MEMORY.name()))
//            return ( Store.MEMORY);
//        else {
//            return ( Store.NONE);
//        }
//    }
}
