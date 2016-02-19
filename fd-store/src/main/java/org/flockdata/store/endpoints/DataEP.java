package org.flockdata.store.endpoints;

import org.flockdata.store.Store;
import org.flockdata.store.StoredContent;
import org.flockdata.store.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by mike on 18/02/16.
 */
@RestController
@RequestMapping("${fd-store.system.api:api}/v1/data")
public class DataEP {
    @Autowired
    StoreService storeService;
    StoredContent getData(String repo, String index, String type, Object id ){
        Store store = Store.valueOf(repo);
        return storeService.doRead(store, index.toLowerCase(), type.toLowerCase(), id);
    }
}
