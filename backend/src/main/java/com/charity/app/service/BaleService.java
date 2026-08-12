package com.charity.app.service;

import com.charity.app.common.AppUrls;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class BaleService extends AbstractBotMessagingService {

    @Value("${bale.bot-token:}")
    private String botToken;

    @Value("${bale.channel:}")
    private String channel;

    @Value("${bale.enabled:false}")
    private boolean enabled;

    public BaleService(@Qualifier("messagingRestTemplate") RestTemplate restTemplate, AppUrls urls) {
        super(restTemplate, urls);
    }

    @Override
    public String getName() {
        return "bale";
    }

    @Override
    protected String botToken() {
        return botToken;
    }

    @Override
    protected String channel() {
        return channel;
    }

    @Override
    protected boolean configuredEnabled() {
        return enabled;
    }

    @Override
    protected String apiHost() {
        return "https://tapi.bale.ai/bot";
    }
}
