package com.ntros.mprocswift.service.user;

import com.ntros.mprocswift.model.Role;

import java.util.List;

public interface RoleService {

    Role getRole(final int roleId);
    List<Role> getAllRoles();
    Role getRoleByName(final String roleName);

}
