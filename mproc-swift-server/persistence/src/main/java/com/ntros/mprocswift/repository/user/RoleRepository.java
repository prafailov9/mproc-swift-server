package com.ntros.mprocswift.repository.user;

import com.ntros.mprocswift.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleName(String roleName);

//    private final JdbcTemplate jdbcTemplate;
//
//    public RoleRepository(final JdbcTemplate jdbcTemplate) {
//        this.jdbcTemplate = jdbcTemplate;
//    }
//
//    public Optional<Role> findById(int roleId) {
//        String sql = "SELECT * FROM role WHERE role_id=?";
//        return Optional.ofNullable(jdbcTemplate.queryForObject(sql, new RoleRowMapper(), roleId));
//    }
//
//    public List<Role> findAll() {
//        String sql = "SELECT * FROM role";
//        return jdbcTemplate.query(sql, new RoleRepository.RoleRowMapper());
//    }
//
//    public Optional<Role> findByRoleName(final String roleName) {
//        String sql = "SELECT * FROM role WHERE role_name=?";
//        return Optional.ofNullable(jdbcTemplate.queryForObject(sql, new RoleRowMapper(), roleName));
//    }
//
//    private static class RoleRowMapper implements RowMapper<Role> {
//        @Override
//        public Role mapRow(ResultSet rs, int rowNum) throws SQLException {
//            Role role = new Role();
//            role.setRoleId(rs.getInt("user_id"));
//            role.setRoleName(rs.getString("role_name"));
//            return role;
//        }
//    }

}
