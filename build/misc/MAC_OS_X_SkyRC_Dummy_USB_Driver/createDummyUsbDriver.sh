#!/bin/bash
# ***** script to create a dummy USB driver to make SkyRC device USB adapter usable with MAC OS X ** 01 Dec 2016 WB *****
# execution: sudo ./createDummyUsbDriver.sh
cp -r SkyRC.kext /System/Library/Extensions/
cd /System/Library/Extensions
chown -R root:wheel SkyRC.kext
chmod -R 755 SkyRC.kext
kextcache -system-caches
echo SkyRC USB dummy driver installed please reboot the system before connecting a SKyRC USB device again.
echo MAC OS X >= 10.13 High Sierra kernel extensions might be blocked, reboot into recovery console "(push cmd+R while boot)" open terminal
echo enter "spctl kext-consent disable" and "csrutil disable", reboot to activate
echo open terminal enter "sudo kextutil /System/Library/Extensions/SkyRC.kext" before USB cable connected. This will load non signed kext
