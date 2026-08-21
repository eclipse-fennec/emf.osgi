# HTTP URI Handler Security — User Guide

This guide explains how Fennec EMF OSGi controls **outbound `http`/`https` resolution** and
how to configure it. It matters to anyone whose application loads EMF models that may contain
cross-document references (proxies), and especially to anyone exposing EMF content over a
network (for example through the `emf.codec` REST integration).

---

## Why this exists

EMF resolves an object reference whose target lives in another document by *demand-loading*
that document. If the reference URI is an absolute `http://…` or `https://…`, EMF opens a
network connection to fetch it — during deserialization, before your code ever sees the
object.

When the model being loaded comes from an untrusted source (an HTTP request body, a message,
an uploaded file), the reference URI is attacker-controlled. A crafted reference such as

```json
{ "name": "x", "manager": { "$ref": "http://169.254.169.254/latest/meta-data/…#//0" } }
```

makes the server issue a request to an address of the attacker's choosing — a **server-side
request forgery (SSRF, CWE-918)**, reaching cloud-metadata endpoints, internal admin services,
service-mesh sidecars, and so on.

In Fennec, every `ResourceSet` created by the `ResourceSetFactory` has the
`RestfulURIHandlerImpl` installed as the handler for `http`/`https`. That handler is therefore
the single place where this fetch happens, and where it is now controlled.

---

## Default behaviour: reads are blocked

**Out of the box, with no configuration, the handler refuses to demand-load any `http`/`https`
URI.** `createInputStream` throws an `IOException` before opening a connection:

```
Blocked outbound http(s) resolution of URI 'http://…' (host '…' is not in the configured
allow-list). Add the host to the REST URI handler configuration, or set option
'allow.uri.resolution'=Boolean.TRUE for a trusted, manual load of this URI.
```

Only the **read / demand-load** path is blocked. The handler's REST-**client** methods —
`createOutputStream` (PUT/POST), `delete`, `exists`, `getAttributes` — are unaffected, so code
that deliberately talks to a REST endpoint keeps working without any configuration.

> This is a behaviour change. If your application relied on EMF resolving `http(s)` proxy
> references automatically, you must now allow the relevant hosts (below).

---

## Allowing hosts (Config Admin)

To permit resolution against known, trusted hosts, provide a Config Admin configuration for
PID **`org.eclipse.fennec.emf.osgi.urihandler.http`** with the `allowedHosts` property.

`configuration.json` (Fennec Configurator / `configurator` format):

```json
{
  "org.eclipse.fennec.emf.osgi.urihandler.http": {
    "allowedHosts": [ "models.example.com", "registry.internal" ]
  }
}
```

Equivalent with the OSGi `ConfigurationAdmin` API:

```java
Configuration cfg = configAdmin.getConfiguration("org.eclipse.fennec.emf.osgi.urihandler.http", "?");
Dictionary<String, Object> props = new Hashtable<>();
props.put("allowedHosts", new String[] { "models.example.com", "registry.internal" });
cfg.update(props);
```

- Hosts are matched **case-insensitively, host only** — no port, no path. `models.example.com`
  matches `http://models.example.com:8080/a/b#…`.
- An empty or absent `allowedHosts` keeps the secure default (everything blocked).
- Requests to any host **not** listed still throw.

### Wildcard patterns

Each `allowedHosts` entry may also be a wildcard:

| Entry | Matches | Does **not** match |
|---|---|---|
| `models.example.com` | that exact host | anything else |
| `*.mydomain.com` | `a.mydomain.com`, `a.b.mydomain.com` (any subdomain) | the apex `mydomain.com`, or look-alikes like `evilmydomain.com` |
| `*` | **every** host | — |

The subdomain wildcard is anchored on the dot, so `*.mydomain.com` never matches
`evilmydomain.com`, and it does **not** include the apex — list `mydomain.com` separately if you
need it.

> ⚠️ **Warning — `*` disables SSRF protection.** A bare `*` permits outbound resolution to *any*
> host, which re-opens exactly the server-side request forgery vector this control exists to
> close. An attacker-supplied reference could then reach cloud-metadata endpoints, internal
> services, and so on. Only use `*` in a trusted, closed environment where the models being
> loaded are fully under your control. Prefer an explicit host list (or `*.yourdomain`) in any
> environment that processes untrusted input. Configuring `*` is logged as a warning at startup.

---

## Allowing a single URI per call

When your own trusted code drives a `ResourceSet` and wants to load one specific, known URI —
without whitelisting its host globally — set the load option
`EMFUriHandlerConstants.OPTION_ALLOW_URI_RESOLUTION` (`"allow.uri.resolution"`) to
`Boolean.TRUE`:

```java
Map<Object, Object> options = new HashMap<>();
options.put(EMFUriHandlerConstants.OPTION_ALLOW_URI_RESOLUTION, Boolean.TRUE);
resource.load(options); // this load may resolve its http(s) references
```

The option applies only to the operation whose options carry it. Attacker-driven demand-loads
go through the `ResourceSet`'s own load options and never carry this key, so they stay blocked.

---

## Discovering http-capable ResourceSets

While a non-empty whitelist is configured, the `ResourceSetFactory` and `ResourceSet` services
advertise the service property **`emf.uri.handler.http=true`**
(`EMFNamespaces.PROP_URI_HANDLER_HTTP`). A consumer that needs a ResourceSet able to resolve
`http(s)` can select one with a target filter:

```java
@Reference(target = "(emf.uri.handler.http=true)")
ResourceSet resourceSet;
```

When no whitelist is configured, the property is absent (the ResourceSet blocks http reads).
The configured host list itself is **not** exposed as a service property.

---

## Scope and limitations

- **Covered:** `http` and `https` demand-loads through ResourceSets created by the Fennec
  `ResourceSetFactory` (the normal path, including the `emf.codec` REST integration).
- **Not covered by this handler:** `file://` and `ftp://` are served by EMF's *default*
  handlers, not by `RestfulURIHandlerImpl`. Do not rely on this policy to prevent local-file
  reads from untrusted models.
- **Raw `new ResourceSetImpl()`** does not go through the Fennec configurator and therefore has
  none of this protection. Obtain your `ResourceSet` from the Fennec `ResourceSetFactory`
  service (or a pre-configured `ResourceSet` service) so the policy applies.

---

## Quick reference

| Concern | Setting |
|---|---|
| Config PID | `org.eclipse.fennec.emf.osgi.urihandler.http` |
| Whitelist property | `allowedHosts` (`String[]`; exact host, `*.suffix`, or `*` for all) |
| Default (no config) | all `http(s)` demand-loads blocked |
| Per-call override | load/save option `allow.uri.resolution` = `Boolean.TRUE` |
| Capability property | `emf.uri.handler.http=true` (only when a whitelist is configured) |
| Blocked method | `createInputStream` (reads); write/probe methods unchanged |
