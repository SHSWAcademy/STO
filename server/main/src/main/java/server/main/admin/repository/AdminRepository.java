package server.main.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.admin.entity.Admin;

public interface AdminRepository extends JpaRepository<Admin, Long> {

}
