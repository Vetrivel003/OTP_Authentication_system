package com.project.spring.data.repository;

import com.project.spring.data.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<AdminUser,Long> {
    Optional<AdminUser> findByEmail(String Email);
}
