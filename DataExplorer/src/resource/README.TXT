                DataExplorer Object Directories
                -------------------------------


APPLICABILITY

	History analysis module; log file source directories; object keys; native log
	files 
	    
                
OVERVIEW

	The DataExplorer supports object keys which are available in the menu tool bar.
	By using object keys you can access OSD log files quickly. Each object key 
	corresponds to an object folder placed in the working directory ('Settings > 
	Data path'). 
	
	Most users hold the native log files (e.g. *.bin) in other folders.
	  
	This document gives you the information to integrate native log files kept in 
	arbitrary directories, external drives or network drives.
	 
	
DETAILS

	A)	Standard object directories
	 
	Native log files might also be placed in the data path and so become accessible
	by the history functions.
		<workingFolder>
		|-> <objectFolder1>
		|-> <objectFolder2>
		
		
	B)	Data path supplementary object directories

	For a better overview it might be advisable to hold the log files in a folder 
	hierarchy like this:
		<workingFolder>
		|-> _SupplementObjectDirs
		|	|-> <objectFolder1>
		|	|-> <objectFolder2>
		|-> <objectFolder1>
		|-> <objectFolder2>
	
 	Or alternatively:
		<workingFolder>
		|-> _SupplementObjectDirs
		|	|-> 2018
		|	|	|-> <objectFolder1>
		|	|	|-> <objectFolder2>
		|	|-> 2019
		|		|-> <objectFolder1>
		|		|-> <objectFolderN>
		|-> <objectFolder1>
		|-> <objectFolder2>
 	
 
	C)	External object directories

	The DataExplorer properties file ('DataExplorer.properties') supports entries 
	for including additional log file paths in the history analysis.
	
	Example:
	
		data_folders_csv      =	 E:/ObjectDirs
		import_folders_csv    =	 F:/Logs/ImportObjectDirs,//SERVER01/Logs
	
	Please note the slash for universal directory paths (URI).
	
	The import directories are scanned if the current device supports imports. Only 
	files with valid import extensions become part of the history.  


REMARKS

 	* In any case you have to create the objects in the DataExplorer menu tool bar.
 	  Be aware of the wizard: 'Settings > Maintenance > Create objects'
 	  It searches the data path (working directory) for populated object 
 	  directories and creates missing object keys after your approval.
 	* The folder hierarchy in the _SupplementObjectDirs folder resp. the properties 
 	  folders is free to choose. The only requirement is that the object folder 
 	  names are identical to the DataExplorer object keys.
	* Deleting object keys in the menu tool bar will not affect the supplementary
	  folder or external object folders.
	* For devices which support imports consider these settings:
		-- Search the working directories for imports via
		   'Settings > History > Screening > Include imports from data path'
		-- Search import folders via 
		   'Settings > History > Screening > Search import path' 
    * The default import folder is the last used import path (modified with the
      current object key). This might spare an 'import_folders_csv' entry.
	
	
WEB SITE

	http://www.nongnu.org/dataexplorer/index.de.html
	

NOTICE 

  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.

    Copyright (c) 2018 Thomas Eickert

 