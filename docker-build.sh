#!/usr/bin/env bash
#
# docker-build.sh — ساخت تصاویر خیریه از محتوایِ کامیت‌شدهٔ برنچِ گیتِ محلی
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
  WORK_DIR="$repo/.wtree-$tag"

  echo "===> [git] worktree $branch"
  git -C "$repo" worktree add --detach "$WORK_DIR" "$branch"

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
    -f "$(wpath "$WORK_DIR/backend/Dockerfile.$tag")" \
    "$(wpath "$WORK_DIR/backend")"

  echo "===> frontend:$tag"
  "$DOCKER" build "${extra[@]}" \
    --tag "charity-frontend:$tag" \
    -f "$(wpath "$WORK_DIR/frontend/Dockerfile.$tag")" \
    "$(wpath "$WORK_DIR/frontend")"

  cleanup
  trap - EXIT
  echo "===> done: $tag ($branch)"
}

targets=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-cache) NCACHE="--no-cache" ;;
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