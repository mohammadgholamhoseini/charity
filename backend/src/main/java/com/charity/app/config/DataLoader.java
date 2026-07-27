package com.charity.app.config;

import com.charity.app.model.Category;
import com.charity.app.model.City;
import com.charity.app.model.Notice;
import com.charity.app.model.Province;
import com.charity.app.model.User;
import com.charity.app.model.User.Role;
import com.charity.app.repository.CategoryRepository;
import com.charity.app.repository.CityRepository;
import com.charity.app.repository.NoticeRepository;
import com.charity.app.repository.ProvinceRepository;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NoticeRepository noticeRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
        seedNotices();
        seedAdmin();
    }

    private void seedNotices() {
        if (noticeRepository.count() == 0) {
            noticeRepository.save(Notice.builder()
                    .title("مسئولیت پرداخت")
                    .content("این پلتفرم صرفاً جهت اطلاع‌رسانی است. مسئولیت هرگونه پرداخت، واریز وجه و انتقال وجوه بر عهده خود کاربر و مرکز خیریه مربوطه می‌باشد و این وب‌سایت هیچ‌گونه مسئولیتی در قبال تراکنش‌های مالی ندارد.")
                    .position(Notice.Position.FOOTER)
                    .active(true)
                    .build());
            log.info("Seeded default disclaimer notice");
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            List<Category> cats = List.of(
                    Category.builder().name("درمانی").description("کمک به بیماران نیازمند درمان").active(true).build(),
                    Category.builder().name("ساخت‌وساز").description("ساخت و بازسازی مراکز و مسکن").active(true).build(),
                    Category.builder().name("آموزش").description("حمایت از دانش‌آموزان و دانشجویان").active(true).build(),
                    Category.builder().name("بلایای طبیعی").description("کمک‌رسانی در سیل، زلزله و حوادث").active(true).build(),
                    Category.builder().name("ایتام و کودکان").description("حمایت از کودکان بدسرپرست").active(true).build(),
                    Category.builder().name("غذا و تغذیه").description("تأمین غذا و بسته‌های حمایتی").active(true).build(),
                    Category.builder().name("اضطراری").description("نیازهای فوری و موارد اضطراری").active(true).build(),
                    Category.builder().name("سایر").description("سایر موارد خیریه").active(true).build()
            );
            categoryRepository.saveAll(cats);
            log.info("Seeded {} categories", cats.size());
        }
    }

    private void seedAdmin() {
        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        User admin = User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .email("admin@charity.local")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded default admin (username=admin, password=admin123). Change in production!");
    }
}
