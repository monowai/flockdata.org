package org.flockdata.batch.resources;

import org.flockdata.track.bean.EntityInputBean;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author nabil
 */
public class FlockDataItemWriter implements ItemWriter<EntityInputBean> {

    @Autowired
    private FdBatchLoader fdBatchLoader;

    @Override
    public void write(List<? extends EntityInputBean> items) throws Exception {
        for (EntityInputBean item : items) {
            fdBatchLoader.write(item);
        }
    }
}
