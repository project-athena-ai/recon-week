package ai.athena.examiner.service;

import ai.athena.examiner.domain.PageTranscript;
import ai.athena.examiner.domain.QuestionSegment;
import ai.athena.examiner.domain.Script;
import ai.athena.examiner.repo.PageTranscriptRepository;
import ai.athena.examiner.repo.QuestionSegmentRepository;
import ai.athena.examiner.repo.ScriptRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * "Read handwritten evidence" — pipeline stage 1 of the Chief Examiner.
 *
 * Receive Script → Evidence Extraction → Question Segmentation
 *
 * Spec behaviours enforced in prompts:
 *  - never invent unreadable handwriting (mark [illegible])
 *  - never hide uncertainty (confidence on every page and segment)
 */
@Service
public class EvidenceService {

    private static final Logger log = LoggerFactory.getLogger(EvidenceService.class);

    private static final String PAGE_PROMPT = """
        You are the Chief Examiner's evidence reader for Cambridge IGCSE handwritten scripts.
        The images are consecutive pages of one student's handwritten exam script.
        The first image is page %d.

        For EACH page, transcribe the handwriting faithfully:
        - Preserve the student's working, including crossed-out work (wrap it in ~~strikethrough~~).
        - Preserve mathematical notation in plain text (use ^ for powers, / for fractions).
        - If a word, symbol or region cannot be read with confidence, write [illegible] — NEVER guess or invent.
        - Note any question labels the student wrote (e.g. "1(a)", "3b ii", "Q7").
        - Give a confidence score 0.0–1.0 for how completely the page was read.

        Respond ONLY with JSON, no markdown, matching exactly:
        {"pages":[{"page":%d,"transcript":"...","confidence":0.0,"illegible":false,"question_labels":["1(a)"]}]}
        One object per page image, in order.
        """;

    private static final String SEGMENT_PROMPT = """
        You are the Chief Examiner's question segmenter for a Cambridge IGCSE handwritten script.
        Below are faithful per-page transcripts of one student's script. [illegible] marks unreadable
        handwriting — do not replace it with guesses.

        Segment the evidence by exam question part, in the order the answers appear.
        - Use canonical labels like "1(a)", "1(b)(ii)", "7". Keep the student's numbering if visible;
          infer from context only when the continuation is obvious (e.g. working flowing across a page break).
        - A segment may span pages. Record page_start and page_end (1-based).
        - Copy the relevant transcript text into each segment verbatim, including [illegible] markers.
        - flags: short notes such as "partially illegible", "crossed-out working", "answer continues in margin",
          or "" if none.
        - confidence 0.0–1.0: how sure you are the text belongs to that question label.
        - If some text cannot be attributed to any question, create a segment labelled "UNATTRIBUTED".

        Respond ONLY with JSON, no markdown, matching exactly:
        {"segments":[{"label":"1(a)","page_start":1,"page_end":1,"transcript":"...","confidence":0.0,"flags":""}]}

        PAGE TRANSCRIPTS:
        %s
        """;

    private final ScriptRepository scripts;
    private final PageTranscriptRepository pages;
    private final QuestionSegmentRepository segments;
    private final StorageService storage;
    private final OpenAiClient openAi;
    private final int renderDpi;
    private final int pagesPerBatch;

    public EvidenceService(ScriptRepository scripts,
                           PageTranscriptRepository pages,
                           QuestionSegmentRepository segments,
                           StorageService storage,
                           OpenAiClient openAi,
                           @Value("${athena.openai.render-dpi}") int renderDpi,
                           @Value("${athena.openai.pages-per-batch}") int pagesPerBatch) {
        this.scripts = scripts;
        this.pages = pages;
        this.segments = segments;
        this.storage = storage;
        this.openAi = openAi;
        this.renderDpi = renderDpi;
        this.pagesPerBatch = Math.max(1, pagesPerBatch);
    }

    /** Kicks off reading in the background; caller returns 202 immediately. */
    @Async
    public void readEvidenceAsync(UUID scriptId) {
        try {
            readEvidence(scriptId);
        } catch (Exception e) {
            log.error("Evidence reading failed for {}", scriptId, e);
            markFailed(scriptId, e.getMessage());
        }
    }

