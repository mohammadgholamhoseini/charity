package com.charity.app.service;

import com.charity.app.common.AppUrls;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class TelegramService extends AbstractBotMessagingService {

    @Value("${telegram.bot-token:}")
    private String botToken;

    @Value("${telegram.channel:}")
    private String channel;

    @Value("${telegram.enabled:false}")
    private boolean enabled;

    public TelegramService(@Qualifier("messagingRestTemplate") RestTemplate restTemplate, AppUrls urls) {
        super(restTemplate, urls);
    }

    @Override
    public String getName() {
        return "telegram";
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
        return "https://api.telegram.org/bot";
    }
}
