package com.charity.app.config.seed;

import com.charity.app.model.City;
import com.charity.app.model.Province;
import com.charity.app.repository.CityRepository;
import com.charity.app.repository.ProvinceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Seeds Iran's 31 provinces and their main cities.
 *
 * <p>These were previously not seeded at all, which meant an admin had to type every province and
 * city by hand before a single centre could be created -- and the public city filter had nothing to
 * filter on. Only runs when the tables are empty, so hand-entered locations are never disturbed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocationSeeder {

    private static final Map<String, List<String>> PROVINCES = new java.util.LinkedHashMap<>();

    static {
        PROVINCES.put("تهران", List.of("تهران", "اسلامشهر", "شهریار", "ری", "ورامین", "پاکدشت", "دماوند"));
        PROVINCES.put("خراسان رضوی", List.of("مشهد", "نیشابور", "سبزوار", "تربت حیدریه", "کاشمر", "قوچان"));
        PROVINCES.put("اصفهان", List.of("اصفهان", "کاشان", "خمینی‌شهر", "نجف‌آباد", "شاهین‌شهر", "شهرضا"));
        PROVINCES.put("فارس", List.of("شیراز", "مرودشت", "جهرم", "فسا", "کازرون", "لار"));
        PROVINCES.put("آذربایجان شرقی", List.of("تبریز", "مراغه", "مرند", "اهر", "میانه", "بناب"));
        PROVINCES.put("آذربایجان غربی", List.of("ارومیه", "خوی", "میاندوآب", "مهاباد", "بوکان", "سلماس"));
        PROVINCES.put("خوزستان", List.of("اهواز", "دزفول", "آبادان", "خرمشهر", "اندیمشک", "بهبهان", "ماهشهر"));
        PROVINCES.put("البرز", List.of("کرج", "فردیس", "نظرآباد", "هشتگرد", "اشتهارد"));
        PROVINCES.put("کرمان", List.of("کرمان", "رفسنجان", "سیرجان", "جیرفت", "بم", "زرند"));
        PROVINCES.put("سیستان و بلوچستان", List.of("زاهدان", "زابل", "چابهار", "ایرانشهر", "سراوان", "خاش"));
        PROVINCES.put("مازندران", List.of("ساری", "بابل", "آمل", "قائم‌شهر", "بهشهر", "چالوس", "نوشهر"));
        PROVINCES.put("گیلان", List.of("رشت", "بندر انزلی", "لاهیجان", "لنگرود", "آستارا", "رودسر"));
        PROVINCES.put("کرمانشاه", List.of("کرمانشاه", "اسلام‌آباد غرب", "هرسین", "کنگاور", "سنقر", "جوانرود"));
        PROVINCES.put("گلستان", List.of("گرگان", "گنبد کاووس", "علی‌آباد کتول", "آق‌قلا", "بندر ترکمن"));
        PROVINCES.put("هرمزگان", List.of("بندرعباس", "میناب", "قشم", "بندر لنگه", "رودان"));
        PROVINCES.put("لرستان", List.of("خرم‌آباد", "بروجرد", "دورود", "الیگودرز", "کوهدشت", "نورآباد"));
        PROVINCES.put("همدان", List.of("همدان", "ملایر", "نهاوند", "تویسرکان", "اسدآباد", "کبودرآهنگ"));
        PROVINCES.put("کردستان", List.of("سنندج", "سقز", "مریوان", "بانه", "قروه", "بیجار"));
        PROVINCES.put("مرکزی", List.of("اراک", "ساوه", "خمین", "محلات", "دلیجان", "شازند"));
        PROVINCES.put("قم", List.of("قم"));
        PROVINCES.put("قزوین", List.of("قزوین", "تاکستان", "الوند", "آبیک", "بوئین‌زهرا"));
        PROVINCES.put("اردبیل", List.of("اردبیل", "پارس‌آباد", "مشگین‌شهر", "خلخال", "گرمی"));
        PROVINCES.put("بوشهر", List.of("بوشهر", "برازجان", "گناوه", "دیلم", "کنگان", "جم"));
        PROVINCES.put("زنجان", List.of("زنجان", "ابهر", "خرمدره", "قیدار", "خدابنده"));
        PROVINCES.put("یزد", List.of("یزد", "میبد", "اردکان", "بافق", "مهریز", "تفت"));
        PROVINCES.put("چهارمحال و بختیاری", List.of("شهرکرد", "بروجن", "فارسان", "لردگان", "سامان"));
        PROVINCES.put("خراسان شمالی", List.of("بجنورد", "شیروان", "اسفراین", "آشخانه", "جاجرم"));
        PROVINCES.put("خراسان جنوبی", List.of("بیرجند", "قائن", "فردوس", "طبس", "نهبندان"));
        PROVINCES.put("سمنان", List.of("سمنان", "شاهرود", "دامغان", "گرمسار", "مهدی‌شهر"));
        PROVINCES.put("ایلام", List.of("ایلام", "دهلران", "آبدانان", "ایوان", "مهران"));
        PROVINCES.put("کهگیلویه و بویراحمد", List.of("یاسوج", "دوگنبدان", "دهدشت", "سی‌سخت", "لیکک"));
    }

    private final ProvinceRepository provinces;
    private final CityRepository cities;

    @Transactional
    public void seed() {
        if (provinces.count() > 0) {
            return;
        }
        int cityCount = 0;
        for (Map.Entry<String, List<String>> entry : PROVINCES.entrySet()) {
            Province province = provinces.save(Province.builder().name(entry.getKey()).build());
            for (String cityName : entry.getValue()) {
                cities.save(City.builder().name(cityName).province(province).build());
                cityCount++;
            }
        }
        log.info("Seeded {} provinces and {} cities", PROVINCES.size(), cityCount);
    }
}
