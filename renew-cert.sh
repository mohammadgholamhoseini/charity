#!/bin/sh
# A MIRROR of /opt/yariju/charity/renew-cert.sh on the VPS, run twice daily by
# /etc/cron.d/certbot-renew. The deploy copies no files -- change both or neither.
#
# Renews the Let's Encrypt certificate, then reloads nginx so it picks the new one up.
#
# certbot only acts when the certificate is inside its 30-day renewal window; on every
# other run it exits 0 having done nothing, so this is safe to run twice a day.
#
# The reload is not optional. nginx reads the certificate files at startup and holds
# them, so after a successful renewal it would go on serving the expired one until
# something else happened to restart it. The reload is graceful -- no dropped requests.
set -e
cd /opt/yariju/charity
docker compose -f compose.prod.yaml run --rm certbot renew --quiet
docker compose -f compose.prod.yaml exec -T nginx nginx -s reload
