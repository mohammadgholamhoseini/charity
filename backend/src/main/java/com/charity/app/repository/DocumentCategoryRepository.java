package com.charity.app.repository;

import com.charity.app.model.DocumentCategory;
import com.charity.app.model.enums.DocumentScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * One table, two lists. Every finder here is scoped, because uniqueness is per scope: «صورت مالی»
 * may exist once for requests and once for centres.
 */
@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {

    /** Feeds the upload pickers and the public endpoint. */
    List<DocumentCategory> findByScopeAndActiveTrueOrderBySortOrderAscNameAsc(DocumentScope scope);

    /** Feeds the admin table, which shows inactive categories too. */
    List<DocumentCategory> findByScopeOrderBySortOrderAscNameAsc(DocumentScope scope);

    Optional<DocumentCategory> findByScopeAndSlug(DocumentScope scope, String slug);

    boolean existsByScopeAndName(DocumentScope scope, String name);

    boolean existsByScopeAndNameAndIdNot(DocumentScope scope, String name, Long id);

    boolean existsByScopeAndSlug(DocumentScope scope, String slug);

    boolean existsByScopeAndSlugAndIdNot(DocumentScope scope, String slug, Long id);
}
