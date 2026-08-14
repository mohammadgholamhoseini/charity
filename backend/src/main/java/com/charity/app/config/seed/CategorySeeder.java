package com.charity.app.config.seed;

import com.charity.app.common.SlugUtil;
import com.charity.app.model.Category;
import com.charity.app.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Reconciles the category taxonomy with the one in the redesign.
 *
 * <p>The seeded taxonomy and the designed one overlap but do not match: «آموزش» is «تحصیل» in the
 * design, «ساخت‌وساز» is «مسکن», and «اشتغال» and «جهیزیه» did not exist at all. Renaming in place
 * is preferable to inserting near-duplicates, because every existing request keeps its category and
 * no foreign key moves.
 *
 * <p>Everything here is guarded on the slug being empty, which is only true on the first run after
 * the migration added the column. After that an admin's own edits to names, colours and ordering
 * are never overwritten.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CategorySeeder {

    /**
     * @param aliases previous names this category may currently be stored under
     */
    private record Seed(String name, String slug, String labelBg, String labelText, int sortOrder,
                        String description, List<String> aliases) {
    }

    /**
     * The colours are the design's eight label swatches. They are duplicated in the admin
     * panel's swatch picker ({@code dashboard/admin/categories.vue}) and their neutral
     * fallback in {@link com.charity.app.mapper.CategoryMapper}; change one and change all
     * three. Because each chosen pair is stored on the row, changing them here only affects
     * a fresh install -- existing rows need a migration, as V10 did for this palette.
     */
    private static final List<Seed> DESIGN_CATEGORIES = List.of(
            new Seed("درمان", "darman", "#D6F2F6", "#0B7F91", 1,
                    "هزینه درمان، جراحی، دارو و خدمات پزشکی", List.of("درمانی")),
            new Seed("بلایای طبیعی", "balaya-tabiei", "#FBE4E8", "#9B1C31", 2,
                    "کمک‌رسانی در سیل، زلزله و حوادث طبیعی", List.of()),
            new Seed("تحصیل", "tahsil", "#DEE9FC", "#1F5FC0", 3,
                    "حمایت تحصیلی از دانش‌آموزان و دانشجویان", List.of("آموزش")),
            new Seed("معیشت", "maishat", "#E7E6F7", "#4B3F9E", 4,
                    "سبد غذایی، اقلام ضروری و هزینه‌های روزمره", List.of("غذا و تغذیه")),
            new Seed("مسکن", "maskan", "#DCEFE8", "#1E6E57", 5,
                    "ودیعه اجاره، تعمیر و تأمین سرپناه", List.of("ساخت‌وساز")),
            new Seed("اشتغال", "eshteghal", "#E4EEDC", "#4A6B2E", 6,
                    "ابزار کار، سرمایه خرد و راه‌اندازی کسب‌وکار", List.of()),
            new Seed("جهیزیه", "jahizieh", "#F2E4F2", "#7A3E75", 7,
                    "تأمین جهیزیه و لوازم ضروری زندگی", List.of()),
            new Seed("ایتام", "aytam", "#E3EAF7", "#254F8E", 8,
                    "حمایت از کودکان بی‌سرپرست و بدسرپرست", List.of("ایتام و کودکان")));

    private static final String FALLBACK_BG = "#E9F0FC";
    private static final String FALLBACK_TEXT = "#576E96";

    private final CategoryRepository categories;

    @Transactional
    public void seed() {
        for (Seed seed : DESIGN_CATEGORIES) {
            Optional<Category> existing = findExisting(seed);
            if (existing.isPresent()) {
                alignExisting(existing.get(), seed);
            } else {
                createFrom(seed);
            }
        }
        backfillLegacyCategories();
    }

    private Optional<Category> findExisting(Seed seed) {
        Optional<Category> bySlug = categories.findBySlug(seed.slug());
        if (bySlug.isPresent()) {
            return bySlug;
        }
        Optional<Category> byName = categories.findByName(seed.name());
        if (byName.isPresent()) {
            return byName;
        }
        for (String alias : seed.aliases()) {
            Optional<Category> byAlias = categories.findByName(alias);
            if (byAlias.isPresent()) {
                return byAlias;
            }
        }
        return Optional.empty();
    }

    /** Only touches a category that has not yet been given a slug, i.e. never touched by an admin. */
    private void alignExisting(Category category, Seed seed) {
        if (category.getSlug() != null && !category.getSlug().isBlank()) {
            return;
        }
        log.info("Aligning category '{}' with design taxonomy '{}'", category.getName(), seed.name());
        category.setName(seed.name());
        category.setSlug(seed.slug());
        category.setLabelBg(seed.labelBg());
        category.setLabelText(seed.labelText());
        category.setSortOrder(seed.sortOrder());
        if (category.getDescription() == null || category.getDescription().isBlank()) {
            category.setDescription(seed.description());
        }
        categories.save(category);
    }

    private void createFrom(Seed seed) {
        log.info("Creating category '{}'", seed.name());
        categories.save(Category.builder()
                .name(seed.name())
                .slug(seed.slug())
                .description(seed.description())
                .labelBg(seed.labelBg())
                .labelText(seed.labelText())
                .sortOrder(seed.sortOrder())
                .active(true)
                .build());
    }

    /**
     * Categories that predate the redesign and have no counterpart in it -- «اضطراری» and «سایر» --
     * are kept rather than deleted, because requests may reference them. They get a derived slug and
     * neutral colours, and an admin can deactivate them from the panel.
     */
    private void backfillLegacyCategories() {
        categories.findAll().stream()
                .filter(category -> category.getSlug() == null || category.getSlug().isBlank())
                .forEach(category -> {
                    String slug = SlugUtil.slugify(category.getName(), 120);
                    if (slug.isBlank() || categories.existsBySlug(slug)) {
                        slug = "category-" + category.getId();
                    }
                    category.setSlug(slug);
                    category.setLabelBg(FALLBACK_BG);
                    category.setLabelText(FALLBACK_TEXT);
                    category.setSortOrder(90);
                    categories.save(category);
                    log.info("Backfilled slug '{}' for legacy category '{}'", slug, category.getName());
                });
    }
}
