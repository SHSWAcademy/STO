package server.main.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.admin.entity.Admin;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByAdminLoginId(String adminLoginId);
}