    void readEvidence(UUID scriptId) throws Exception {
        Script script = scripts.findById(scriptId).orElseThrow();

        // 1. Evidence extraction: render PDF pages to PNG
        List<byte[]> pngs = renderPdf(script);
        updatePageCount(scriptId, pngs.size());

        // 2. Transcribe in batches with the vision model
        List<PageResult> pageResults = new ArrayList<>();
        for (int start = 0; start < pngs.size(); start += pagesPerBatch) {
            int end = Math.min(start + pagesPerBatch, pngs.size());
            List<byte[]> batch = pngs.subList(start, end);
            String prompt = PAGE_PROMPT.formatted(start + 1, start + 1);
            JsonNode json = openAi.generateJson(prompt, batch);
            for (JsonNode p : json.path("pages")) {
                pageResults.add(new PageResult(
                        p.path("page").asInt(start + 1),
                        p.path("transcript").asText(""),
                        BigDecimal.valueOf(p.path("confidence").asDouble(0.0)),
                        p.path("illegible").asBoolean(false)));
            }
        }

        // 3. Question segmentation over the full transcript (text-only call)
        StringBuilder sb = new StringBuilder();
        for (PageResult p : pageResults) {
            sb.append("--- PAGE ").append(p.page()).append(" (confidence ")
              .append(p.confidence()).append(") ---\n")
              .append(p.transcript()).append("\n\n");
        }
        JsonNode segJson = openAi.generateJson(SEGMENT_PROMPT.formatted(sb), List.of());

        persistResults(scriptId, pageResults, segJson);
        log.info("Script {} read: {} pages, {} segments",
                scriptId, pageResults.size(), segJson.path("segments").size());
    }

    private List<byte[]> renderPdf(Script script) throws Exception {
        try (PDDocument doc = Loader.loadPDF(storage.resolve(script.getStoragePath()).toFile())) {
            PDFRenderer renderer = new PDFRenderer(doc);
            List<byte[]> out = new ArrayList<>(doc.getNumberOfPages());
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, renderDpi);
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ImageIO.write(img, "png", buf);
                out.add(buf.toByteArray());
            }
            return out;
        }
    }

    @Transactional
    void updatePageCount(UUID scriptId, int count) {
        Script s = scripts.findById(scriptId).orElseThrow();
        s.setPageCount(count);
        s.setStatus(Script.Status.READING);
        scripts.save(s);
    }

    @Transactional
    void persistResults(UUID scriptId, List<PageResult> pageResults, JsonNode segJson) {
        // idempotent re-runs: clear previous results
        pages.deleteByScriptId(scriptId);
        segments.deleteByScriptId(scriptId);

        for (PageResult p : pageResults) {
            pages.save(new PageTranscript(scriptId, p.page(), p.transcript(),
                    p.confidence(), p.illegible()));
        }
        int seq = 0;
        for (JsonNode s : segJson.path("segments")) {
            segments.save(new QuestionSegment(
                    scriptId,
                    s.path("label").asText("UNATTRIBUTED"),
                    s.path("page_start").asInt(1),
                    s.path("page_end").asInt(1),
                    s.path("transcript").asText(""),
                    BigDecimal.valueOf(s.path("confidence").asDouble(0.0)),
                    s.path("flags").asText(""),
                    seq++));
        }
        Script script = scripts.findById(scriptId).orElseThrow();
        script.setStatus(Script.Status.READ);
        script.setErrorMessage(null);
        scripts.save(script);
    }

    @Transactional
    void markFailed(UUID scriptId, String message) {
        scripts.findById(scriptId).ifPresent(s -> {
            s.setStatus(Script.Status.FAILED);
            s.setErrorMessage(message == null ? "unknown error"
                    : message.substring(0, Math.min(message.length(), 990)));
            scripts.save(s);
        });
    }

    record PageResult(int page, String transcript, BigDecimal confidence, boolean illegible) {}
}
