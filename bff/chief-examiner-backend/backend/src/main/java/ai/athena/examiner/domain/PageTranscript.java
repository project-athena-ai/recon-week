package ai.athena.examiner.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "page_transcript")
public class PageTranscript {

    @Id
    private UUID id;

    @Column(name = "script_id", nullable = false)
    private UUID scriptId;

    @Column(name = "page_number", nullable = false)
    private int pageNumber;

    @Column(name = "transcript", nullable = false, columnDefinition = "text")
    private String transcript;

    @Column(name = "confidence")
    private BigDecimal confidence;

    @Column(name = "illegible", nullable = false)
    private boolean illegible;

    protected PageTranscript() {}

    public PageTranscript(UUID scriptId, int pageNumber, String transcript,
                          BigDecimal confidence, boolean illegible) {
        this.id = UUID.randomUUID();
        this.scriptId = scriptId;
        this.pageNumber = pageNumber;
        this.transcript = transcript;
        this.confidence = confidence;
        this.illegible = illegible;
    }

    public UUID getId() { return id; }
    public UUID getScriptId() { return scriptId; }
    public int getPageNumber() { return pageNumber; }
    public String getTranscript() { return transcript; }
    public BigDecimal getConfidence() { return confidence; }
    public boolean isIllegible() { return illegible; }
}
