package com.charity.app.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * The one place that knows the deployment's URLs and timezone.
 *
 * <p>These used to be a single {@code app.base-url} pointing at the backend port, which meant every
 * "view on site" link posted to Telegram and Bale pointed at the API rather than the site and 404'd
 * in every deployment. The public site and the API are separate hosts and now have separate config.
 */
@Component
public class AppUrls {

    private final String apiBaseUrl;
    private final String siteBaseUrl;
    private final ZoneId zoneId;

    public AppUrls(@Value("${app.base-url}") String apiBaseUrl,
                   @Value("${app.site-url}") String siteBaseUrl,
                   @Value("${app.timezone:Asia/Tehran}") String timezone) {
        this.apiBaseUrl = stripTrailingSlash(apiBaseUrl);
        this.siteBaseUrl = stripTrailingSlash(siteBaseUrl);
        this.zoneId = ZoneId.of(timezone);
    }

    public String site() {
        return siteBaseUrl;
    }

    public String api() {
        return apiBaseUrl;
    }

    public ZoneId zone() {
        return zoneId;
    }

    /** Absolute public URL of a request. Percent-encoded, because slugs are Persian. */
    public String requestUrl(String slug) {
        return siteBaseUrl + "/requests/" + encodePathSegment(slug);
    }

    public String centerUrl(String slug) {
        return siteBaseUrl + "/centers/" + encodePathSegment(slug);
    }

    public String categoryUrl(String slug) {
        return siteBaseUrl + "/requests/category/" + encodePathSegment(slug);
    }

    /** Absolute URL of an uploaded file. This one is served by the API, not the site. */
    public String fileUrl(String filename) {
        return filename == null || filename.isBlank()
                ? null
                : apiBaseUrl + "/api/public/files/" + encodePathSegment(filename);
    }

    /**
     * Attaches the configured zone to a stored local date-time.
     *
     * <p>Timestamps are stored as local date-times and were previously exposed as
     * {@code "yyyy-MM-dd HH:mm"}, which carries no offset and cannot be parsed by a crawler or
     * dropped into {@code <time datetime>}. Converting here gives a real ISO-8601 offset without
     * migrating the columns.
     */
    public OffsetDateTime iso(LocalDateTime value) {
        return value == null ? null : value.atZone(zoneId).toOffsetDateTime();
    }

    private static String encodePathSegment(String value) {
        // URLEncoder is form-encoding, which turns a space into '+'; path segments want %20.
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String stripTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
