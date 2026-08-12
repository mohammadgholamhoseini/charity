package com.charity.app.service;

import com.charity.app.model.Request;

/** A channel a published request can be announced on. */
public interface MessagingService {

    /** Stable key used to record which channel a message id belongs to. */
    String getName();

    boolean isEnabled();

    /** @return the remote message id, or null if the announcement did not go out. */
    Integer publishRequest(Request request);
}
