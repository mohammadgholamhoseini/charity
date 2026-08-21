package com.charity.app.repository;

import com.charity.app.model.RequestDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestDocumentRepository extends JpaRepository<RequestDocument, Long> {

    List<RequestDocument> findByRequestIdOrderBySortOrderAscIdAsc(Long requestId);

    /** Enforces the per-owner cap before a single byte is written to disk. */
    long countByRequestId(Long requestId);

    /** Answers the question a TEXT blob could not: is this document category still in use? */
    long countByCategoryId(Long categoryId);

    /**
     * Guards the unlink. Two rows may legitimately point at the same stored file only if a future
     * feature copies one, but deleting the file out from under a surviving row would be silent
     * data loss, so the check is made rather than assumed.
     */
    long countByStoredFilename(String storedFilename);

    boolean existsByRequestId(Long requestId);

    /** Bulk move used when an admin deletes a document category and nominates a replacement. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RequestDocument d SET d.category.id = :replacementId WHERE d.category.id = :categoryId")
    int reassignCategory(@Param("categoryId") Long categoryId, @Param("replacementId") Long replacementId);
}
