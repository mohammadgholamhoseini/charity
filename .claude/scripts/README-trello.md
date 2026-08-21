# Trello access for the `tasks` agent

`trello.sh` is a thin wrapper over the Trello REST API. It exists so leftover work has a home
outside a chat transcript — a session ends, a context window compacts, and a "we still need to
drop `documents_json`" note dies with it.

## Getting a key and a token

Both come from Trello, in a browser, signed in as yourself. Claude does not do this step and
should never be given the values.

1. Open <https://trello.com/power-ups/admin> and create a Power-Up (any name — it exists only to
   own an API key). Under **API key**, generate one. That is `TRELLO_KEY`.
2. On the same page, next to the key, click **Token**. Trello asks you to authorise the Power-Up
   against your own account and then shows a long string. That is `TRELLO_TOKEN`.

The token is as powerful as your Trello password on the boards it can reach. Treat it that way.

## Where to put them

In `~/.trello.env` — that is `C:\Users\<you>\.trello.env` on this machine. **Outside the
repository, deliberately**, so no `git add -A` can ever sweep it in:

```
TRELLO_KEY=...
TRELLO_TOKEN=...
```

No quotes, no `export`, one per line. `trello.sh` sources it only when the variables are not
already in the environment, so a real environment variable wins if you prefer that.

Override the location with `TRELLO_ENV_FILE=/some/other/path` if you keep secrets elsewhere.

## Verify

```bash
bash .claude/scripts/trello.sh check
```

Prints your username and nothing else. If it prints the setup help instead, the file is missing
or the names inside it are misspelt.

## What the script does with the credentials

- Reads them from the environment or `~/.trello.env`, never from anywhere in this repository.
- Sends them in an `Authorization` header, never as a URL query parameter — so they stay out of
  proxy logs and out of any URL that gets pasted somewhere.
- Passes that header to `curl` through `--config` on **stdin**, so it does not appear in the
  process list where any other user on the machine could read it with `ps`.
- Never echoes them. `check` prints a username, not a token.

## Commands

```
check                          whoami — proves the credentials work
boards                         list your boards (id  name)
lists <boardId>                lists on a board
cards <listId>                 open cards in a list
card <cardId>                  one card, with its description
add <listId> <name> [desc]     create a card at the bottom of a list
move <cardId> <listId>         move a card to another list
comment <cardId> <text>        add a comment
archive <cardId>               archive (close) a card
board-new <name>               create a board
list-new <boardId> <name>      create a list on a board
```

Ids are the long Trello ids the API returns, not the short slugs in a browser URL.

## If the key is ever exposed

Revoke it at <https://trello.com/power-ups/admin> — deleting the Power-Up invalidates the key and
every token issued against it. Individual tokens can also be revoked from
<https://trello.com/my/account> under **Applications**.
