/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.auditbucket.spring.xml;

import com.auditbucket.spring.AuditBucketClientFactoryBean;
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

        bdef.setBeanClass(AuditBucketClientFactoryBean.class);
        BeanDefinitionBuilder clientBuilder = startClientBuilder(AuditBucketClientFactoryBean.class, properties);
        client = ClientBeanDefinitionParser.buildClientDef(clientBuilder);

        parserContext.getRegistry().registerBeanDefinition(id, client);

        return bdef;
    }

}
