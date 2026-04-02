package server.main.diclosure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.diclosure.entity.Disclosure;

public interface DisclosureRepository extends JpaRepository<Disclosure, Long> {
}
