package com.ntros.mprocswift.service.user;

import com.ntros.mprocswift.model.Role;
import com.ntros.mprocswift.repository.user.RoleRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class RoleDataService implements RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleDataService(final RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role getRole(int roleId) {
        return roleRepository
                .findById(roleId)
                .orElseThrow(
                        () -> new RuntimeException(String.format("Role not found for id: %s", roleId)));
    }

    @Override
    public List<Role> getAllRoles() {
        return Optional
                .of(roleRepository.findAll())
                .orElseThrow(() -> new RuntimeException("No roles in db"));
    }

    @Override
    public Role getRoleByName(String roleName) {
        return roleRepository
                .findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException(String.format("Role not found for name: %s", roleName)));
    }


}
