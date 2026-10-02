# NimbusLine VPN — Cloudflare Scanner

This repository is the new home for the Cloudflare scanner component extracted from mo3iiibest77-hub/PattNG.

## Ported scanner components

- V2rayNG/app/src/main/java/com/v2ray/ang/senpai/CloudflareScanner.kt — Cloudflare CIDR candidate generation, latency + traffic validation, good-CIDR discovery, ISP-targeted scanning, and best-IP application.
- V2rayNG/app/src/main/java/com/v2ray/ang/senpai/IspManager.kt — persisted ISP/CIDR profiles, discovery progress, and manual CIDR management.
- V2rayNG/app/src/main/assets/cf_ranges_v4.txt — Cloudflare IPv4/CIDR candidate list used by the scanner.

## Important

The scanner is intentionally preserved close to the source implementation first. It currently depends on the surrounding V2rayNG/Xray runtime types (ProfileItem, MmkvManager, CoreConfigManager, CoreNativeManager, and RealTrafficSpeedTest). The next step is to build NimbusLine's standalone VPN runtime around these scanner contracts rather than silently rewriting scanner behavior during extraction.

Source snapshot: PattNG commit c489a5aa2b59ccb92be7c8f849ef80ef54f99c81.
