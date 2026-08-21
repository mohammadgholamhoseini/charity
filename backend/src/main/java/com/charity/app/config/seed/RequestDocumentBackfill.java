package com.charity.app.config.seed;

import com.charity.app.common.JsonListConverter;
import com.charity.app.model.DocumentCategory;
import com.charity.app.model.Request;
import com.charity.app.model.RequestDocument;
import com.charity.app.model.enums.DocumentScope;
import com.charity.app.repository.DocumentCategoryRepository;
import com.charity.app.repository.RequestDocumentRepository;
import com.charity.app.repository.RequestRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Copies the old {@code requests.documents_json} array into {@code request_documents}.
 *
 * <p>Not a SQL migration, for the same reason slug generation is not: the column holds a JSON array
 * in a {@code TEXT} column, and unpacking one needs {@code JSON_TABLE}, which MySQL 8.4 has and H2
 * does not. A migration that only ran on one of the two profiles would leave the {@code local}
 * profile -- the only gate this project has -- unable to reproduce production's data shape.
 *
 * <p>Old rows carry nothing but a stored filename: no category, no title, no size. They are filed
 * under the seeded {@code (REQUEST, 'general')} category, which is why V12 seeds it.
 *
 * <p>Runs on every start and skips any request that already has documents, exactly as
 * {@link SlugBackfill} does, so it is a no-op once settled and safe if it is interrupted halfway.
 * The source column is deliberately left in place; dropping it is a later migration, once this has
 * been observed to have run in production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestDocumentBackfill {

    /** The category old documents land in. Seeded by V12; absent only if an admin deleted it. */
    private static final String FALLBACK_CATEGORY_SLUG = "general";

    private final RequestRepository requests;
    private final RequestDocumentRepository requestDocuments;
    private final DocumentCategoryRepository documentCategories;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void backfill() {
        List<Object[]> rows = pendingRows();
        if (rows.isEmpty()) {
            return;
        }

        Optional<DocumentCategory> fallback =
                documentCategories.findByScopeAndSlug(DocumentScope.REQUEST, FALLBACK_CATEGORY_SLUG);
        if (fallback.isEmpty()) {
            // Nothing to file them under and no business inventing a category on the fly. Loud,
            // because it means documents that existed before are not being shown any more.
            log.warn("{} requests still carry documents_json but the '{}' document category is gone; "
                    + "recreate it and restart to backfill them", rows.size(), FALLBACK_CATEGORY_SLUG);
            return;
        }

        JsonListConverter parser = new JsonListConverter();
        int migrated = 0;
        int touched = 0;
        for (Object[] row : rows) {
            Long requestId = ((Number) row[0]).longValue();
            // Idempotent, and the reason a half-finished run can simply be repeated.
            if (requestDocuments.existsByRequestId(requestId)) {
                continue;
            }
            List<String> filenames = parser.convertToEntityAttribute((String) row[1]);
            if (filenames.isEmpty()) {
                continue;
            }
            Request request = requests.findById(requestId).orElse(null);
            if (request == null) {
                continue;
            }
            for (int i = 0; i < filenames.size(); i++) {
                String filename = filenames.get(i);
                if (filename == null || filename.isBlank()) {
                    continue;
                }
                requestDocuments.save(RequestDocument.builder()
                        .request(request)
                        .category(fallback.get())
                        .storedFilename(filename)
                        .sortOrder(i)
                        .build());
                migrated++;
            }
            touched++;
        }
        if (migrated > 0) {
            log.info("Backfilled {} documents on {} requests from documents_json", migrated, touched);
        }
    }

    /**
     * Native, because the entity no longer maps {@code documents_json} -- that is the whole point of
     * this class. Reads only the two columns it needs.
     */
    @SuppressWarnings("unchecked")
    private List<Object[]> pendingRows() {
        return entityManager.createNativeQuery("""
                SELECT id, documents_json FROM requests
                 WHERE documents_json IS NOT NULL AND documents_json <> '[]'
                """).getResultList();
    }
}
