#!/usr/bin/env bash
#
# docker-status.sh — آیا چیزی که روی localhost می‌بینم، همان کدِ برنچ است؟
#
# این اسکریپت وجود دارد چون یک بار master محلی پنج روز کدِ قدیمی سرو کرد و هیچ‌چیز
# خراب به نظر نمی‌رسید: کانتینرها `Up` و `healthy` بودند، `docker compose up` هم بی‌صدا
# همان image قبلی را دوباره استارت می‌کرد. تنها نشانه این بود که یک قابلیتِ merge‌شده
# روی سایت زنده دیده می‌شد و روی پورت ۸۰ نه.
#
# ریشه‌اش در compose.yaml است و عمدی است: سرویس‌ها فقط `image:` دارند و `build:` ندارند،
# پس `docker compose up` هرگز کد جدید را برنمی‌دارد. **این را با افزودن `build:` «درست»
# نکنید** — به‌خصوص برای master. کانتکست build همین working tree است، که تقریباً همیشه
# روی development ایستاده؛ یعنی `build:` یک image با تگ master می‌سازد که داخلش کدِ
# development است. آن حالت از وضعیت فعلی بدتر است، چون دیگر کهنه نیست بلکه دروغ است.
# راه درست همان docker-build.sh است که master را از worktree جدا و از آخرین کامیتِ
# برنچ می‌سازد.
#
# استفاده:
#   ./docker-status.sh
#
set -euo pipefail

repo="$(cd "$(dirname "$0")" && pwd)"

detect_docker() {
  if command -v docker >/dev/null 2>&1 && docker version >/dev/null 2>&1; then
    echo "docker"
  elif command -v docker.exe >/dev/null 2>&1 && docker.exe version >/dev/null 2>&1; then
    echo "docker.exe"
  else
    echo "NONE"
  fi
}
DOCKER="$(detect_docker)"
if [[ "$DOCKER" == "NONE" ]]; then
  echo "مشکل: دستور docker یا docker.exe در دسترس نیست." >&2
  exit 1
fi

git -C "$repo" fetch --quiet origin 2>/dev/null || true

stale=0

# container-name | image | branch | url
rows=(
  "charity-frontend-dev-1|charity-frontend:dev|development|http://localhost:8080"
  "charity-backend-dev-1|charity-backend:dev|development|http://localhost:8081"
  "charity-frontend-master-1|charity-frontend:master|master|http://localhost"
  "charity-backend-master-1|charity-backend:master|master|http://localhost:81"
)

for row in "${rows[@]}"; do
  IFS='|' read -r cname image branch url <<< "$row"

  if ! "$DOCKER" ps --format '{{.Names}}' | grep -qx "$cname"; then
    printf '%-28s %s\n' "$cname" "— در حال اجرا نیست"
    continue
  fi

  built="$("$DOCKER" image inspect "$image" --format '{{index .Config.Labels "charity.commit"}}' 2>/dev/null || true)"
  tip="$(git -C "$repo" rev-parse --short "$branch" 2>/dev/null || echo '?')"

  if [[ -z "$built" || "$built" == "<no value>" ]]; then
    printf '%-28s %s\n' "$cname" "⚠ بدون برچسب commit — پیش از افزودن label ساخته شده. یک بار دوباره بسازید."
    stale=1
    continue
  fi

  # پسوند -dirty یعنی از working tree ساخته شده. آن را برای مقایسه کنار می‌گذاریم اما از
  # مقایسه صرف‌نظر نمی‌کنیم: یک image که از working treeِ یک کامیتِ قدیمی ساخته شده دقیقاً
  # به همان اندازه کهنه است، و بی‌خیال شدنش همان حفره‌ای است که این اسکریپت قرار بود ببندد.
  local_edits=""
  base="$built"
  if [[ "$built" == *-dirty ]]; then
    base="${built%-dirty}"
    local_edits=" + تغییرات محلی"
  fi

  if [[ "$base" == "$tip" ]]; then
    printf '%-28s %s\n' "$cname" "✓ $base = نوکِ $branch$local_edits — $url"
  else
    behind="$(git -C "$repo" rev-list --count "$base..$branch" 2>/dev/null || echo '?')"
    printf '%-28s %s\n' "$cname" "✗ $base اما نوکِ $branch الان $tip است ($behind کامیت عقب)$local_edits — $url"
    stale=1
  fi
done

if [[ "$stale" == "1" ]]; then
  cat <<'EOF'

یک یا چند کانتینر کدِ قدیمی سرو می‌کنند. برای به‌روز کردن:

  ./docker-build.sh master && docker compose up -d backend-master frontend-master
  ./docker-build.sh dev    && docker compose up -d backend-dev    frontend-dev

`docker compose up` به‌تنهایی کافی نیست — دلیلش بالای همین فایل نوشته شده.
EOF
  exit 1
fi

echo
echo "همه‌چیز با برنچ‌ها هم‌راستا است."
