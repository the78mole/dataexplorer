#!/bin/bash
# ***** script to create a dummy USB driver to make Junsi iCHarger 308/4010 DUO USB adapter usable with MAC OS X ** 16 May 2016 WB *****
# execution: sudo createDummyUsbDriver.sh
cp -r iChargerDuo.kext /System/Library/Extensions/
cd /System/Library/Extensions
chown -R root:wheel iChargerDuo.kext
chmod -R 755 iChargerDuo.kext
kextcache -system-caches
echo iCharger308/4010 Duo dummy driver installed please reboot the system before connecting iCharger 308/4010 Duo again.
