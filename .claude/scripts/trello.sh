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

# Python otherwise inherits the Windows console codepage (CP1256 here), which cannot encode
# every character a card may contain and kills the read commands with UnicodeEncodeError.
# The API speaks UTF-8; so should we, in both directions.
export PYTHONIOENCODING=UTF-8

TMPDIR_SELF="$(mktemp -d)"
trap 'rm -rf "$TMPDIR_SELF"' EXIT

auth_config() {
  printf 'header = "Authorization: OAuth oauth_consumer_key=\\"%s\\", oauth_token=\\"%s\\""\n' \
    "$TRELLO_KEY" "$TRELLO_TOKEN"
}

# --- reads -------------------------------------------------------------------

get() {
  auth_config | curl -sS --fail-with-body --config - -X GET "$API$1"
}

# --- writes ------------------------------------------------------------------
#
# Everything is sent as a UTF-8 JSON body, never as form fields.
#
# `curl --data-urlencode` was the original approach and it silently destroyed every
# Persian string on this machine: the Windows build re-encoded the text into the ANSI
# codepage (CP1256) and percent-escaped the result twice, so a card titled «مدارک»
# was stored literally as "%E3%CF%C7%D1%98". The shell was innocent — it hands curl
# clean UTF-8 — so the fix is to keep the text away from curl's own encoders. Python
# writes the JSON, curl posts the bytes verbatim with --data-binary.
#
# Field values reach Python through a NUL-separated file rather than argv, because
# argv crosses the same Windows codepage boundary that broke it the first time, and
# a NUL separator is the only one that cannot appear inside a card description.

# write METHOD PATH [key value]...
write() {
  local method="$1" path="$2"
  shift 2

  local fields="$TMPDIR_SELF/fields" body="$TMPDIR_SELF/body.json"
  : >"$fields"
  while (($# >= 2)); do
    printf '%s\0%s\0' "$1" "$2" >>"$fields"
    shift 2
  done

  python -c '
import io, json, sys
raw = io.open(sys.argv[1], "rb").read()
parts = raw.split(b"\0")[:-1]
pairs = dict(zip(
    (p.decode("utf-8") for p in parts[0::2]),
    (p.decode("utf-8") for p in parts[1::2]),
))
io.open(sys.argv[2], "w", encoding="utf-8", newline="\n").write(
    json.dumps(pairs, ensure_ascii=False))
' "$fields" "$body"

  {
    auth_config
    printf 'header = "Content-Type: application/json"\n'
  } | curl -sS --fail-with-body --config - -X "$method" "$API$path" --data-binary "@$body"
}

fmt() { python -c "$1"; }

usage() {
  cat <<'USAGE'
usage: trello.sh <command> [args]

reads
  check                          whoami — proves the credentials work
  boards                         list your boards (id  name)
  lists <boardId>                lists on a board (id  name)
  cards <listId>                 open cards in a list (id  name  url)
  card <cardId>                  one card, with its description
  comments <cardId>              comments on a card (actionId  text)

writes
  add <listId> <name> [desc]     create a card at the bottom of a list
  move <cardId> <listId>         move a card to another list
  rename <cardId> <name>         change a card's title
  describe <cardId> <desc>       replace a card's description
  comment <cardId> <text>        add a comment
  comment-rm <cardId> <actionId> delete one comment
  archive <cardId>               archive (close) a card
  delete <cardId>                delete a card permanently
  board-new <name>               create a board
  list-new <boardId> <name>      create a list on a board
  list-rename <listId> <name>    rename a list
  trailer <cardId>               print the `Trello:` line for a commit message
  link <cardId> <sha>            comment a commit on its card

labels
  labels <boardId>               labels on a board (id  colour  name)
  label-new <boardId> <name> <colour>
  label-set <labelId> <name> <colour>
  label-add <cardId> <labelId>   put a label on a card
  label-rm <cardId> <labelId>    take it off
  card-labels <cardId>           labels currently on a card

Ids are the long Trello ids, not the short URL slugs.
USAGE
}

cmd="${1:-}"
case "$cmd" in
  check)
    get "/members/me?fields=username,fullName" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("connected as", d["username"], "(" + (d.get("fullName") or "") + ")")'
    ;;

  boards)
    get "/members/me/boards?fields=name,closed" |
      fmt 'import sys,json
for b in json.load(sys.stdin):
    if not b.get("closed"):
        print(b["id"], b["name"], sep="\t")'
    ;;

  lists)
    get "/boards/${2:?boardId required}/lists?fields=name" |
      fmt 'import sys,json
for l in json.load(sys.stdin):
    print(l["id"], l["name"], sep="\t")'
    ;;

  cards)
    get "/lists/${2:?listId required}/cards?fields=name,shortUrl" |
      fmt 'import sys,json
