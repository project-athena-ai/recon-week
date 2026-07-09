package ai.athena.examiner.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "script")
public class Script {

    public enum Status { UPLOADED, READING, READ, FAILED }

    @Id
    private UUID id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(name = "paper_id")   private String paperId;
    @Column(name = "exam")       private String exam;
    @Column(name = "paper")      private String paper;
    @Column(name = "slot")       private String slot;
    @Column(name = "day_name")   private String dayName;
    @Column(name = "date_label") private String dateLabel;
    @Column(name = "uploaded_by")private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private OffsetDateTime uploadedAt = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.UPLOADED;

    @Column(name = "page_count")
    private Integer pageCount;

    @Column(name = "error_message")
    private String errorMessage;

    protected Script() {}

    public Script(UUID id, String fileName, long sizeBytes, String storagePath) {
        this.id = id;
        this.fileName = fileName;
        this.sizeBytes = sizeBytes;
        this.storagePath = storagePath;
    }

    public UUID getId() { return id; }
    public String getFileName() { return fileName; }
    public long getSizeBytes() { return sizeBytes; }
    public String getStoragePath() { return storagePath; }
    public String getPaperId() { return paperId; }
    public String getExam() { return exam; }
    public String getPaper() { return paper; }
    public String getSlot() { return slot; }
    public String getDayName() { return dayName; }
    public String getDateLabel() { return dateLabel; }
    public String getUploadedBy() { return uploadedBy; }
    public OffsetDateTime getUploadedAt() { return uploadedAt; }
    public Status getStatus() { return status; }
    public Integer getPageCount() { return pageCount; }
    public String getErrorMessage() { return errorMessage; }

    public void setPaperId(String v) { this.paperId = v; }
    public void setExam(String v) { this.exam = v; }
    public void setPaper(String v) { this.paper = v; }
    public void setSlot(String v) { this.slot = v; }
    public void setDayName(String v) { this.dayName = v; }
    public void setDateLabel(String v) { this.dateLabel = v; }
    public void setUploadedBy(String v) { this.uploadedBy = v; }
    public void setStatus(Status v) { this.status = v; }
    public void setPageCount(Integer v) { this.pageCount = v; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
}
