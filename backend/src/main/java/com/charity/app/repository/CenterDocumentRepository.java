package com.charity.app.repository;

import com.charity.app.model.CenterDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CenterDocumentRepository extends JpaRepository<CenterDocument, Long> {

    List<CenterDocument> findByCenterIdOrderBySortOrderAscIdAsc(Long centerId);

    long countByCenterId(Long centerId);

    long countByCategoryId(Long categoryId);

    /** Guards the unlink -- see {@link RequestDocumentRepository#countByStoredFilename}. */
    long countByStoredFilename(String storedFilename);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE CenterDocument d SET d.category.id = :replacementId WHERE d.category.id = :categoryId")
    int reassignCategory(@Param("categoryId") Long categoryId, @Param("replacementId") Long replacementId);
}
