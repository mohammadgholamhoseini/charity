#!/usr/bin/env bash
#
# Thin wrapper over the Trello REST API, for the `tasks` agent and for hand use.
#
# Credentials are read from the environment, or from a file OUTSIDE this repository
# (default ~/.trello.env). They are never echoed, never written to a file in the repo,
# and never placed in a URL — the token goes in an Authorization header supplied to
# curl through --config on stdin, so it does not appear in the process list either.
#
# Setup: see .claude/scripts/README-trello.md
#
set -euo pipefail

CRED_FILE="${TRELLO_ENV_FILE:-$HOME/.trello.env}"
if [[ -z "${TRELLO_KEY:-}" || -z "${TRELLO_TOKEN:-}" ]] && [[ -f "$CRED_FILE" ]]; then
  set -a
  # shellcheck disable=SC1090
  . "$CRED_FILE"
  set +a
fi

if [[ -z "${TRELLO_KEY:-}" || -z "${TRELLO_TOKEN:-}" ]]; then
  cat >&2 <<'HELP'
TRELLO_KEY / TRELLO_TOKEN are not set.

Put them in ~/.trello.env (that path is outside the repository on purpose):

  TRELLO_KEY=your_key
  TRELLO_TOKEN=your_token

Then re-run. See .claude/scripts/README-trello.md for how to get both.
HELP
  exit 2
fi

API="https://api.trello.com/1"

# curl reads the auth header from stdin rather than argv, so the token never lands
# in the process list or in shell history.
api() {
  local method="$1" path="$2"
  shift 2
  printf 'header = "Authorization: OAuth oauth_consumer_key=\\"%s\\", oauth_token=\\"%s\\""\n' \
    "$TRELLO_KEY" "$TRELLO_TOKEN" |
    curl -sS --fail-with-body --config - -X "$method" "$API$path" "$@"
}

# Reads JSON on stdin and prints the given jq-ish fields. Uses python because this
# repo already depends on it for the other helper scripts and jq is not installed.
fmt() {
  python -c "$1"
}

usage() {
  cat <<'USAGE'
usage: trello.sh <command> [args]

  check                          whoami — proves the credentials work
  boards                         list your boards (id  name)
  lists <boardId>                lists on a board (id  name)
  cards <listId>                 open cards in a list (id  name)
  card <cardId>                  one card, with its description
  add <listId> <name> [desc]     create a card at the bottom of a list
  move <cardId> <listId>         move a card to another list
  comment <cardId> <text>        add a comment
  archive <cardId>               archive (close) a card
  board-new <name>               create a board
  list-new <boardId> <name>      create a list on a board

Ids are the long Trello ids, not the short URL slugs.
USAGE
}

cmd="${1:-}"
case "$cmd" in
  check)
    api GET "/members/me?fields=username,fullName" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("connected as", d["username"], "(" + (d.get("fullName") or "") + ")")'
    ;;

  boards)
    api GET "/members/me/boards?fields=name,closed" |
      fmt 'import sys,json
for b in json.load(sys.stdin):
    if not b.get("closed"):
        print(b["id"], b["name"], sep="\t")'
    ;;

  lists)
    api GET "/boards/${2:?boardId required}/lists?fields=name" |
      fmt 'import sys,json
for l in json.load(sys.stdin):
    print(l["id"], l["name"], sep="\t")'
    ;;

  cards)
    api GET "/lists/${2:?listId required}/cards?fields=name,shortUrl,due" |
      fmt 'import sys,json
for c in json.load(sys.stdin):
    print(c["id"], c["name"], c["shortUrl"], sep="\t")'
    ;;

  card)
    api GET "/cards/${2:?cardId required}?fields=name,desc,shortUrl,idList,closed" |
      fmt 'import sys,json
d=json.load(sys.stdin)
print("name:", d["name"])
print("url: ", d["shortUrl"])
print("list:", d["idList"], "  archived:", d["closed"])
print("---")
print(d.get("desc") or "(no description)")'
    ;;

  add)
    listId="${2:?listId required}"
    name="${3:?card name required}"
    desc="${4:-}"
    api POST "/cards" \
      --data-urlencode "idList=$listId" \
      --data-urlencode "name=$name" \
      --data-urlencode "desc=$desc" \
      --data-urlencode "pos=bottom" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("created", d["id"], d["shortUrl"])'
    ;;

  move)
    api PUT "/cards/${2:?cardId required}" \
      --data-urlencode "idList=${3:?listId required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("moved", d["id"], "->", d["idList"])'
    ;;

  comment)
    api POST "/cards/${2:?cardId required}/actions/comments" \
      --data-urlencode "text=${3:?comment text required}" |
      fmt 'import sys,json; json.load(sys.stdin); print("comment added")'
    ;;

  archive)
    api PUT "/cards/${2:?cardId required}" --data-urlencode "closed=true" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("archived", d["id"])'
    ;;

  board-new)
    api POST "/boards" \
      --data-urlencode "name=${2:?board name required}" \
      --data-urlencode "defaultLists=false" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("board", d["id"], d["shortUrl"])'
    ;;

  list-new)
    api POST "/lists" \
      --data-urlencode "idBoard=${2:?boardId required}" \
      --data-urlencode "name=${3:?list name required}" \
      --data-urlencode "pos=bottom" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("list", d["id"], d["name"])'
    ;;

  ""|-h|--help|help)
    usage
    ;;

  *)
    echo "unknown command: $cmd" >&2
    usage >&2
    exit 1
    ;;
esac
