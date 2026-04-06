package server.main.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.main.admin.entity.Commons;

public interface CommonsRepository extends JpaRepository<Commons, Long> {
    // 배당일 조회
    @Query("SELECT c.allocateDate FROM Commons c")
    int findAllocateDate();
}
