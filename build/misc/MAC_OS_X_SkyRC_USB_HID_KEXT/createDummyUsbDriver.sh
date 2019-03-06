#!/bin/bash
# ***** script to install kernel extension to make SkyRC USB-HID adapter usable with MAC OS X ** 06 Mar 2019 WB *****
# execution: sudo ./createDummyUsbDriver.sh
cp -r SkyRC.kext /System/Library/Extensions/
cd /System/Library/Extensions
chown -R root:wheel SkyRC.kext
chmod -R 755 SkyRC.kext
kextcache -system-caches
echo Prior SkyRC USB kernel extension installed please reboot the system before connecting a SKyRC USB device again.
echo MAC OS X >= 10.13 High Sierra kernel extensions might be blocked, reboot into recovery console "(push cmd+R while boot)" open terminal
echo enter "spctl kext-consent disable" and "csrutil disable", reboot to activate
echo open terminal enter "sudo kextutil /System/Library/Extensions/SkyRC.kext" before USB cable connected. This will load non signed kext
echo Alternative
echo Kext Utiliy may help installing kernel extension, search internet for download link
echo Unfortunately, "kext-dev-mode=1" and "rootless=0" these keys are no longer working for future versions of Mac OS X starting with El Capitan GM. 
echo You need disable SIP with Clover Bootloader.
echo To run Kext Utility, you need to run in terminal this command "sudo spctl --master-disable"
echo After starting Kext Utility just drag and drop the kernel extension complete directory tree to let it install
