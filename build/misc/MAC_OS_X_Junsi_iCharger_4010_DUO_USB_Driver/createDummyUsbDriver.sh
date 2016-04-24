#!/bin/bash
# ***** script to create a dummy USB driver to make Junsi iCHarger 4010 DUO USB adapter usable with MAC OS X ** 20 Apr 2016 WB *****
# execution: sudo createDummyUsbDriver.sh
cp -r iCharger4010Duo.kext /System/Library/Extensions/
cd /System/Library/Extensions
chown -R root:wheel iCharger4010Duo.kext
chmod -R 755 iCharger4010Duo.kext
kextcache -system-caches
echo iCharger4010Duo dummy driver installed please reboot the system before connecting iCharger4010Duo again.
