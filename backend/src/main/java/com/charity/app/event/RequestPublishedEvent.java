package com.charity.app.event;

/** Raised after a request first becomes PUBLISHED, once the transaction has committed. */
public record RequestPublishedEvent(Long requestId) {
}
