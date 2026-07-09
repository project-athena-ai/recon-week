package ai.athena.examiner.repo;

import ai.athena.examiner.domain.QuestionSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QuestionSegmentRepository extends JpaRepository<QuestionSegment, UUID> {
    List<QuestionSegment> findByScriptIdOrderBySeq(UUID scriptId);
    void deleteByScriptId(UUID scriptId);
}
