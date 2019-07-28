/*
 *  Copyright 2012-2016 the original author or authors.
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

package org.flockdata.authentication;

/**
 * @author mholdsworth
 * @since 17/02/2016
 */
public class FdRoles {
  public static final String FD_USER = "FD_USER";
  public static final String FD_ADMIN = "FD_ADMIN";
  public static final String FD_ROLE_ADMIN = "ROLE_" + FD_ADMIN;
  public static final String FD_ROLE_USER = "ROLE_" + FD_USER;

  public static final String EXP_ADMIN = "hasRole('" + FD_ADMIN + "')";
  public static final String EXP_USER = "hasRole('" + FD_USER + "')";
  public static final String EXP_EITHER = "hasAnyRole('" + FD_USER + "','" + FD_ADMIN + "')";
}
