#!/usr/bin/env python3
from pathlib import Path

ROOT = Path("V2rayNG")
PKG = ROOT / "app/src/main/java/com/v2ray/ang/nimbus"
OVERLAY = Path("../nimbus/src/main/java/com/v2ray/ang/nimbus")

def replace_once(path, old, new):
    p = Path(path)
    s = p.read_text()
    if old not in s:
        raise SystemExit(f"patch anchor not found: {path}: {old[:80]!r}")
    p.write_text(s.replace(old, new, 1))

PKG.mkdir(parents=True, exist_ok=True)
for src in OVERLAY.glob("*.kt"):
    dst = PKG / src.name
    dst.write_text(src.read_text().replace("import com.v2ray.ang.handler.IspManager", "import com.v2ray.ang.senpai.IspManager"))

# Start NimbusLine's scheduler with the application.
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

# Enforce the protocol/transport policy after every subscription refresh.
manager = ROOT / "app/src/main/java/com/v2ray/ang/handler/AngConfigManager.kt"
replace_once(
    manager,
    "val count = parseConfigViaSub(configText, it.guid, false)\n",
    "val count = parseConfigViaSub(configText, it.guid, false)\n            NimbusPolicy.filterSubscription(it.guid)\n",
)
replace_once(
    manager,
    "import com.v2ray.ang.util.Utils\n",
    "import com.v2ray.ang.util.Utils\nimport com.v2ray.ang.nimbus.NimbusPolicy\n",
)
# Direct pasted/imported configs are filtered too.
replace_once(
    manager,
    "if (countSub > 0) {\n                updateConfigViaSubAll()\n            }\n",
    "if (countSub > 0) {\n                updateConfigViaSubAll()\n            }\n            NimbusPolicy.filterSubscription(subid)\n",
)

# Keep the Android foreground-worker requirements explicit for API 34+.
manifest = ROOT / "app/src/main/AndroidManifest.xml"
m = manifest.read_text()
perm_anchor = '<manifest xmlns:android="http://schemas.android.com/apk/res/android"'
if "android.permission.FOREGROUND_SERVICE_DATA_SYNC" not in m:
    m = m.replace(
        perm_anchor,
        perm_anchor + '\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />',
        1,
    )
worker_service = '''        <service
            android:name="androidx.work.impl.foreground.SystemForegroundService"
            android:foregroundServiceType="dataSync"
            tools:node="merge" />\n'''
if "androidx.work.impl.foreground.SystemForegroundService" not in m:
    marker = "    <application"
    m = m.replace(marker, worker_service + "\n" + marker, 1)
manifest.write_text(m)

# Nimbus branding at the application level without rewriting the UI.
strings = ROOT / "app/src/main/res/values/strings.xml"
if strings.exists():
    s = strings.read_text()
    import re
    s = re.sub(r'(<string name="app_name"[^>]*>).*?(</string>)', r'\1NimbusLine\2', s, count=1)
    strings.write_text(s)

# Make the build application identity NimbusLine.
gradle = ROOT / "app/build.gradle.kts"
g = gradle.read_text()
g = g.replace('applicationId = "com.quietstorm.ng"', 'applicationId = "com.nimbusline.vpn"')
g = g.replace('versionName = "2.3.4"', 'versionName = "1.0.0"')
gradle.write_text(g)

print("NimbusLine overlay applied successfully.")
