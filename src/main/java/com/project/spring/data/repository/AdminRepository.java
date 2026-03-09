package com.project.spring.data.repository;

import com.project.spring.data.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminRepository extends JpaRepository<AdminUser,Long> {
}
