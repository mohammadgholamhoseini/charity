package com.charity.app.service;

import com.charity.app.model.Notice;
import com.charity.app.payload.NoticeRequest;
import com.charity.app.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    @Transactional(readOnly = true)
    public List<Notice> listAll() {
        return noticeRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Notice> listActiveByPosition(Notice.Position position) {
        return noticeRepository.findByActiveAndPositionOrderByCreatedAtDesc(true, position);
    }

    @Transactional
    public Notice create(NoticeRequest req) {
        Notice n = Notice.builder()
                .title(req.getTitle())
                .content(req.getContent())
                .position(req.getPosition() != null ? req.getPosition() : Notice.Position.FOOTER)
                .active(req.isActive())
                .build();
        return noticeRepository.save(n);
    }

    @Transactional
    public Notice update(Long id, NoticeRequest req) {
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("اطلاعیه یافت نشد"));
        n.setTitle(req.getTitle());
        n.setContent(req.getContent());
        if (req.getPosition() != null) {
            n.setPosition(req.getPosition());
        }
        n.setActive(req.isActive());
        return noticeRepository.save(n);
    }

    @Transactional
    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }
}
