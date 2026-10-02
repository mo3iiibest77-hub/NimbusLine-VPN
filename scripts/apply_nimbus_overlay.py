#!/usr/bin/env python3
from pathlib import Path
import re

ROOT = Path("V2rayNG")
PKG = ROOT / "app/src/main/java/com/v2ray/ang/nimbus"
OVERLAY = Path("../nimbus/src/main/java/com/v2ray/ang/nimbus")

def replace_once(path, old, new):
    p = Path(path)
    s = p.read_text()
    if old not in s:
        raise SystemExit(f"patch anchor not found: {path}: {old[:100]!r}")
    p.write_text(s.replace(old, new, 1))

PKG.mkdir(parents=True, exist_ok=True)
for src in OVERLAY.glob("*.kt"):
    dst = PKG / src.name
    dst.write_text(src.read_text())

# Application bootstrap: start WorkManager scheduling once the base app is initialized.
app = ROOT / "app/src/main/java/com/v2ray/ang/AngApplication.kt"
replace_once(
    app,
    "import com.v2ray.ang.handler.SettingsManager\n",
    "import com.v2ray.ang.handler.SettingsManager\nimport com.v2ray.ang.nimbus.NimbusBootstrap\n",
)
replace_once(
    app,
    "ThemeManager.refresh()\n",
    "ThemeManager.refresh()\n\n        NimbusBootstrap.initialize(this)\n",
)

# Subscription/import pipeline: parse normally, then keep only Nimbus-supported WS/XHTTP+TLS
# profiles and apply the hardcoded transport values before Xray config generation.
manager = ROOT / "app/src/main/java/com/v2ray/ang/handler/AngConfigManager.kt"
replace_once(
    manager,
    "import com.v2ray.ang.util.Utils\n",
    "import com.v2ray.ang.util.Utils\nimport com.v2ray.ang.nimbus.NimbusPolicy\n",
)
replace_once(
    manager,
    "val count = parseConfigViaSub(configText, it.guid, false)\n",
    "val count = parseConfigViaSub(configText, it.guid, false)\n            NimbusPolicy.filterSubscription(it.guid)\n",
)
replace_once(
    manager,
    "if (countSub > 0) {\n                updateConfigViaSubAll()\n            }\n",
    "if (countSub > 0) {\n                updateConfigViaSubAll()\n            }\n            NimbusPolicy.filterSubscription(subid)\n",
)

# UI: retain the upstream V2rayNG field architecture, but constrain Nimbus transport/security
# selectors to the two supported Cloudflare transports.
base = ROOT / "app/src/main/java/com/v2ray/ang/ui/server/BaseServerActivity.kt"
replace_once(
    base,
    "import com.v2ray.ang.ui.compose.verticalScrollbar\n",
    "import com.v2ray.ang.ui.compose.verticalScrollbar\nimport com.v2ray.ang.nimbus.NimbusConfigPipeline\nimport com.v2ray.ang.nimbus.NimbusPolicy\n",
)
replace_once(
    base,
    "networkOptions = stringArrayResource(R.array.networks).toList(),",
    "networkOptions = listOf(\"ws\", \"xhttp\"),",
)
replace_once(
    base,
    "streamSecurityOptions = stringArrayResource(R.array.streamsecurityxs).toList(),",
    "streamSecurityOptions = listOf(\"tls\"),",
)
replace_once(
    base,
    "if (!validateProtocolConfig(config)) return false\n\n        config.description",
    "if (!validateProtocolConfig(config)) return false\n        if (!NimbusPolicy.isSupported(config)) {\n            toast(R.string.toast_action_not_allowed)\n            return false\n        }\n\n        config.description",
)
replace_once(
    base,
    "val savedGuid = MmkvManager.encodeServerConfig(editGuid, config)\n",
    "val savedGuid = MmkvManager.encodeServerConfig(editGuid, config)\n        NimbusConfigPipeline.onProfileSaved(this, savedGuid)\n",
)

# Foreground worker requirements for API 34+. Add the tools namespace because the merged
# WorkManager service declaration uses tools:node.
manifest = ROOT / "app/src/main/AndroidManifest.xml"
m = manifest.read_text()
if "xmlns:tools=" not in m:
    m = m.replace(
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android"',
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools"',
        1,
    )
if "android.permission.FOREGROUND_SERVICE_DATA_SYNC" not in m:
    m = m.replace(
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">',
        '<manifest xmlns:android="http://schemas.android.com/apk/res/android" xmlns:tools="http://schemas.android.com/tools">\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />',
        1,
    )
worker_service = """        <service
            android:name="androidx.work.impl.foreground.SystemForegroundService"
            android:foregroundServiceType="dataSync"
            tools:node="merge" />
"""
if "androidx.work.impl.foreground.SystemForegroundService" not in m:
    m = m.replace("    <application", worker_service + "\n    <application", 1)
manifest.write_text(m)

# Branding/application id.
strings = ROOT / "app/src/main/res/values/strings.xml"
if strings.exists():
    s = strings.read_text()
    s = re.sub(r'(<string name="app_name"[^>]*>).*?(</string>)', r'\1NimbusLine\2', s, count=1)
    strings.write_text(s)

gradle = ROOT / "app/build.gradle.kts"
g = gradle.read_text()
g = g.replace('applicationId = "com.quietstorm.ng"', 'applicationId = "com.nimbusline.vpn"')
g = g.replace('versionName = "2.3.4"', 'versionName = "1.0.0"')
gradle.write_text(g)

print("NimbusLine overlay applied successfully.")