for c in json.load(sys.stdin):
    print(c["id"], c["name"], c["shortUrl"], sep="\t")'
    ;;

  card)
    get "/cards/${2:?cardId required}?fields=name,desc,shortUrl,idList,closed" |
      fmt 'import sys,json
d=json.load(sys.stdin)
print("name:", d["name"])
print("url: ", d["shortUrl"])
print("list:", d["idList"], "  archived:", d["closed"])
print("---")
print(d.get("desc") or "(no description)")'
    ;;

  comments)
    get "/cards/${2:?cardId required}/actions?filter=commentCard" |
      fmt 'import sys,json
for a in json.load(sys.stdin):
    print(a["id"], a["data"]["text"].replace("\n", " ⏎ ")[:120], sep="\t")'
    ;;

  add)
    write POST "/cards" \
      idList "${2:?listId required}" \
      name "${3:?card name required}" \
      desc "${4:-}" \
      pos bottom |
      fmt 'import sys,json; d=json.load(sys.stdin); print("created", d["id"], d["shortUrl"])'
    ;;

  move)
    write PUT "/cards/${2:?cardId required}" idList "${3:?listId required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("moved", d["id"], "->", d["idList"])'
    ;;

  rename)
    write PUT "/cards/${2:?cardId required}" name "${3:?name required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("renamed", d["id"])'
    ;;

  describe)
    write PUT "/cards/${2:?cardId required}" desc "${3?desc required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("described", d["id"])'
    ;;

  comment)
    write POST "/cards/${2:?cardId required}/actions/comments" text "${3:?comment text required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("comment added", d["id"])'
    ;;

  comment-rm)
    # /actions/{id} rather than /cards/{id}/actions/{id}/comments -- the nested form 404s.
    auth_config | curl -sS --fail-with-body --config - \
      -X DELETE "$API/actions/${3:?actionId required}" >/dev/null
    echo "comment deleted"
    ;;

  archive)
    write PUT "/cards/${2:?cardId required}" closed true |
      fmt 'import sys,json; d=json.load(sys.stdin); print("archived", d["id"])'
    ;;

  delete)
    auth_config | curl -sS --fail-with-body --config - \
      -X DELETE "$API/cards/${2:?cardId required}" >/dev/null
    echo "deleted ${2}"
    ;;

  board-new)
    write POST "/boards" name "${2:?board name required}" defaultLists false |
      fmt 'import sys,json; d=json.load(sys.stdin); print("board", d["id"], d["shortUrl"])'
    ;;

  list-new)
    write POST "/lists" idBoard "${2:?boardId required}" name "${3:?list name required}" pos bottom |
      fmt 'import sys,json; d=json.load(sys.stdin); print("list", d["id"], d["name"])'
    ;;

  list-rename)
    write PUT "/lists/${2:?listId required}" name "${3:?name required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("renamed list", d["id"], d["name"])'
    ;;

  labels)
    get "/boards/${2:?boardId required}/labels?fields=name,color&limit=50" |
      fmt 'import sys,json
for l in json.load(sys.stdin):
    print(l["id"], l["color"] or "-", l["name"] or "(unnamed)", sep="	")'
    ;;

  label-new)
    write POST "/labels"       idBoard "${2:?boardId required}"       name "${3:?label name required}"       color "${4:?colour required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("label", d["id"], d["color"], d["name"])'
    ;;

  label-set)
    write PUT "/labels/${2:?labelId required}" name "${3:?name required}" color "${4:?colour required}" |
      fmt 'import sys,json; d=json.load(sys.stdin); print("label", d["id"], d["color"], d["name"])'
    ;;

  label-add)
    write POST "/cards/${2:?cardId required}/idLabels" value "${3:?labelId required}" >/dev/null
    echo "label added"
    ;;

  label-rm)
    auth_config | curl -sS --fail-with-body --config -       -X DELETE "$API/cards/${2:?cardId required}/idLabels/${3:?labelId required}" >/dev/null
    echo "label removed"
    ;;

  card-labels)
    get "/cards/${2:?cardId required}/labels?fields=name,color" |
      fmt 'import sys,json
for l in json.load(sys.stdin):
    print(l["id"], l["color"] or "-", l["name"] or "(unnamed)", sep="	")'
    ;;

  trailer)
    get "/cards/${2:?cardId required}?fields=shortUrl,idShort,name" |
      fmt 'import sys,json
d=json.load(sys.stdin)
print("Trello: %s" % d["shortUrl"])'
    ;;

  link)
    # The card learns the sha; the commit already carries the card URL in its trailer.
    # Run this after committing -- the sha does not exist before.
    card="${2:?cardId required}"
    sha="${3:?commit sha required}"
    line="$(git log -1 --format='%h %s' "$sha")"
    branch="$(git rev-parse --abbrev-ref HEAD)"
    write POST "/cards/$card/actions/comments"       text "Committed on \`$branch\`: $line" |
      fmt 'import sys,json; json.load(sys.stdin); print("linked")'
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
