#!/bin/bash
# ***** script to register OpenSerialDataExplorer mime type ** 22 Apr 2009 WB *****

xdg-mime install --novendor OpenSerialData-mime.xml
xdg-icon-resource install --context mimetypes --size 48 OpenSerialDataExplorer.xpm application-x-OpenSerialData
xdg-desktop-menu install --novendor OpenSerialDataExplorer.directory OpenSerialDataExplorer.desktop
xdg-mime default OpenSerialDataExplorer.desktop application/x-OpenSerialData
