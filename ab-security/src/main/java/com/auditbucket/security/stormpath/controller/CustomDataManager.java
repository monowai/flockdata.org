/*
 * Copyright 2014 Stormpath, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.auditbucket.security.stormpath.controller;

import com.auditbucket.security.stormpath.model.CustomDataBean;
import com.auditbucket.security.stormpath.model.CustomDataFieldBean;
import com.stormpath.sdk.client.Client;
import com.stormpath.sdk.directory.CustomData;
import com.stormpath.spring.security.authz.CustomDataPermissionsEditor;
import com.stormpath.spring.security.provider.StormpathAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Bean encapsulating <a href="http://docs.stormpath.com/java/product-guide/#custom-data">Custom Data</a> operations
 * (i.e. retrieve, delete, insert) to be executed in the <a href="http://www.stormpath.com">Stormpath</a> account by
 * means of the <a href="https://github.com/stormpath/stormpath-sdk-java">Stormpath Java SDK</a>
 * <pre/>
 * Since this stormpath app has the a cache configured for Stormpath SDK, we will be evicting the Account information from
 * the cache so the Authentication token can be re-generated with the updated set of permissions. The eviction will
 * only take place when the `spring security permission string` is somehow modified. Specifically, we are using {@link CacheEvict}
 * in the {@link #addCustomDataField(String, String, String, boolean)} and {@link #deleteCustomDataField(String, String, boolean)}
 * but it will be executed only if the key of the custom data field being modified is the `spring security permission string`.
 *
 * @since 0.2.0
 */
@Component
public class CustomDataManager {
    private static final Logger logger = LoggerFactory.getLogger(CustomDataManager.class);

    @Autowired
    private Client stormpathClient;

    @Autowired
    private StormpathAuthenticationProvider authenticationProvider;

    /**
     * Method that returns the custom data for the given account.
     *
     * @param accountHref the account href whose custom data is being requested
     * @return a {@link CustomDataBean} instance containing the custom data fields
     */
    @PreAuthorize("isAuthenticated() and principal.properties.get('href') == #accountHref") //Is the user the owner of the account to be edited?
    public CustomDataBean getCustomData(String accountHref) {
        if (!StringUtils.hasText(accountHref)) {
            throw new IllegalArgumentException("accountHref cannot be null or empty");
        }
        CustomData customData = stormpathClient.getResource(accountHref + "/customData", CustomData.class);
        List<CustomDataFieldBean> customDataFieldBeanList = new ArrayList<CustomDataFieldBean>();
        for (Map.Entry<String, Object> entry : customData.entrySet()) {
            customDataFieldBeanList.add(new CustomDataFieldBean(entry.getKey(), entry.getValue()));
        }

        CustomDataBean customDataBean = new CustomDataBean();
        customDataBean.setCustomDataFields(customDataFieldBeanList);
        return customDataBean;
    }

    /**
     *  This method will insert or update a custom data field into the custom data of the given account. When a new
     *  field is added (i.e, not previously existing key) then the field is created with the key and the value as String.
     *  If the key already exists, the existing value will be converted to a list (if it is not a list already) and the
     *  new value will be added to it.
     *  <pre/>
     *  If the key equals the <a href="https://github.com/stormpath/stormpath-spring-security/wiki#wiki-permissions">spring
     *  security permission string</a> then {@link CustomDataPermissionsEditor} will be used to update it.
     *  <pre/>
     *  If the key is the `spring security permission string`, the account will be evicted from the cache so the set of
     *  permissions is properly re-evaluated with the updated information later on.
     *
     * @param accountHref the href of the account where the key-value field will be added.
     * @param key the key of the custom data field to be added
     * @param value the value of the custom data field to be added
     * @param isPermissionKey true if the key is the "spring permission string", false otherwise
     * @return a {@link CustomDataFieldBean} instance containing the custom data field just updated
     */
    @CacheEvict(value = "com.stormpath.sdk.account.Account", key = "#accountHref", condition = "#isPermissionKey" ) //Let's get the account data evicted from the cache so the authentication's permissions are updated
    @PreAuthorize("isAuthenticated() and principal.properties.get('href') == #accountHref") //Is the user the owner of the account to be edited?
    public CustomDataFieldBean addCustomDataField(String accountHref, String key, String value, boolean isPermissionKey) {
        if (!StringUtils.hasText(accountHref)) {
            throw new IllegalArgumentException("accountHref cannot be null or empty");
        }
        if (key == null || key.length() == 0 || key.contains(" ")) {
            throw new IllegalArgumentException("key cannot be null, empty or contain spaces");
        }

        CustomData customData = stormpathClient.getResource(accountHref + "/customData", CustomData.class);

        if( isPermissionKey ) {
            addPermission(customData, value);
        } else {
            addField(customData, key, value);
        }

        return new CustomDataFieldBean(key, customData.get(key));
    }


    /**
     * This method will delete a custom data field from the given account.
     * <pre/>
     *  If the key is the `spring security permission string`, the account will be evicted from the cache so the set of
     *  permissions is properly re-evaluated with the updated information later on.
     *
     * @param accountHref the href of the account where the custom data field will be deleted
     * @param key the key of the custom data field to be deleted
     * @param isPermissionKey true if the key is the "spring permission string", false otherwise
     */
    @CacheEvict(value = "com.stormpath.sdk.account.Account", key = "#accountHref", condition = "#isPermissionKey" ) //Let's get the account data evicted from the cache so the authentication's permissions are updated
    @PreAuthorize("isAuthenticated() and principal.properties.get('href') == #accountHref") //Is the user the owner of the account to be edited?
    public void deleteCustomDataField(String accountHref, String key, boolean isPermissionKey) {
        if (!StringUtils.hasText(accountHref)) {
            throw new IllegalArgumentException("accountHref cannot be null or empty");
        }
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }

        CustomData customData = this.stormpathClient.getResource(accountHref + "/customData", CustomData.class);

        customData.remove(key);
        customData.save();

        if( isPermissionKey ) {
            refreshAuthentication();
        }


    }

    private void addPermission(CustomData customData, String value) {
        CustomDataPermissionsEditor customDataPermissionsEditor = new CustomDataPermissionsEditor(customData);
        customDataPermissionsEditor.append(value);
        customData.save();
        refreshAuthentication();
    }

    private void addField(CustomData customData, String key, String value) {
        Object existingValue = customData.get(key);
        if (existingValue == null) {
            customData.put(key, value);
        } else if (existingValue instanceof String) {
            Set<String> set = new LinkedHashSet<String>();
            set.add((String)existingValue);
            set.add(value);
            customData.put(key, set);
        } else if (existingValue instanceof List) {
            ((List) existingValue).add(value);
            customData.put(key, existingValue);
        } else {
            String msg = "Unable to recognize CustomData field '" + key + "' value of type " +
                    value.getClass().getName() + ".  Expected type: String or List<String>";
            throw new IllegalArgumentException(msg);
        }
        customData.save();
    }

    /**
     * Whenever the data of the spring security string is updated in the custom data we need to re-authenticate
     * the user in order to get its Permissions regenerated with the new values.
     */
    private void refreshAuthentication() {
        UserDetails user = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (user != null && user.getUsername() != null && user.getPassword() != null) {
            SecurityContextHolder.clearContext();
            Authentication authentication = authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
            if(authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
    }

}
