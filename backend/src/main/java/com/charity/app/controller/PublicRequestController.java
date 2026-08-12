package com.charity.app.controller;

import com.charity.app.common.AppUrls;
import com.charity.app.common.Paging;
import com.charity.app.model.enums.RequestStatus;
import com.charity.app.model.enums.Urgency;
import com.charity.app.payload.RequestDetailResponse;
import com.charity.app.payload.RequestFilter;
import com.charity.app.payload.RequestSummary;
import com.charity.app.service.RequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Duration;
import java.util.List;

@RestController
@RequestMapping("/api/public/requests")
@RequiredArgsConstructor
public class PublicRequestController {

    private final RequestService requests;
    private final AppUrls urls;

    /**
     * The public listing. Every parameter is optional and all of them combine.
     *
     * <p>Categories and cities are accepted by slug and name rather than by id, so the frontend can
     * put readable, shareable values in the query string instead of database identifiers.
     */
    @GetMapping
    public ResponseEntity<Page<RequestSummary>> list(
            @RequestParam(required = false) List<String> category,
            @RequestParam(required = false) List<Long> categoryId,
            @RequestParam(required = false) List<Urgency> urgency,
            @RequestParam(required = false) List<String> city,
            @RequestParam(required = false) List<Long> cityId,
            @RequestParam(required = false) Long provinceId,
            @RequestParam(required = false) String center,
            @RequestParam(required = false) Long centerId,
            @RequestParam(required = false) List<RequestStatus> status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {

        RequestFilter filter = new RequestFilter(
                categoryId, category, urgency, cityId, city, provinceId, centerId, center, status, q);

        Page<RequestSummary> result = requests.publicList(filter, Paging.of(page, size, sort));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(1)).cachePublic())
                .header("X-Total-Count", String.valueOf(result.getTotalElements()))
                .body(result);
    }

    /**
     * A request by slug.
     *
     * <p>Answers 301 when the slug is stale, 410 when the request was deleted, and 404 when it never
     * existed or was never public. Getting these apart matters: a 404 for something that has moved
     * discards a URL's ranking, and a 410 gets a removed page out of the index far faster.
     */
    @GetMapping("/{slug}")
    public ResponseEntity<RequestDetailResponse> detail(@PathVariable String slug, WebRequest webRequest) {
        String canonical = requests.canonicalSlugFor(slug);
        if (canonical != null) {
            return redirectTo(urls.requestUrl(canonical));
        }

        RequestDetailResponse detail = requests.publicDetail(slug);

        // Conditional GET: repeat visits and crawler re-checks cost a 304 rather than a full render.
        long lastModified = detail.updatedAt() == null ? -1 : detail.updatedAt().toInstant().toEpochMilli();
        if (lastModified > 0 && webRequest.checkNotModified(lastModified)) {
            return null;
        }
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
                .lastModified(lastModified)
                .body(detail);
    }

    /** Lets a visitor paste the printed code «۱۰۲۴» and land on the right page. */
    @GetMapping("/by-code/{code}")
    public ResponseEntity<Void> byCode(@PathVariable String code) {
        return redirectTo(urls.requestUrl(requests.slugForCode(code)));
    }

    /**
     * Resolves the numeric ids used by {@code /case/{id}} links already posted to the Telegram and
     * Bale channels, so none of them break.
     */
    @GetMapping("/legacy/{id}")
    public ResponseEntity<Void> legacy(@PathVariable Long id) {
        return redirectTo(urls.requestUrl(requests.slugForLegacyId(id)));
    }

    private <T> ResponseEntity<T> redirectTo(String location) {
        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI.create(location)).build();
    }
}
