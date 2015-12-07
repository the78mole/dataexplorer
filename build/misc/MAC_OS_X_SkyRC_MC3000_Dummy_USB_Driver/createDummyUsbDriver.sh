#!/bin/bash
# ***** script to create a dummy USB driver to make SkyRC MC3000 USB adapter usable with MAC OS X ** 06 Dec 2015 WB *****
# execution: sudo createDummyUsbDriver.sh
cp -r MC3000.kext /System/Library/Extensions/
cd /System/Library/Extensions
chown -R root:wheel MC3000.kext
chmod -R 755 MC3000.kext
kextcache -system-caches
echo MC3000 dummy driver installed please reboot the system before connecting MC3000 again.
