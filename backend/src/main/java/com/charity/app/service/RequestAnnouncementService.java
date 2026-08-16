package com.charity.app.service;

import com.charity.app.common.error.ConflictException;
import com.charity.app.model.Request;
import com.charity.app.payload.RequestDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Announcing a request on the messaging channels, from either direction: automatically when it is
 * first published, or by hand afterwards.
 *
 * <p>The manual path exists because the automatic one has exactly one chance. The event is fired
 * on creation-with-publish and on the <em>first</em> transition to PUBLISHED, so a transient
 * failure -- a Bale read timeout, a container still running yesterday's image -- left the row at
 * {@code bale_posted = 0} with nothing in the product able to fix it. RQ-1017 is the worked
 * example; it published successfully and was never announced.
 *
 * <p>Deliberately carries no {@code @Transactional} anywhere. The HTTP calls to the bot APIs must
 * not run inside a transaction, so every database touch goes through {@link RequestService}, whose
 * methods open their own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestAnnouncementService {

    private final List<MessagingService> channels;
    private final RequestService requests;

    /**
     * Posts to every enabled channel that does not already carry this request.
     *
     * <p>The {@code alreadyPosted} check is what makes a retry safe: a request whose Telegram
     * announcement went out and whose Bale announcement did not gets only the Bale one on a second
     * attempt, rather than a duplicate in the channel that already worked.
     *
     * @return how many channels accepted it
     */
    public int announce(Long requestId) {
        Request request = requests.loadForMessaging(requestId);
        int sent = 0;
        for (MessagingService channel : channels) {
            if (!channel.isEnabled() || channel.alreadyPosted(request)) {
                continue;
            }
            try {
                Integer messageId = channel.publishRequest(request);
                if (messageId != null) {
                    requests.recordMessagingResult(request.getId(), channel.getName(), messageId);
                    sent++;
                }
            } catch (Exception e) {
                log.error("Announcement on {} failed for request {}", channel.getName(), request.getId(), e);
            }
        }
        return sent;
    }

    /**
     * The panel's «انتشار در کانال» button, for an admin or for the centre that owns the request.
     *
     * <p>Runs on the caller's thread rather than through the {@code @Async} listener, which is the
     * opposite of what the automatic path does and is meant to be. That path is async because it
     * used to sit inside the create transaction and held it open for as long as the slowest bot
     * API; this is a rare manual click with nothing behind it. Running it inline buys two things
     * worth more than the wait: the response carries the true post-announcement state, so the
     * button disappears immediately instead of after a guess; and a channel that refuses produces
     * a real error message for the user instead of silence. The wait is bounded by the existing
     * five-second read timeout on {@code messagingRestTemplate}.
     */
    public RequestDetailResponse reannounce(Long id, boolean asCenter) {
        Request request = requests.loadForAnnounce(id, asCenter);

        boolean anythingToSend = channels.stream()
                .anyMatch(channel -> channel.isEnabled() && !channel.alreadyPosted(request));
        if (!anythingToSend) {
            throw new ConflictException("ALREADY_ANNOUNCED",
                    "این درخواست قبلاً در همه کانال‌های فعال اعلام شده است");
        }

        if (announce(id) == 0) {
            throw new ConflictException("ANNOUNCE_FAILED",
                    "ارسال به کانال ناموفق بود. چند لحظه بعد دوباره تلاش کنید.");
        }
        return asCenter ? requests.centerDetail(id) : requests.adminDetail(id);
    }
}
