package org.flockdata.authentication;

/**
 * Created by mike on 17/02/16.
 */
public class FdRoles {
    public static final String FD_USER ="FD_USER";
    public static final String FD_ADMIN ="FD_ADMIN";
    public static final String FD_ROLE_ADMIN = "ROLE_"+FD_ADMIN;
    public static final String FD_ROLE_USER = "ROLE_"+FD_USER;

    public static final String EXP_ADMIN = "hasRole('"+FD_ADMIN+"')";
    public static final String EXP_USER = "hasRole('"+ FD_USER+"')";
    public static final String EXP_EITHER = "hasAnyRole('"+ FD_USER+"','"+FD_ADMIN+"')";
}
