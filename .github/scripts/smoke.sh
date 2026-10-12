#!/usr/bin/env bash
set -euo pipefail
mkdir -p smoke-output
PACKAGE=com.lodoskent.makro
SERVICE=com.lodoskent.makro/.MacroAccessibilityService
echo "Installing APK and launching accessibility service"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb logcat -c
adb shell settings put secure enabled_accessibility_services "$SERVICE"
adb shell settings put secure accessibility_enabled 1
adb shell am start -n "$PACKAGE/.MainActivity"
sleep 14
adb shell dumpsys accessibility > smoke-output/accessibility.txt || true
adb shell dumpsys activity services "$PACKAGE" > smoke-output/activity-services.txt || true
adb logcat -d -v time > smoke-output/logcat.txt
echo "=== Relevant Android logs ==="
grep -Ei "LKMacro|AndroidRuntime|FATAL EXCEPTION|com.lodoskent.makro|AccessibilityService" smoke-output/logcat.txt | tail -100 || true
echo "=== Accessibility service status ==="
grep -Ei -A 12 -B 4 "lodoskent|enabled services|bound services" smoke-output/accessibility.txt | head -120 || true
if grep -q "FATAL EXCEPTION" smoke-output/logcat.txt; then
    echo "Crash found in logcat!"
    exit 1
fi
if ! grep -q "Accessibility service connected; bubble=true" smoke-output/logcat.txt; then
    echo "Service not confirmed alive with floating bubble!"
    exit 1
fi
echo "Accessibility smoke test passed"
