package com.charity.app.mapper;

import com.charity.app.common.AppUrls;
import com.charity.app.model.Notice;
import com.charity.app.payload.NoticeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class NoticeMapper {

    private final AppUrls urls;

    public NoticeResponse toResponse(Notice notice) {
        LocalDateTime now = LocalDateTime.now(urls.zone());
        return new NoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getPlacement(),
                notice.getPlacement() == null ? null : notice.getPlacement().label(),
                urls.iso(notice.getStartAt()),
                urls.iso(notice.getEndAt()),
                notice.getLinkUrl(),
                notice.isActive(),
                notice.isExpiredAt(now),
                urls.iso(notice.getCreatedAt()),
                urls.iso(notice.getUpdatedAt()));
    }
}
