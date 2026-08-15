package com.charity.app.service;

import com.charity.app.common.AppUrls;
import com.charity.app.model.Request;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shared behaviour for the Telegram- and Bale-style bot APIs, which are the same protocol behind
 * different hosts. The two services were previously near-identical 160-line copies of each other.
 */
@Slf4j
public abstract class AbstractBotMessagingService implements MessagingService {

    private static final Locale FA = Locale.forLanguageTag("fa-IR");

    protected final RestTemplate restTemplate;
    protected final AppUrls urls;

    protected AbstractBotMessagingService(RestTemplate restTemplate, AppUrls urls) {
        this.restTemplate = restTemplate;
        this.urls = urls;
    }

    protected abstract String botToken();

    protected abstract String channel();

    protected abstract boolean configuredEnabled();

    /** Base URL of the bot API, without the token. */
    protected abstract String apiHost();

    @Override
    public boolean isEnabled() {
        String token = botToken();
        return configuredEnabled()
                && token != null
                && !token.isBlank()
                && !token.startsWith("REPLACE")
                && channel() != null
                && !channel().isBlank();
    }

    @Override
    public Integer publishRequest(Request request) {
        if (!isEnabled()) {
            // INFO, not DEBUG. The root level is INFO and nothing configures it lower, so a channel
            // that is switched off or half-configured used to be completely silent -- indistinguishable
            // from one that posted successfully. That silence is most of why this took so long to find.
            log.info("{} is disabled or not fully configured; skipping announcement for request {}",
                    getName(), request.getId());
            return null;
        }
        try {
            Integer messageId = sendMessage(buildMessageText(request));
            if (messageId == null) {
                return null;
            }
            if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
                trySend("sendPhoto", "photo", request.getImageUrl(), "📸 " + request.getTitle());
            }
            List<String> documents = request.getDocuments();
            if (documents != null) {
                documents.forEach(doc -> trySend("sendDocument", "document", doc, "📎 " + doc));
            }
            return messageId;
        } catch (Exception e) {
            log.error("Failed to announce request {} on {}", request.getId(), getName(), e);
            return null;
        }
    }

    protected String buildMessageText(Request request) {
        NumberFormat numbers = NumberFormat.getNumberInstance(FA);
        StringBuilder text = new StringBuilder();

        text.append("🆘 *درخواست کمک خیریه*\n\n");
        text.append("📌 *عنوان:* ").append(escape(request.getTitle())).append("\n");
        if (request.getCategory() != null) {
            text.append("🏷 *دسته‌بندی:* ").append(escape(request.getCategory().getName())).append("\n");
        }
        // The beneficiary's name used to be printed here, out of details.beneficiaryName. The site
        // never showed it and the privacy page promises it is never published, so posting it to a
        // public channel contradicted both. The field is gone from the model entirely now.
        if (request.getCenter() != null) {
            text.append("🏥 *مرکز:* ").append(escape(request.getCenter().getName())).append("\n");
            if (request.getCenter().getCity() != null) {
                text.append("📍 *شهر:* ").append(escape(request.getCenter().getCity().getName())).append("\n");
            }
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            text.append("📝 *توضیحات:* ").append(escape(request.getDescription())).append("\n");
        }
        if (request.getAmountNeeded() != null) {
            text.append("💰 *مبلغ مورد نیاز:* ").append(numbers.format(request.getAmountNeeded())).append(" تومان\n");
        }
        if (request.getCenter() != null && request.getCenter().getContactPhone() != null) {
            text.append("📞 *تماس:* ").append(escape(request.getCenter().getContactPhone())).append("\n");
        }
        // Points at the public site. This used to be built from the backend's own base URL, so every
        // link posted to the channel pointed at the API port and 404'd.
        text.append("\n🌐 مشاهده در سایت: ").append(urls.requestUrl(request.getSlug()));

        return text.toString();
    }

    private Integer sendMessage(String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", channel());
        body.put("text", text);
        body.put("parse_mode", "Markdown");
        body.put("disable_web_page_preview", false);

        Map<?, ?> response = restTemplate.postForObject(apiUrl() + "/sendMessage", body, Map.class);
        if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
            Map<?, ?> result = (Map<?, ?>) response.get("result");
            return result == null ? null : (Integer) result.get("message_id");
        }
        log.error("{} sendMessage failed: {}", getName(), response);
        return null;
    }

    /** An attachment failing must not fail the announcement that already went out. */
    private void trySend(String method, String field, String filename, String caption) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", channel());
            body.put(field, urls.fileUrl(filename));
            body.put("caption", caption);
            restTemplate.postForObject(apiUrl() + "/" + method, body, Map.class);
        } catch (Exception e) {
            log.warn("Failed to send {} {} on {}", field, filename, getName(), e);
        }
    }

    private String apiUrl() {
        return apiHost() + botToken();
    }

    protected static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("`", "\\`");
    }
}
