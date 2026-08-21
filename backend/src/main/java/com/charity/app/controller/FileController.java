package com.charity.app.controller;

import com.charity.app.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/public/files")
@RequiredArgsConstructor
public class FileController {

    /**
     * The only content types served inline. Everything else -- office documents included -- gets
     * {@code Content-Disposition: attachment}.
     */
    private static final List<MediaType> INLINE_TYPES =
            List.of(MediaType.valueOf("image/*"), MediaType.APPLICATION_PDF);

    private final FileStorageService storage;

    /**
     * Serves an uploaded file.
     *
     * <p>Previously every file came back as {@code application/octet-stream} with
     * {@code Content-Disposition: attachment}, which meant no uploaded image would ever render in an
     * {@code <img>} tag or in a social/messenger link preview -- both of which the redesign depends
     * on. Images and PDFs are now served inline with their real content type.
     *
     * <p>Inline is an allowlist, not the absence of a blocklist. {@code docx} and {@code xlsx} are
     * accepted uploads now and {@code MediaTypeFactory} gives them their real OOXML content types,
     * which are on nobody's list here -- so they download as attachments, which is the only sane
     * thing a browser can do with them anyway. Anything else new lands on the same side by default.
     */
    @GetMapping("/{filename}")
    public ResponseEntity<Resource> download(@PathVariable String filename) throws IOException {
        Path path = storage.resolve(filename);
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        MediaType contentType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        boolean inline = INLINE_TYPES.stream().anyMatch(allowed -> allowed.includes(contentType));

        ContentDisposition disposition = ContentDisposition
                .builder(inline ? "inline" : "attachment")
                .filename(filename, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                // Stored names are random UUIDs, so a given URL's content never changes.
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000, immutable")
                // Serving inline makes sniffing protection matter: without it a browser could be
                // talked into interpreting a file as something other than its declared type.
                .header("X-Content-Type-Options", "nosniff")
                .contentLength(Files.size(path))
                .lastModified(Files.getLastModifiedTime(path).toMillis())
                .body(new UrlResource(path.toUri()));
    }
}
