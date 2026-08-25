#!/usr/bin/env bash
#
# docker-build.sh — ساخت تصاویر خیریه از برنچ‌های محلی
#
# تصویر dev در صورت فعال بودن برنچ development از working tree فعلی ساخته می‌شود
# تا تغییرات commit‌نشده هم داخل image قرار بگیرند. تصویر master همیشه از آخرین
# commit برنچ master و داخل worktree جداگانه ساخته می‌شود.
#
# هر تصویر از برنچ خودش ساخته می‌شود (بدون نیاز به push / اینترنت / توکن):
#   - development -> charity-frontend:dev   و  charity-backend:dev   (پورت 8080 / 8081)
#   - master       -> charity-frontend:master و charity-backend:master (پورت 80 / 81)
#
# استفاده:
#   ./docker-build.sh dev            # فقط development
#   ./docker-build.sh master        # فقط master
#   ./docker-build.sh all           # هر دو
#   ./docker-build.sh dev --no-cache
#   ./docker-build.sh dev --committed # فقط آخرین commit برنچ development
#
set -euo pipefail

repo="$(cd "$(dirname "$0")" && pwd)"
WORK_DIR=""

# --- تشخیص داکر: `docker` محلی یا داکرِ ویندوزی (docker.exe) ---
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

# مسیر لینوکسی را برای docker.exe به ویندوزی تبدیل می‌کند
wpath() {
  if [[ "$DOCKER" == "docker.exe" ]]; then
    wslpath -w "$1"
  else
    echo "$1"
  fi
}

# --- تصاویر پایه ---
# اتصال به Docker Hub از اینجا «متناوب» است، نه قطع: بعضی تلاش‌ها TLS handshake
# timeout می‌دهند و تلاش بعدی موفق می‌شود (در تست، تلاش ششم جواب داد). بدون این حلقه
# یک timeout گذرا کل build را از پایه می‌شکند. تصاویر یک‌بار کشیده و کش می‌شوند، و از
# آن پس build کاملاً آفلاین است.
BASE_IMAGES=(
  "maven:3.9-eclipse-temurin-21-alpine"
  "eclipse-temurin:21-jre-alpine"
  "node:22-alpine"
)
PULL_RETRIES="${PULL_RETRIES:-25}"

ensure_base_images() {
  local img i ok
  for img in "${BASE_IMAGES[@]}"; do
    if "$DOCKER" image inspect "$img" >/dev/null 2>&1; then
      echo "===> [base] $img — موجود است"
      continue
    fi
    echo "===> [base] $img — در حال دریافت (حداکثر $PULL_RETRIES تلاش)"
    ok=0
    for ((i = 1; i <= PULL_RETRIES; i++)); do
      if "$DOCKER" pull -q "$img" >/dev/null 2>&1; then
        echo "     دریافت شد (تلاش $i)"
        ok=1
        break
      fi
      sleep 3
    done
    if [[ "$ok" != "1" ]]; then
      echo "مشکل: دریافت تصویر پایه \"$img\" پس از $PULL_RETRIES تلاش ناموفق بود." >&2
      echo "      اتصال به registry-1.docker.io برقرار نشد. اتصال را بررسی و دوباره اجرا کنید." >&2
      exit 1
    fi
  done
}

# نامِ تگِ تصویر -> برنچِ گیت
branch_of() {
  case "$1" in
    dev) echo "development" ;;
    master) echo "master" ;;
    *) echo "$1" ;;
  esac
}

