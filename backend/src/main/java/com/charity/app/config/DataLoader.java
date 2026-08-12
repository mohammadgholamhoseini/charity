package com.charity.app.config;

import com.charity.app.config.seed.CategorySeeder;
import com.charity.app.config.seed.LocationSeeder;
import com.charity.app.config.seed.SlugBackfill;
import com.charity.app.model.Notice;
import com.charity.app.model.User;
import com.charity.app.model.enums.NoticePlacement;
import com.charity.app.model.enums.UserRole;
import com.charity.app.repository.NoticeRepository;
import com.charity.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Brings a database up to a usable state on startup: reference data, the initial admin, and any
 * slugs the migrations left for the application to compute.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final CategorySeeder categorySeeder;
    private final LocationSeeder locationSeeder;
    private final SlugBackfill slugBackfill;
    private final UserRepository users;
    private final NoticeRepository notices;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.initial-password:}")
    private String initialAdminPassword;

    @Override
    public void run(ApplicationArguments args) {
        categorySeeder.seed();
        locationSeeder.seed();
        seedFooterNotice();
        seedAdmin();
        slugBackfill.backfill();
    }

    private void seedFooterNotice() {
        if (notices.count() > 0) {
            return;
        }
        notices.save(Notice.builder()
                .title("مسئولیت پرداخت")
                .content("یاری‌جو صرفاً جهت اطلاع‌رسانی است. مسئولیت هرگونه پرداخت، واریز وجه و انتقال "
                        + "وجوه بر عهده کاربر و مرکز خیریه مربوطه است و این وب‌سایت هیچ‌گونه مسئولیتی "
                        + "در قبال تراکنش‌های مالی ندارد.")
                .placement(NoticePlacement.FOOTER)
                .active(true)
                .build());
        log.info("Seeded default footer disclaimer");
    }

    /**
     * Creates the first admin only when a password is supplied.
     *
     * <p>The previous version hardcoded {@code admin123} and printed it to the startup log in
     * plaintext. Now the password comes from {@code ADMIN_INITIAL_PASSWORD}, nothing is logged, and
     * a deployment that forgets to set it gets a clear warning instead of a publicly-known account.
     */
    private void seedAdmin() {
        if (users.findByUsername("admin").isPresent()) {
            return;
        }
        if (initialAdminPassword == null || initialAdminPassword.isBlank()) {
            log.warn("No admin account exists and ADMIN_INITIAL_PASSWORD is not set -- "
                    + "set it and restart to create the initial admin.");
            return;
        }
        users.save(User.builder()
                .username("admin")
                .password(passwordEncoder.encode(initialAdminPassword))
                .email("admin@yariju.local")
                .role(UserRole.ADMIN)
                .enabled(true)
                .build());
        log.info("Created initial admin account 'admin' from ADMIN_INITIAL_PASSWORD");
    }
}
