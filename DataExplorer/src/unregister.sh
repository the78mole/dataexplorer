#!/bin/bash
# ***** script to register OpenSerialDataExplorer mime type ** 22 Apr 2009 WB *****

#xdg-mime default /opt/OpenSerialDataExplorer/OpenSerialDataExplorer.desktop ""
#sudo rm -f /usr/bin/OpenSerialDataExplorer
xdg-desktop-menu uninstall --novendor /opt/OpenSerialDataExplorer/OpenSerialDataExplorer.directory /opt/OpenSerialDataExplorer/OpenSerialDataExplorer.desktop
xdg-icon-resource uninstall --context mimetypes --size 48 /opt/OpenSerialDataExplorer/OpenSerialDataExplorer.xpm application-x-OpenSerialData
xdg-mime uninstall --novendor /opt/OpenSerialDataExplorer/OpenSerialData-mime.xml
