package com.charity.app.service;

import com.charity.app.event.RequestPublishedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Announces newly published requests on the messaging channels.
 *
 * <p>Runs after commit and off the request thread. Announcing used to happen synchronously inside
 * the create transaction using an HTTP client with no timeouts, so a slow messaging API blocked the
 * caller indefinitely while holding a database transaction -- enough concurrent submissions would
 * exhaust both the thread pool and the connection pool.
 *
 * <p>The work itself lives in {@link RequestAnnouncementService}, because the panel's manual
 * re-announce button has to do exactly the same thing and a failure here is otherwise permanent.
 */
@Component
@RequiredArgsConstructor
public class RequestPublishedListener {

    private final RequestAnnouncementService announcements;

    @Async
    @TransactionalEventListener
    public void onPublished(RequestPublishedEvent event) {
        announcements.announce(event.requestId());
    }
}
