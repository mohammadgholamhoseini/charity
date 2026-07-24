package com.charity.app.service;

import com.charity.app.model.CharityCase;

public interface MessagingService {
    String getName();
    Integer publishCase(CharityCase c);
    boolean isEnabled();
}
