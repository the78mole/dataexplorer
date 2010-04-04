#!/bin/bash
# ***** script to register DataExplorer mime type ** 22 Apr 2009 WB *****

#xdg-mime default DataExplorer.desktop ""
#sudo rm -f /usr/bin/DataExplorer
xdg-desktop-menu uninstall --novendor /tmp/DevicePropertiesEditor.directory DevicePropertiesEditor.desktop
xdg-desktop-menu uninstall --novendor /tmp/DataExplorer.directory DataExplorer.desktop
xdg-icon-resource uninstall --context mimetypes --size 48 DataExplorer.xpm application-x-OpenSerialData
xdg-mime uninstall --novendor OpenSerialData-mime.xml
