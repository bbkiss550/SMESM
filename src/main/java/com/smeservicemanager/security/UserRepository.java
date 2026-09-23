package com.smeservicemanager.security;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "role")
    Optional<User> findByUsernameIgnoreCase(String username);

    @EntityGraph(attributePaths = "role")
    @Query("select u from User u where u.status = 'A' and u.role.code = 'TECHNICIAN' order by u.firstName")
    List<User> findActiveTechnicians();

    @EntityGraph(attributePaths = "role")
    @Query("select u from User u where (:q = '' or lower(u.username) like lower(concat('%', :q, '%')) or lower(concat(u.firstName, ' ', u.lastName)) like lower(concat('%', :q, '%'))) order by u.id")
    Page<User> search(@Param("q") String query, Pageable pageable);
}
