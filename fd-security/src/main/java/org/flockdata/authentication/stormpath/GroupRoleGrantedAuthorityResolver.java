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

package org.flockdata.authentication.stormpath;

//import com.stormpath.sdk.group.Group;
//import com.stormpath.spring.security.provider.GroupGrantedAuthorityResolver;

import java.util.List;
import java.util.Map;

/**
 * Another sample implementation of the {@code GroupGrantedAuthorityResolver} interface that allows a Stormpath
 * href found is translated to domain-specific role names.
 * <h2>Overview</h2>
 * This implementation converts a Group into one or more granted authorities roles based on the configured mapping.
 *
 * <h2>Usage</h2>
 * The configured mapping must have href groups as the key of the map. The value of each key is a list of roles that such
 * key will be translated to.
 * <p>
 * For stormpath, in code:
 * <pre>
 * Map&lt;String, List&lt;String&gt;&gt; rolesMap = new HashMap&lt;String, List&lt;String&gt;&gt;();
 * List&lt;String&gt; roles = new ArrayList&lt;String&gt;();
 * roles.add("ROLE_A");
 * roles.add("ROLE_C");
 * rolesMap.put("https://api.stormpath.com/v1/groups/d8UDkz9QPcn2z73j93m6Z", roles);
 * new GroupRoleGrantedAuthorityResolver(rolesMap);
 * </pre>
 * <p>
 * Or maybe in spring.xml:
 * {@literal
 * <pre>
 * <beans:bean id="groupRoleGrantedAuthoritiesMap" class="java.util.HashMap" scope="prototype" >
 *      <beans:constructor-arg>
 *          <beans:map key-type="java.lang.String" value-type="java.util.List">
 *              <beans:entry key="https://api.stormpath.com/v1/groups/d8UDkz9QPcn2z73j93m6Z">
 *                  <beans:list>
 *                      <beans:value>ROLE_A</beans:value>
 *                      <beans:value>ROLE_C</beans:value>
 *                  </beans:list>
 *              </beans:entry>
 *          </beans:map>
 *      </beans:constructor-arg>
 *  </beans:bean>
 * </pre>
 * }
 * In the above configuration, the https://api.stormpath.com/v1/groups/d8UDkz9QPcn2z73j93m6Z group translates into two granted
 * authorities: ROLE_A and ROLE_C. This allows your Spring Security application to remain Stormpath-agnostic and thus allowing
 * your own set of business-specific roles to be used in your application.
 */
public class GroupRoleGrantedAuthorityResolver {//implements GroupGrantedAuthorityResolver {

  private Map<String, List<String>> rolesMap;

  public GroupRoleGrantedAuthorityResolver(Map<String, List<String>> roleMap) {
    if (roleMap == null || roleMap.size() == 0) {
      throw new IllegalArgumentException("roleMap property cannot be null or empty.");
    }
    this.rolesMap = roleMap;
  }

  public Map<String, List<String>> getRolesMap() {
    return rolesMap;
  }

  public void setRolesMap(Map<String, List<String>> roleMap) {
    if (roleMap == null || roleMap.size() == 0) {
      throw new IllegalArgumentException("roleMap property cannot be null or empty.");
    }
    this.rolesMap = roleMap;
  }

//    @Override
//    public Set<GrantedAuthority> resolveGrantedAuthorities(Group group) {
//
//        Set<GrantedAuthority> set = new HashSet<GrantedAuthority>();
//
//        String groupHref = group.getHref();
//
//        //REST resource hrefs should never ever be null:
//        if (groupHref == null) {
//            throw new IllegalStateException("Group does not have an href property. This should never happen.");
//        }
//
//        if (rolesMap.containsKey(groupHref)) {
//            for(String role : rolesMap.get(groupHref)) {
//                set.add(new SimpleGrantedAuthority(role));
//            }
//        } else {
//            throw new UnsupportedOperationException("No GrantedAuthority mapping found for " + group);
//        }
//
//        return set;
//    }

}
