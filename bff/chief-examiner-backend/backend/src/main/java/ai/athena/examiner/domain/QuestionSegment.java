package ai.athena.examiner.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "question_segment")
public class QuestionSegment {

    @Id
    private UUID id;

    @Column(name = "script_id", nullable = false)
    private UUID scriptId;

    @Column(name = "label", nullable = false)
    private String label;

    @Column(name = "page_start", nullable = false)
    private int pageStart;

    @Column(name = "page_end", nullable = false)
    private int pageEnd;

    @Column(name = "transcript", nullable = false, columnDefinition = "text")
    private String transcript;

    @Column(name = "confidence")
    private BigDecimal confidence;

    @Column(name = "flags")
    private String flags;

    @Column(name = "seq", nullable = false)
    private int seq;

    protected QuestionSegment() {}

    public QuestionSegment(UUID scriptId, String label, int pageStart, int pageEnd,
                           String transcript, BigDecimal confidence, String flags, int seq) {
        this.id = UUID.randomUUID();
        this.scriptId = scriptId;
        this.label = label;
        this.pageStart = pageStart;
        this.pageEnd = pageEnd;
        this.transcript = transcript;
        this.confidence = confidence;
        this.flags = flags;
        this.seq = seq;
    }

    public UUID getId() { return id; }
    public UUID getScriptId() { return scriptId; }
    public String getLabel() { return label; }
    public int getPageStart() { return pageStart; }
    public int getPageEnd() { return pageEnd; }
    public String getTranscript() { return transcript; }
    public BigDecimal getConfidence() { return confidence; }
    public String getFlags() { return flags; }
    public int getSeq() { return seq; }
}
