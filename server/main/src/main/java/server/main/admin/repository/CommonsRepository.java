package server.main.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.main.admin.entity.Common;

public interface CommonsRepository extends JpaRepository<Common, Long> {
    // 배당일 조회
    @Query("SELECT c FROM Common c")
    Common findAllocateDate();
}
