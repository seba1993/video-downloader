# Downloader Remote API

The remote API is embedded in `Downloader.jar` and uses the same managers as the
Swing interface. It never edits serialized configuration files directly.

## Configuration

Create `remote-api.properties` next to `Downloader.jar`:

```properties
enabled=true
bind=0.0.0.0
port=8787
token=replace-with-a-long-random-token
```

- `enabled`: starts the server when `true`.
- `bind`: use `127.0.0.1` for local access or `0.0.0.0` for LAN/VPN access.
- `port`: HTTP port, `8787` by default.
- `token`: required credential with at least 16 characters.

Restart the downloader after changing this file. The dashboard is available at
`http://COMPUTER_IP:8787/`.

Do not expose this port directly to the Internet. Use a local network, Tailscale,
or another VPN. The API uses HTTP and its token is not encrypted in transit.

## Authentication

Every `/api` request requires one of these headers:

```text
Authorization: Bearer TOKEN
X-API-Token: TOKEN
```

The dashboard asks for the token and keeps it in browser session storage.

## Monitoring Endpoints

```text
GET /api/health
GET /api/streams
GET /api/streams/{uid}
GET /api/tasks
GET /api/logs?limit=100
GET /api/system
```

`limit` is restricted to a maximum of 500 log lines.

Example:

```powershell
$headers = @{ Authorization = "Bearer TOKEN" }
Invoke-RestMethod http://127.0.0.1:8787/api/health -Headers $headers
Invoke-RestMethod http://127.0.0.1:8787/api/streams -Headers $headers
```

## Control Endpoints

```text
POST /api/streams/{uid}/enable
POST /api/streams/{uid}/disable
POST /api/streams/{uid}/restart
POST /api/streams/{uid}/refresh
```

- `enable` and `disable` persist the setting through `ConfigurationManager`.
- `restart` stops and removes the current task, then rebuilds its schedule. An
  enabled stream starts again when its schedule is valid, normally within the
  scheduler interval.
- `refresh` rebuilds the schedule without stopping the current recording.

Example:

```powershell
Invoke-RestMethod http://127.0.0.1:8787/api/streams/123456/disable `
  -Method Post -Headers $headers
```

The first version deliberately does not expose shutdown, deletion, creation, or
arbitrary command execution.

## Windows Firewall

LAN access may require an inbound Windows Firewall rule for TCP port `8787`.
Create it only for private networks and keep the API behind the token and a
trusted LAN or VPN.

Run this command once in PowerShell as Administrator:

```powershell
New-NetFirewallRule -DisplayName "Video Downloader Remote API" `
  -Direction Inbound -Protocol TCP -LocalPort 8787 `
  -Action Allow -Profile Private
```
