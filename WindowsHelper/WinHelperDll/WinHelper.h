/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    GNU DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
   
    Copyright (c) 2008,2009,2010,2011 Winfried Bruegmann
****************************************************************************************/
/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class de_utils_WindowsHelper */

#ifndef _Included_gde_utils_WindowsHelper
#define _Included_gde_utils_WindowsHelper
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     de_utils_WindowsHelper
 * Method:    createDesktopLink
 * Signature: (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V
 */
JNIEXPORT jstring JNICALL Java_gde_utils_WindowsHelper_createDesktopLink
  (JNIEnv *, jclass, jstring, jstring, jstring, jstring, jstring, jint, jstring);

/*
 * Class:     de_utils_WindowsHelper
 * Method:    getFilePathFromLink
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gde_utils_WindowsHelper_getFilePathFromLink
  (JNIEnv *, jclass, jstring);

/*
 * Class:     de_utils_WindowsHelper
 * Method:    enumerateSerialPorts
 * Signature: (Ljava/util/Vector;)Ljava/util/Vector;
 */
JNIEXPORT jobjectArray JNICALL Java_gde_utils_WindowsHelper_enumerateSerialPorts
  (JNIEnv *, jclass);

/*
 * Class:     de_utils_WindowsHelper
 * Method:    findApplicationPath
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_gde_utils_WindowsHelper_findApplicationPath
  (JNIEnv *, jclass, jstring);

#ifdef __cplusplus
}
#endif
#endif
