package ai.athena.examiner.repo;

import ai.athena.examiner.domain.Script;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScriptRepository extends JpaRepository<Script, UUID> {
    List<Script> findAllByOrderByUploadedAtDesc();
}
