package ai.athena.examiner.web;

import ai.athena.examiner.domain.PageTranscript;
import ai.athena.examiner.domain.QuestionSegment;
import ai.athena.examiner.domain.Script;
import ai.athena.examiner.repo.PageTranscriptRepository;
import ai.athena.examiner.repo.QuestionSegmentRepository;
import ai.athena.examiner.repo.ScriptRepository;
import ai.athena.examiner.service.EvidenceService;
import ai.athena.examiner.service.OpenAiClient;
import ai.athena.examiner.service.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/scripts")
public class ScriptController {

    private final ScriptRepository scripts;
    private final PageTranscriptRepository pages;
    private final QuestionSegmentRepository segments;
    private final StorageService storage;
    private final EvidenceService evidence;
    private final OpenAiClient openAi;

    public ScriptController(ScriptRepository scripts, PageTranscriptRepository pages,
                            QuestionSegmentRepository segments, StorageService storage,
                            EvidenceService evidence, OpenAiClient openAi) {
        this.scripts = scripts;
        this.pages = pages;
        this.segments = segments;
        this.storage = storage;
        this.evidence = evidence;
        this.openAi = openAi;
    }

    /** Upload a handwritten script PDF with the calendar-card metadata. */
    @PostMapping
    public ScriptDto upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(required = false) String paperId,
                            @RequestParam(required = false) String exam,
                            @RequestParam(required = false) String paper,
                            @RequestParam(required = false) String slot,
                            @RequestParam(required = false) String day,
                            @RequestParam(required = false) String date,
                            @RequestParam(required = false) String uploadedBy) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        String name = file.getOriginalFilename() == null ? "script.pdf" : file.getOriginalFilename();
        if (!name.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF scripts are accepted");
        }

        UUID id = UUID.randomUUID();
        Path stored = storage.save(id, file);
        Script s = new Script(id, name, file.getSize(), stored.toString());
        s.setPaperId(paperId);
        s.setExam(exam);
        s.setPaper(paper);
        s.setSlot(slot);
        s.setDayName(day);
        s.setDateLabel(date);
        s.setUploadedBy(uploadedBy);
        scripts.save(s);
        return ScriptDto.from(s);
    }

    /** Trigger "Read handwritten evidence" for a script. Returns 202; poll GET /{id}. */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> read(@PathVariable UUID id) {
        if (!openAi.isConfigured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_API_KEY is not configured on the server");
        }
        Script s = scripts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such script"));
        if (s.getStatus() == Script.Status.READING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already reading");
        }
        evidence.readEvidenceAsync(id);
        return ResponseEntity.accepted().body(Map.of("status", "READING"));
    }

    @GetMapping
    public List<ScriptDto> list() {
        return scripts.findAllByOrderByUploadedAtDesc().stream().map(ScriptDto::from).toList();
    }

    @GetMapping("/{id}")
    public ScriptDto get(@PathVariable UUID id) {
        return scripts.findById(id).map(ScriptDto::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such script"));
    }

    /** Full reading result: per-page transcripts + question segments. */
    @GetMapping("/{id}/transcript")
    public TranscriptDto transcript(@PathVariable UUID id) {
        Script s = scripts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No such script"));
        List<PageDto> p = pages.findByScriptIdOrderByPageNumber(id).stream().map(PageDto::from).toList();
        List<SegmentDto> g = segments.findByScriptIdOrderBySeq(id).stream().map(SegmentDto::from).toList();
        return new TranscriptDto(ScriptDto.from(s), g, p);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        scripts.deleteById(id);
    }

    // ---------- DTOs ----------

    public record ScriptDto(UUID id, String fileName, long sizeBytes, String paperId,
                            String exam, String paper, String slot, String day, String date,
                            String uploadedBy, OffsetDateTime uploadedAt,
                            String status, Integer pageCount, String errorMessage) {
        static ScriptDto from(Script s) {
            return new ScriptDto(s.getId(), s.getFileName(), s.getSizeBytes(), s.getPaperId(),
                    s.getExam(), s.getPaper(), s.getSlot(), s.getDayName(), s.getDateLabel(),
                    s.getUploadedBy(), s.getUploadedAt(),
                    s.getStatus().name(), s.getPageCount(), s.getErrorMessage());
        }
    }

    public record PageDto(int page, String transcript, BigDecimal confidence, boolean illegible) {
        static PageDto from(PageTranscript p) {
            return new PageDto(p.getPageNumber(), p.getTranscript(), p.getConfidence(), p.isIllegible());
        }
    }

    public record SegmentDto(String label, int pageStart, int pageEnd, String transcript,
                             BigDecimal confidence, String flags) {
        static SegmentDto from(QuestionSegment s) {
            return new SegmentDto(s.getLabel(), s.getPageStart(), s.getPageEnd(),
                    s.getTranscript(), s.getConfidence(), s.getFlags());
        }
    }

    public record TranscriptDto(ScriptDto script, List<SegmentDto> segments, List<PageDto> pages) {}
}
