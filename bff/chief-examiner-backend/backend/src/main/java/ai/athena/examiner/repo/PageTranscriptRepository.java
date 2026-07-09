package ai.athena.examiner.repo;

import ai.athena.examiner.domain.PageTranscript;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PageTranscriptRepository extends JpaRepository<PageTranscript, UUID> {
    List<PageTranscript> findByScriptIdOrderByPageNumber(UUID scriptId);
    void deleteByScriptId(UUID scriptId);
}
