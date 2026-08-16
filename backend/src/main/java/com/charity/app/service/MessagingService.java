package com.charity.app.service;

import com.charity.app.model.Request;

/** A channel a published request can be announced on. */
public interface MessagingService {

    /** Stable key used to record which channel a message id belongs to. */
    String getName();

    boolean isEnabled();

    /**
     * Whether this channel already carries this request.
     *
     * <p>Asked before every send, so the manual re-announce button can retry the channel that
     * failed without posting a second copy to the one that succeeded. Each implementation reads
     * its own column rather than the caller switching on {@link #getName()}.
     */
    boolean alreadyPosted(Request request);

    /** @return the remote message id, or null if the announcement did not go out. */
    Integer publishRequest(Request request);
}
