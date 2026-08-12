package com.charity.app.service;

import com.charity.app.common.AppUrls;
import com.charity.app.common.error.NotFoundException;
import com.charity.app.common.error.ValidationException;
import com.charity.app.mapper.NoticeMapper;
import com.charity.app.model.Notice;
import com.charity.app.model.enums.NoticePlacement;
import com.charity.app.payload.NoticeRequest;
import com.charity.app.payload.NoticeResponse;
import com.charity.app.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository notices;
    private final NoticeMapper mapper;
    private final AppUrls urls;

    /**
     * The single announcement currently showing in a placement, if any.
     *
     * <p>Returns at most one: the design shows one banner and one footer line, so choosing which is
     * the server's job rather than something the client should have to work out from a list.
     */
    @Transactional(readOnly = true)
    public Optional<NoticeResponse> currentFor(NoticePlacement placement) {
        return notices.findServable(placement, LocalDateTime.now(urls.zone()), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<NoticeResponse> listAll() {
        return notices.findAllByOrderByCreatedAtDesc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NoticeResponse get(Long id) {
        return mapper.toResponse(load(id));
    }

    @Transactional
    public NoticeResponse create(NoticeRequest req) {
        validateWindow(req);
        Notice notice = Notice.builder()
                .title(req.title())
                .content(req.content())
                .placement(req.placement())
                .startAt(req.startAt())
                .endAt(req.endAt())
                .linkUrl(blankToNull(req.linkUrl()))
                .active(req.active())
                .build();
        return mapper.toResponse(notices.save(notice));
    }

    @Transactional
    public NoticeResponse update(Long id, NoticeRequest req) {
        validateWindow(req);
        Notice notice = load(id);
        notice.setTitle(req.title());
        notice.setContent(req.content());
        notice.setPlacement(req.placement());
        notice.setStartAt(req.startAt());
        notice.setEndAt(req.endAt());
        notice.setLinkUrl(blankToNull(req.linkUrl()));
        notice.setActive(req.active());
        return mapper.toResponse(notices.save(notice));
    }

    @Transactional
    public void delete(Long id) {
        notices.delete(load(id));
    }

    private void validateWindow(NoticeRequest req) {
        if (req.startAt() != null && req.endAt() != null && req.endAt().isBefore(req.startAt())) {
            throw new ValidationException("پایان نمایش نمی‌تواند قبل از شروع آن باشد");
        }
    }

    private Notice load(Long id) {
        return notices.findById(id).orElseThrow(() -> new NotFoundException("اطلاعیه یافت نشد"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