build_branch() {
  local tag="$1"
  local branch
  branch="$(branch_of "$tag")"
  local extra=()
  [[ -n "${2:-}" && "$2" == "--no-cache" ]] && extra=(--no-cache)
  local source_dir=""
  local source_mode="committed-worktree"
  local current_branch
  current_branch="$(git -C "$repo" branch --show-current)"

  if [[ "$tag" == "dev" && "$current_branch" == "development" && "${COMMITTED_ONLY:-0}" != "1" ]]; then
    source_dir="$repo"
    source_mode="working-tree"
    echo "===> [source] development working tree"
    if [[ -n "$(git -C "$repo" status --short)" ]]; then
      echo "     شامل تغییرات commit‌نشده"
    fi
  else
    WORK_DIR="$repo/.wtree-$tag"
    if [[ -d "$WORK_DIR" ]]; then
      git -C "$repo" worktree remove --force "$WORK_DIR" 2>/dev/null || true
    fi
    echo "===> [source] committed branch: $branch"
    git -C "$repo" worktree add --detach "$WORK_DIR" "$branch"
    source_dir="$WORK_DIR"
  fi

  # کامیتی که image از آن ساخته می‌شود، به‌صورت label داخل خودِ image می‌ماند. بدون این،
  # هیچ راهی نیست بفهمیم کانتینرِ در حال اجرا چند روز از برنچش عقب است — و دقیقاً همین
  # باعث شد master محلی پنج روز کدِ قدیمی سرو کند بی‌آنکه چیزی خراب به نظر برسد.
  # `docker-status.sh` همین label را می‌خواند.
  local source_commit
  if [[ "$source_mode" == "working-tree" ]]; then
    source_commit="$(git -C "$repo" rev-parse --short HEAD)"
    [[ -n "$(git -C "$repo" status --short)" ]] && source_commit="${source_commit}-dirty"
  else
    source_commit="$(git -C "$repo" rev-parse --short "$branch")"
  fi
  echo "     commit: $source_commit"

  cleanup() {
    if [[ -n "${WORK_DIR:-}" && -d "$WORK_DIR" ]]; then
      git -C "$repo" worktree remove --force "$WORK_DIR" 2>/dev/null || true
    fi
    WORK_DIR=""
  }
  trap cleanup EXIT

  # --pull=false: تصاویر پایه از قبل محلی هستند، پس BuildKit نباید دوباره سراغ رجیستری
  # برود. با اتصال متناوب، همان یک درخواستِ اضافه کافی است که build را بشکند.
  echo "===> backend:$tag"
  "$DOCKER" build "${extra[@]}" --pull=false \
    --tag "charity-backend:$tag" \
    --label "charity.branch=$branch" \
    --label "charity.source=$source_mode" \
    --label "charity.commit=$source_commit" \
    -f "$(wpath "$source_dir/backend/Dockerfile.$tag")" \
    "$(wpath "$source_dir/backend")"

  echo "===> frontend:$tag"
  "$DOCKER" build "${extra[@]}" --pull=false \
    --tag "charity-frontend:$tag" \
    --label "charity.branch=$branch" \
    --label "charity.source=$source_mode" \
    --label "charity.commit=$source_commit" \
    -f "$(wpath "$source_dir/frontend/Dockerfile.$tag")" \
    "$(wpath "$source_dir/frontend")"

  cleanup
  trap - EXIT
  local image_id
  image_id="$("$DOCKER" image inspect "charity-frontend:$tag" --format '{{.Id}}' 2>/dev/null || true)"
  echo "===> done: $tag ($branch, $source_mode)"
  [[ -n "$image_id" ]] && echo "     frontend image: ${image_id:0:28}"
  if [[ "$tag" == "dev" ]]; then
    echo "     open: http://localhost:8080"
  else
    echo "     open: http://localhost"
  fi
}

targets=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-cache) NCACHE="--no-cache" ;;
    --committed) COMMITTED_ONLY="1" ;;
    all) targets=(dev master) ;;
    dev|master) targets+=("$1") ;;
    *) echo "مشکل: آرگومان ناشناخته \"$1\"" >&2; exit 1 ;;
  esac
  shift
done

[[ ${#targets[@]} -eq 0 ]] && targets=(dev)
ensure_base_images
for b in "${targets[@]}"; do
  build_branch "$b" "${NCACHE:-}"
done
echo "ALL DONE"
