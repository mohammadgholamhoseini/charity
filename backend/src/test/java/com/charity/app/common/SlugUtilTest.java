package com.charity.app.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slug construction, exercised on the character variants that actually occur in Persian input.
 *
 * <p>This is the one place in the test suite where Persian literals belong: the class exists to
 * fold Arabic yeh against Persian yeh, three digit sets against one, and a zero-width non-joiner
 * against a separator. Asserting that with ASCII placeholders would test nothing.
 *
 * <p>The invariant that carries the most weight is at the bottom: a request slug always ends in its
 * code. Collision-freedom, the 301 after a title edit, and {@code extractCode} all rest on it.
 */
class SlugUtilTest {

    /** Zero-width non-joiner: invisible, and present in a large share of real Persian titles. */
    private static final String ZWNJ = "‌";

    @Nested
    @DisplayName("normalizePersian folds the variants that would otherwise split one word in two")
    class Normalisation {

        @ParameterizedTest(name = "[{0}] -> [{1}]")
        @CsvSource({
                // Arabic yeh and kaf are what an Arabic keyboard layout produces for Persian words.
                "ياري, یاری",
                "كمك, کمک",
                // Teh marbuta and the alef variants arrive from copy-pasted Arabic text.
                "صدقة, صدقه",
                "أحمد, احمد",
                "إیران, ایران",
        })
        void foldsLetterVariants(String input, String expected) {
            assertThat(SlugUtil.normalizePersian(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("both Arabic-Indic digit sets become ASCII digits")
        void foldsBothDigitSets() {
            // U+0660.. is Arabic-Indic; U+06F0.. is the extended set Persian actually uses. A title
            // typed on one keyboard and searched from the other has to match.
            assertThat(SlugUtil.normalizePersian("٠١٢٣٤٥٦٧٨٩"))
                    .isEqualTo("0123456789");
            assertThat(SlugUtil.normalizePersian("۰۱۲۳۴۵۶۷۸۹"))
                    .isEqualTo("0123456789");
        }

        @Test
        @DisplayName("null normalises to empty rather than throwing")
        void nullIsEmpty() {
            assertThat(SlugUtil.normalizePersian(null)).isEmpty();
        }

        @Test
        @DisplayName("text already canonical is returned unchanged")
        void canonicalTextIsUntouched() {
            String canonical = "یاری" + ZWNJ + "جو";
            assertThat(SlugUtil.normalizePersian(canonical)).isEqualTo(canonical);
        }
    }

    @Nested
    @DisplayName("slugify")
    class Slugify {

        @Test
        @DisplayName("keeps Persian letters instead of transliterating them")
        void staysPersian() {
            // The deliberate choice this class documents: a transliterated slug matches nothing a
            // Persian speaker would type into a search box.
            assertThat(SlugUtil.slugify("کمک هزینه", 180))
                    .isEqualTo("کمک-هزینه");
        }

        @Test
        @DisplayName("a zero-width non-joiner becomes a separator, not a silent join")
        void zwnjBecomesADash() {
            // Without this, «یاری‌جو» and «یاری جو» would be two different URLs for one thing.
            assertThat(SlugUtil.slugify("یاری" + ZWNJ + "جو", 180))
                    .isEqualTo("یاری-جو");
        }

        @Test
        @DisplayName("the Arabic and Persian spellings of one word slugify identically")
        void variantSpellingsConverge() {
            String arabicLayout = "كمك ياري";
            String persianLayout = "کمک یاری";
            assertThat(SlugUtil.slugify(arabicLayout, 180))
                    .isEqualTo(SlugUtil.slugify(persianLayout, 180));
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", "---", "!!! ??? ...", "‌"})
        @DisplayName("input with nothing sluggable yields an empty string, never a bare dash")
        void degeneratesToEmpty(String input) {
            assertThat(SlugUtil.slugify(input, 180)).isEmpty();
        }

        @Test
        @DisplayName("null yields empty rather than throwing")
        void nullIsEmpty() {
            assertThat(SlugUtil.slugify(null, 180)).isEmpty();
        }

        @Test
        @DisplayName("punctuation collapses to a single dash and never leads or trails")
        void punctuationCollapses() {
            assertThat(SlugUtil.slugify("  --help,,,  me!!  ", 180)).isEqualTo("help-me");
        }

        @Test
        @DisplayName("truncation falls back to the last separator so a word is not cut in half")
        void truncatesOnASeparator() {
            // 13 lands mid-"gamma"; the last dash past the halfway mark wins, so "gamma" is dropped
            // whole rather than left as "gam".
            assertThat(SlugUtil.slugify("alpha beta gamma delta", 13)).isEqualTo("alpha-beta");

            // 17 lands on the dash after "gamma", so "gamma" survives intact.
            assertThat(SlugUtil.slugify("alpha beta gamma delta", 17)).isEqualTo("alpha-beta-gamma");
        }

        @Test
        @DisplayName("a truncated slug never ends in a dash")
        void truncationLeavesNoTrailingDash() {
            assertThat(SlugUtil.slugify("alpha beta gamma", 11)).doesNotEndWith("-");
        }

        @Test
        @DisplayName("a single word longer than the limit is cut rather than emptied")
        void oneLongWordIsCut() {
            // No dash past the halfway mark, so there is nothing to fall back to. Cutting is right;
            // returning empty would lose the whole title.
            assertThat(SlugUtil.slugify("abcdefghijklmnop", 8)).isEqualTo("abcdefgh");
        }

        @Test
        @DisplayName("uppercase is folded to lowercase")
        void lowercases() {
            assertThat(SlugUtil.slugify("Help Me Now", 180)).isEqualTo("help-me-now");
        }
    }

    @Nested
    @DisplayName("requestSlug and extractCode are two halves of one contract")
    class CodeSuffix {

        @Test
        @DisplayName("the slug always ends in its lowercased code")
        void alwaysEndsInTheCode() {
            assertThat(SlugUtil.requestSlug("کمک هزینه", "RQ-1042"))
                    .endsWith("-rq-1042");
        }

        @Test
        @DisplayName("a title with nothing sluggable still produces a usable slug")
        void emptyTitleFallsBack() {
            // Otherwise the slug would be a bare "-rq-1042", which is a broken URL rather than a
            // short one.
            assertThat(SlugUtil.requestSlug("!!!", "RQ-1042")).isEqualTo("request-rq-1042");
            assertThat(SlugUtil.requestSlug(null, "RQ-1042")).isEqualTo("request-rq-1042");
        }

        @Test
        @DisplayName("two requests with identical titles get different slugs")
        void collisionsAreImpossibleByConstruction() {
            // This is why there is no retry loop and no "-2" counter anywhere.
            String title = "کمک هزینه درمان";
            assertThat(SlugUtil.requestSlug(title, "RQ-1001"))
                    .isNotEqualTo(SlugUtil.requestSlug(title, "RQ-1002"));
        }

        @Test
        @DisplayName("extractCode recovers the code from a slug the same method built")
        void roundTrips() {
            String slug = SlugUtil.requestSlug("کمک هزینه", "RQ-1042");
            assertThat(SlugUtil.extractCode(slug)).isEqualTo("RQ-1042");
        }

        @Test
        @DisplayName("a title edit changes the slug but not the code it carries")
        void codeSurvivesATitleEdit() {
            // This is what lets an old slug be resolved and 301'd instead of 404'd.
            String before = SlugUtil.requestSlug("old title", "RQ-1042");
            String after = SlugUtil.requestSlug("a completely different title", "RQ-1042");
            assertThat(before).isNotEqualTo(after);
            assertThat(SlugUtil.extractCode(before)).isEqualTo(SlugUtil.extractCode(after));
        }

        @Test
        @DisplayName("extractCode returns null for a slug that carries no code")
        void noCodeMeansNull() {
            assertThat(SlugUtil.extractCode("a-plain-slug")).isNull();
            assertThat(SlugUtil.extractCode(null)).isNull();
        }

        @Test
        @DisplayName("a title containing rq- does not fool extractCode")
        void theLastOccurrenceWins() {
            // lastIndexOf, not indexOf: the code is the suffix, and a title may legitimately
            // contain the same three characters.
            assertThat(SlugUtil.extractCode(SlugUtil.requestSlug("rq-9 spare parts", "RQ-1042")))
                    .isEqualTo("RQ-1042");
        }
    }
}
