package com.charity.app.config.seed;

import com.charity.app.common.SlugUtil;
import com.charity.app.model.Center;
import com.charity.app.model.Request;
import com.charity.app.repository.CenterRepository;
import com.charity.app.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Fills in the slugs the migrations deliberately left null.
 *
 * <p>Persian slugs need Unicode work -- folding Arabic yeh and kaf to their Persian forms, stripping
 * zero-width non-joiners, normalising three different digit sets -- that is not reasonably
 * expressible in SQL that has to run on both MySQL and H2. Doing it here also guarantees the stored
 * slugs match exactly what {@link SlugUtil} would generate for a new record, so the two can never
 * drift apart.
 *
 * <p>Runs on every start but only touches rows with a missing slug, so it is a no-op once settled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SlugBackfill {

    private final RequestRepository requests;
    private final CenterRepository centers;

    @Transactional
    public void backfill() {
        backfillRequests();
        backfillCenters();
    }

    private void backfillRequests() {
        List<Request> pending = requests.findAll().stream()
                .filter(request -> isBlank(request.getSlug()) || isBlank(request.getCode()))
                .toList();
        if (pending.isEmpty()) {
            return;
        }
        for (Request request : pending) {
            if (isBlank(request.getCode())) {
                request.setCode("RQ-" + (1000 + request.getId()));
            }
            // The code suffix makes the slug unique by construction, so no collision check is needed.
            request.setSlug(SlugUtil.requestSlug(request.getTitle(), request.getCode()));
            requests.save(request);
        }
        log.info("Backfilled slugs for {} requests", pending.size());
    }

    private void backfillCenters() {
        List<Center> pending = centers.findAll().stream()
                .filter(center -> isBlank(center.getSlug()))
                .toList();
        if (pending.isEmpty()) {
            return;
        }
        for (Center center : pending) {
            String base = SlugUtil.slugify(center.getName(), 180);
            if (base.isEmpty()) {
                base = "center";
            }
            String candidate = base;
            // Centre slugs carry no code, so collisions are resolved with a counter.
            int suffix = 2;
            while (centers.existsBySlug(candidate)) {
                candidate = base + "-" + suffix++;
            }
            center.setSlug(candidate);
            centers.save(center);
        }
        log.info("Backfilled slugs for {} centers", pending.size());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
