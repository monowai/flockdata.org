/*
 * Copyright (c) 2016. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package org.flockdata.store.endpoints;

import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mike on 18/02/16.
 */
@RestController
@RequestMapping("${org.fd.store.system.api:api}/v1/data")
public class DataEP {
    @Autowired
    StoreService storeService;

    @RequestMapping(value = "/{repo}/{index}/{type}/{key}", produces = "application/json;charset=UTF-8", method = RequestMethod.GET)
    StoredContent getData(@PathVariable("repo") String repo,
                          @PathVariable ("index")String index,
                          @PathVariable ("type") String type,
                          @PathVariable ("key") String key ){
        Store store = Store.valueOf(repo);
        return storeService.doRead(store, index.toLowerCase(), type.toLowerCase(), key);
    }
}
