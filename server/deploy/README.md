# Phase A public deployment preparation — NOT DEPLOYED

The owner has not accepted Phase A. There is no rented VPS, configured public
hostname, or verified public HTTPS endpoint. This directory is preparation, not
deployment evidence. Do not run multi-user acceptance tests until a phone using
normal mobile data can reach the public HTTPS endpoint without Wi-Fi, USB,
`adb reverse`, a VPN to the PC, or a laptop proxy. Do not start Phase B work.

## What the owner must supply

- A funded hosting project with permission to create the VPS, or an existing
  Linux VPS IP plus SSH username and authorized public-key access. Do not paste
  passwords, API tokens, or private keys into chat. Account invitations require
  an actual accepting operator; a provisioned VPS with SSH access is sufficient.
- A budget/provider decision. Proposed small pilot: DigitalOcean Basic Regular,
  1 vCPU / 2 GiB / 50 GiB, US$12/month; optional weekly backups add US$2.40/month.
  Taxes, domain registration, and usage above included allowances are extra.
- An API hostname and DNS access, such as an owner-chosen subdomain of an existing
  church domain. A new domain's cost depends on registrar/TLD and needs a separate
  decision. Set its A record to the VPS; publish AAAA only with working IPv6.
- Confirmation whether this is a new pilot database or an approved migration of
  existing member data, plus the designated operator responsible for backups.
  Never replace the phone's endpoint/session or import a real database silently.
- A phone available for the initial HTTPS reachability check on normal mobile
  data. Multi-user acceptance remains held until this check passes.

Prices checked 2026-11-06: [Droplets](https://www.digitalocean.com/pricing/droplets),
[backups](https://www.digitalocean.com/pricing/backups). Certificates can be free
through [Let's Encrypt](https://letsencrypt.org/getting-started/).

## Prepared stack and rollout limits

`compose.yml` is a standalone stack, not an override for the development Compose
file. Caddy publishes TCP 80/443; API and PostgreSQL have no host port mappings.
PostgreSQL is on an internal network. API/database/uploads/certificate storage
survive container replacement; restart policies resume after Docker starts.
Caddy's public hostname enables automatic certificate renewal and HTTP-to-HTTPS
redirection. Preserve its `/data` volume. The image build explicitly excludes
workstation credentials, `.runtime`, tests, logs, uploads, and `.env`; it uses
locked npm dependencies and a non-root Node 24 LTS process.

This checkout already contains community code and Phase B tables from earlier
work. No Phase B implementation or acceptance is part of this preparation.
The proxy returns 404 for `/api/community` and its child paths, but the API's
startup still initializes the existing mixed schema. Review the selected commit
and migration plan before deploying; this is not a claim of an isolated Phase A
release. Never delete existing tables to make a release appear Phase A-only.

Docker and Caddy are unavailable on the current PC. The files are statically
reviewed; image build, Compose validation, certificate issuance, permission
checks, firewall checks, and public reachability must be verified on the VPS.

PIN throttling uses Express `req.ip`. This stack explicitly sets
`TRUST_PROXY_HOPS=1` because the API is private behind exactly one Caddy proxy.
Unset is the default for local/direct access and ignores forwarded headers;
any other configured value is rejected. A pure request/configuration unit test
checks those settings and rejects spoofed extra hops. The actual VPS must still
verify Caddy's forwarding behavior and absence of direct API access before
opening the pilot. Never copy the one-hop setting to a directly reachable API
or a topology with different proxy paths.

## Operator steps after access and budget are supplied

1. Provision a maintained Linux distribution and Docker Engine with Compose.
   Restrict SSH to the operator; allow inbound TCP 80/443 in both provider and
   host firewalls. Do not expose 3000 or 5432. Verify Docker's published ports
   from outside the VPS rather than relying only on host firewall assumptions.
2. Transfer the reviewed commit to the VPS through authorized repository/SSH
   access. Work in `server/deploy`. Copy `.env.example` to `.env`, restrict it to
   the operator (`chmod 600 .env`), and fill the hostname/operator email. Generate
   independent database and JWT values with `openssl rand -hex 32`. The hex
   password avoids database URL escaping ambiguity. Keep `.env` out of Git and
   terminal logs. The required-value checks reject blank values.
3. Validate without printing interpolated secrets:

   ```bash
   docker compose --env-file .env -f compose.yml config --quiet
   docker compose --env-file .env -f compose.yml build --pull
   docker compose --env-file .env -f compose.yml run --rm --no-deps --entrypoint caddy https validate --config /etc/caddy/Caddyfile --adapter caddyfile
   docker compose --env-file .env -f compose.yml up -d
   docker compose --env-file .env -f compose.yml ps
   ```

   Investigate unhealthy services before continuing. No npm acceptance suite is
   run by these commands. Do not use `docker compose down -v`: that deletes data.
4. Verify trusted HTTPS from an external network at
   `https://<chosen-hostname>/api/health`; do not bypass TLS validation. Record
   timestamp, actual hostname, status, and expected application JSON. Check an
   unauthenticated `/api/auth/me` returns 401 and `/api/community` returns 404.
5. Check the same health URL in the phone browser on normal mobile data with
   Wi-Fi off and USB disconnected. A browser check establishes reachability;
   it does not prove Android app sync. Only after this passes may multi-user
   acceptance start. Use designated test accounts and a documented pilot plan.
6. Set the Android API endpoint to the exact public HTTPS origin in an explicit
   reviewed app update, rebuild/install, and verify the chosen endpoint. At
   present `ApiClient.DEFAULT_DEVICE_URL` is localhost and its runtime setter
   has no settings UI caller. Do not leave the phone using USB localhost or
   change its real account to an independent server database without a migration
   decision. Record app-level mobile-data evidence separately.

## Backup and rollback

Before an update, record the running commit/image IDs and take an application-
consistent backup. During a scheduled maintenance window stop the API (leave DB
running), create a custom-format `pg_dump` with `docker compose ... exec -T db
pg_dump -U perazim_user -d perazim_church -Fc`, and archive uploads from its named
volume. Save outputs outside the container with restrictive permissions. Resume
the API after both finish. Copy encrypted backups off-host and verify a restore
in an isolated database before relying on them. Provider disk backups supplement
these backups; they do not by themselves prove database/application consistency.

Never restore into the live database without an explicit maintenance/recovery
decision and a pre-restore backup. Code rollback alone does not undo schema
changes. Keep private `.env` and certificate storage recoverable; rotating JWT
secrets invalidates member sessions. Retain database/uploads volumes when
replacing containers.

References: [Caddy Compose deployment](https://caddyserver.com/docs/running#docker-compose),
[automatic HTTPS](https://caddyserver.com/docs/automatic-https),
[Node supported releases](https://nodejs.org/en/about/previous-releases).
