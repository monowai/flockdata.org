/*
 * Copyright (c) 2012-2014 "FlockData LLC"
 *
 * This file is part of FlockData.
 *
 * FlockData is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FlockData is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FlockData.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.flockdata.spring.xml;

import org.flockdata.spring.FlockDataClientFactoryBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

class ClientBeanDefinitionParser implements BeanDefinitionParser {
    protected static final Log logger = LogFactory.getLog(ClientBeanDefinitionParser.class);

    /**
     * Managing common properties for NodeClient and TransportClient
     *
     * @param beanClass
     * @param properties
     * @return
     */
    private static BeanDefinitionBuilder startClientBuilder(Class beanClass, String properties) {
        BeanDefinitionBuilder nodeFactory = BeanDefinitionBuilder.rootBeanDefinition(beanClass);

        if (properties != null && properties.length() > 0) {
            nodeFactory.addPropertyReference("properties", properties);
        }

        return nodeFactory;
    }

    private static BeanDefinition buildClientDef(BeanDefinitionBuilder nodeFactory) {
        return nodeFactory.getBeanDefinition();
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        // When node is not null, we should build a client.
        // When node is null, we want to build a transport client.

        String id = XMLParserUtil.getElementStringValue(element, "id");

        String properties = XMLParserUtil.getElementStringValue(element, "properties");


        BeanDefinition client;

        GenericBeanDefinition bdef = new GenericBeanDefinition();

        bdef.setBeanClass(FlockDataClientFactoryBean.class);
        BeanDefinitionBuilder clientBuilder = startClientBuilder(FlockDataClientFactoryBean.class, properties);
        client = ClientBeanDefinitionParser.buildClientDef(clientBuilder);

        parserContext.getRegistry().registerBeanDefinition(id, client);

        return bdef;
    }

}
