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
    private final ProvinceRepository provinceRepository;
    private final CityRepository cityRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedCategories();
        seedNotices();
        seedProvinces();
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

    private void seedProvinces() {
        if (provinceRepository.count() == 0) {
            Object[][] data = {
                    {"تهران", new String[]{"تهران", "شهریار", "اسلامشهر", "قدس", "ری", "پاکدشت", "ورامین"}},
                    {"اصفهان", new String[]{"اصفهان", "کاشان", "خمینی‌شهر", "نجف‌آباد", "شاهین‌شهر"}},
                    {"خراسان رضوی", new String[]{"مشهد", "نیشابور", "سبزوار", "تربت حیدریه", "قوچان"}},
                    {"فارس", new String[]{"شیراز", "مرودشت", "جهرم", "کازرون", "فسا"}},
                    {"خوزستان", new String[]{"اهواز", "آبادان", "دزفول", "خرمشهر", "بندر ماهشهر"}},
                    {"آذربایجان شرقی", new String[]{"تبریز", "مراغه", "میانه", "مرند", "اهر"}},
                    {"البرز", new String[]{"کرج", "فردیس", "نظرآباد", "هشتگرد", "محمدشهر"}},
                    {"همدان", new String[]{"همدان", "ملایر", "نهاوند", "اسدآباد", "بهار"}}
            };
            int total = 0;
            for (Object[] row : data) {
                Province p = Province.builder().name((String) row[0]).build();
                p = provinceRepository.save(p);
                for (String cname : (String[]) row[1]) {
                    cityRepository.save(City.builder().name(cname).province(p).build());
                    total++;
                }
            }
            log.info("Seeded {} provinces and {} cities", data.length, total);
        }
    }

    private void seedAdmin() {
        User admin = userRepository.findByUsername("admin").orElse(null);
        if (admin == null) {
            admin = User.builder()
                    .username("admin")
                    .email("admin@charity.local")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            log.info("Seeded default admin (username=admin, password=admin123). Change in production!");
        }
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setEmail("admin@charity.local");
        admin.setRole(Role.ADMIN);
        admin.setEnabled(true);
        userRepository.save(admin);
    }
}
