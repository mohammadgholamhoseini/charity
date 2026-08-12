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

  cleanup() {
    if [[ -n "${WORK_DIR:-}" && -d "$WORK_DIR" ]]; then
      git -C "$repo" worktree remove --force "$WORK_DIR" 2>/dev/null || true
    fi
    WORK_DIR=""
  }
  trap cleanup EXIT

  echo "===> backend:$tag"
  "$DOCKER" build "${extra[@]}" \
    --tag "charity-backend:$tag" \
    --label "charity.branch=$branch" \
    --label "charity.source=$source_mode" \
    -f "$(wpath "$source_dir/backend/Dockerfile.$tag")" \
    "$(wpath "$source_dir/backend")"

  echo "===> frontend:$tag"
  "$DOCKER" build "${extra[@]}" \
    --tag "charity-frontend:$tag" \
    --label "charity.branch=$branch" \
    --label "charity.source=$source_mode" \
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
for b in "${targets[@]}"; do
  build_branch "$b" "${NCACHE:-}"
done
echo "ALL DONE"
