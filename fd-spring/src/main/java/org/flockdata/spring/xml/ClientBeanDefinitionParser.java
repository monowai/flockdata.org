/*
 *  Copyright 2012-2017 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.flockdata.spring.xml;

import org.flockdata.spring.FlockDataClientFactoryBean;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

@Component
public class ClientBeanDefinitionParser implements BeanDefinitionParser {
  private static org.slf4j.Logger logger = LoggerFactory.getLogger(ClientBeanDefinitionParser.class);

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
