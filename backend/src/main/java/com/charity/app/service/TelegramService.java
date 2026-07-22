package com.charity.app.service;

import com.charity.app.model.CharityCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${telegram.bot-token}")
    private String botToken;

    @Value("${telegram.channel}")
    private String channel;

    @Value("${telegram.enabled:false}")
    private boolean enabled;

    @Value("${app.base-url:http://localhost:${server.port:8085}}")
    private String baseUrl;

    @Value("${server.port:8085}")
    private String serverPort;

    private String apiUrl() {
        return "https://api.telegram.org/bot" + botToken;
    }

    public Integer publishCase(CharityCase c) {
        if (!enabled || botToken.startsWith("REPLACE")) {
            log.warn("Telegram disabled or token not set. Skipping publish for case {}", c.getId());
            return null;
        }
        try {
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

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", channel);
            body.put("text", text.toString());
            body.put("parse_mode", "Markdown");
            body.put("disable_web_page_preview", false);

            Map response = restTemplate.postForObject(apiUrl() + "/sendMessage", body, Map.class);
            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                Map result = (Map) response.get("result");
                return (Integer) result.get("message_id");
            }
            log.error("Telegram send failed: {}", response);
            return null;
        } catch (Exception e) {
            log.error("Error publishing case {} to telegram", c.getId(), e);
            return null;
        }
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
