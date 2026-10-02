# NimbusLine VPN overlay

NimbusLine is an Xray-based Android VPN derived from the user's PattNG/V2rayNG source.

The build workflow checks out the pinned PattNG source, overlays NimbusLine's policy/scanner code, applies the source patch, and builds the Android APK.

## Runtime policy

Only profiles using TLS + WebSocket or TLS + XHTTP are retained. The initial supported port set is 443 and 53 as requested; Cloudflare's documented default HTTPS proxy ports include 443 and do not include 53, so port 53 is intentionally treated as an explicit NimbusLine compatibility option rather than claimed as a default Cloudflare HTTPS proxy port.

Hardcoded TLS/final-mask values live in NimbusPolicy.kt and are intentionally centralized so the owner can replace them later.

The existing CloudflareScanner from PattNG is reused without changing its core scan algorithm.
