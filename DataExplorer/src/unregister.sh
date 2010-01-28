#!/bin/bash
# ***** script to register OpenSerialDataExplorer mime type ** 22 Apr 2009 WB *****

#xdg-mime default OpenSerialDataExplorer.desktop ""
#sudo rm -f /usr/bin/OpenSerialDataExplorer
xdg-desktop-menu uninstall --novendor /tmp/DevicePropertiesEditor.directory DevicePropertiesEditor.desktop
xdg-desktop-menu uninstall --novendor /tmp/OpenSerialDataExplorer.directory OpenSerialDataExplorer.desktop
xdg-icon-resource uninstall --context mimetypes --size 48 OpenSerialDataExplorer.xpm application-x-OpenSerialData
xdg-mime uninstall --novendor OpenSerialData-mime.xml
