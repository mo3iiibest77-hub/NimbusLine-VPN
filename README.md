# NimbusLine VPN

Standalone native Android VPN client written in Kotlin.

## Supported configurations

NimbusLine is intentionally narrow. It accepts only:

1. VLESS + WebSocket + TLS
2. VLESS + XHTTP + TLS

It is not a V2rayNG fork and does not build the V2rayNG application.

## Runtime

The application owns its UI, profile model, parser, validator, persistence, VPN service, scanner, and Xray configuration generation. Xray runs through the AndroidLibXrayLite Android binding.

Profile fields include address, port, UUID, encryption, flow, transport, WS/XHTTP host/path/mode/extra, TLS SNI/ALPN/fingerprint/allowInsecure/cipher suites, FinalMask JSON, and browser-dialer state.

When a scanner-selected Cloudflare edge IP is applied, only the profile address changes; the TLS SNI and HTTP Host remain the configured values.

## Scanner

A WorkManager job runs every two hours. It retrieves Cloudflare's public IPv4 ranges, samples candidate addresses, performs TCP + TLS/SNI + HTTP/WS reachability checks, and stores the best successful address per profile.

## Xray

GitHub Actions downloads AndroidLibXrayLite v26.7.31 as libv2ray.aar. The binding is kept separate from the application source. Upgrade the binding and its embedded Xray core together after compatibility testing.

FinalMask is emitted as streamSettings.finalmask when present.

## Build

GitHub Actions builds the standalone project and uploads the debug APK artifact.

For local Android Studio builds, obtain app/libs/libv2ray.aar from the AndroidLibXrayLite release used by CI, then run:

gradle assembleDebug
