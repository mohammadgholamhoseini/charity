package com.charity.app.service;

import com.charity.app.model.CharityCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class BaleService implements MessagingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${bale.bot-token}")
    private String botToken;

    @Value("${bale.channel}")
    private String channel;

    @Value("${bale.enabled:false}")
    private boolean enabled;

    @Value("${app.base-url:http://localhost:${server.port:8085}}")
    private String baseUrl;

    @Override
    public String getName() {
        return "bale";
    }

    @Override
    public boolean isEnabled() {
        return enabled && !botToken.startsWith("REPLACE");
    }

    private String apiUrl() {
        return "https://tapi.bale.ai/bot" + botToken;
    }

    @Override
    public Integer publishCase(CharityCase c) {
        if (!isEnabled()) {
            log.warn("Bale disabled or token not set. Skipping publish for case {}", c.getId());
            return null;
        }
        try {
            String text = buildMessageText(c);
            Integer msgId = sendMessage(text);
            if (msgId == null) return null;

            String imageUrl = c.getImageUrl();
            if (imageUrl != null && !imageUrl.isBlank()) {
                try {
                    sendPhoto(channel, imageUrl, "📸 " + c.getTitle());
                } catch (Exception e) {
                    log.error("Failed to send image {} to Bale", imageUrl, e);
                }
            }

            List<String> docs = c.getDocuments();
            if (docs != null && !docs.isEmpty()) {
                for (String doc : docs) {
                    try {
                        sendDocument(channel, doc, "📎 " + doc);
                    } catch (Exception e) {
                        log.error("Failed to send document {} to Bale", doc, e);
                    }
                }
            }

            return msgId;
        } catch (Exception e) {
            log.error("Error publishing case {} to Bale", c.getId(), e);
            return null;
        }
    }

    private String buildMessageText(CharityCase c) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("fa", "IR"));
        String amount = nf.format(c.getAmountNeeded());

        String categoryName = c.getCategory() != null ? c.getCategory().getName() : "";
        Map<String, Object> details = c.getDetails();
        String beneficiary = details != null && details.get("beneficiaryName") != null
                ? details.get("beneficiaryName").toString() : "";

        StringBuilder text = new StringBuilder();
        text.append("🆘 *درخواست کمک خیریه*\n\n");
        text.append("📌 *عنوان:* ").append(escape(c.getTitle())).append("\n");
        if (!categoryName.isBlank()) {
            text.append("🏷 *دسته‌بندی:* ").append(escape(categoryName)).append("\n");
        }
        if (!beneficiary.isBlank()) {
            text.append("👤 *ذینفع:* ").append(escape(beneficiary)).append("\n");
        }
        if (c.getCenter() != null && c.getCenter().getName() != null) {
            text.append("🏥 *مرکز:* ").append(escape(c.getCenter().getName())).append("\n");
        }
        if (c.getDescription() != null && !c.getDescription().isBlank()) {
            text.append("📝 *توضیحات:* ").append(escape(c.getDescription())).append("\n");
        }
        text.append("💰 *مبلغ مورد نیاز:* ").append(amount).append(" تومان\n");
        text.append("📞 *تماس:* ").append(escape(c.getContactInfo() != null ? c.getContactInfo() : "")).append("\n\n");
        text.append("🌐 مشاهده و کمک در سایت: ").append(siteCaseUrl(c.getId()));

        return text.toString();
    }

    private Integer sendMessage(String text) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", channel);
        body.put("text", text);
        body.put("parse_mode", "Markdown");
        body.put("disable_web_page_preview", false);

        Map response = restTemplate.postForObject(apiUrl() + "/sendMessage", body, Map.class);
        if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
            Map result = (Map) response.get("result");
            return (Integer) result.get("message_id");
        }
        log.error("Bale sendMessage failed: {}", response);
        return null;
    }

    private void sendDocument(String chatId, String filename, String caption) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("document", fileUrl(filename));
        body.put("caption", caption);
        restTemplate.postForObject(apiUrl() + "/sendDocument", body, Map.class);
    }

    private void sendPhoto(String chatId, String filename, String caption) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("photo", fileUrl(filename));
        body.put("caption", caption);
        restTemplate.postForObject(apiUrl() + "/sendPhoto", body, Map.class);
    }

    private String fileUrl(String filename) {
        return baseUrl + "/api/public/files/" + filename;
    }

    private String siteCaseUrl(Long caseId) {
        return baseUrl + "/case/" + caseId;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("_", "\\_").replace("*", "\\*").replace("[", "\\[")
                .replace("]", "\\]").replace("`", "\\`");
    }
}
