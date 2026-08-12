package com.charity.app.service;

import com.charity.app.event.RequestPublishedEvent;
import com.charity.app.model.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Announces newly published requests on the messaging channels.
 *
 * <p>Runs after commit and off the request thread. Announcing used to happen synchronously inside
 * the create transaction using an HTTP client with no timeouts, so a slow messaging API blocked the
 * caller indefinitely while holding a database transaction -- enough concurrent submissions would
 * exhaust both the thread pool and the connection pool.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestPublishedListener {

    private final List<MessagingService> channels;
    private final RequestService requestService;

    @Async
    @TransactionalEventListener
    public void onPublished(RequestPublishedEvent event) {
        Request request = requestService.loadForMessaging(event.requestId());
        for (MessagingService channel : channels) {
            if (!channel.isEnabled()) {
                continue;
            }
            try {
                Integer messageId = channel.publishRequest(request);
                if (messageId != null) {
                    requestService.recordMessagingResult(request.getId(), channel.getName(), messageId);
                }
            } catch (Exception e) {
                log.error("Announcement on {} failed for request {}", channel.getName(), request.getId(), e);
            }
        }
    }
}
