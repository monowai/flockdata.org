package org.flockdata.test.engine.functional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

/**
 * Base class for Web App context driven classes
 * Created by mike on 12/02/16.
 */
@WebAppConfiguration
@ActiveProfiles({"dev","web-dev"})
public class WacBase extends EngineBase{

    @Autowired
    public WebApplicationContext wac;

}
