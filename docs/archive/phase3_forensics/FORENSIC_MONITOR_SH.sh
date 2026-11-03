#!/bin/bash
# PERAZIM ANDROID — Independent Forensic Monitoring (post-audit)
# Read-only repeatable. Invoke after each workstream / phase / significant refactor.
# Source: /home/murugu/Android/app (repo root)
REPO=/home/murugu/Android/app; mkdir -p $REPO/forensic
DATE=$(date +%Y%m%d_%H%M%S)
echo "=== FORENSIC SNAPSHOT $DATE ===" >> $REPO/forensic/monitor_$DATE.log
(cd $REPO && echo "HEAD=$(git rev-parse HEAD) BRANCH=$(git branch --show-current)" >> $REPO/forensic/monitor_$DATE.log)
(cd $REPO && git status --short >> $REPO/forensic/monitor_$DATE.log)
(cd $REPO && echo "CLAIMED_PHASE0=$(git rev-parse 3ac4f8cb) CLAIMED_PHASE1=$(git rev-parse f24728e) CLAIMED_PHASE2=$(git rev-parse 9b4270a) CURRENT=$(git rev-parse HEAD)" >> $REPO/forensic/monitor_$DATE.log)
echo "PLACEHOLDER_CHECK=$(grep -rnci 'TODO\|FIXME\|stub\|placeholder\|not implemented\|return null' src/main/java/org/perazimchurch/app/community/ src/main/java/org/perazimchurch/app/presentation/Phase3* 2>/dev/null | tail -1)" >> $REPO/forensic/monitor_$DATE.log
echo "DEVICE=$(adb -d shell getprop ro.product.model 2>/dev/null) API=$(adb -d shell getprop ro.build.version.sdk 2>/dev/null) APP=$(adb -d shell pm list packages 2>/dev/null | grep 'org.perazimchurch.app')" >> $REPO/forensic/monitor_$DATE.log
echo "SNAPSHOT WRITTEN: $REPO/forensic/monitor_$DATE.log (read-only; no source modified)"
