/**************************************************************************************
  	This file is part of OpenSerialDataExplorer.

    OpenSerialDataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    OpenSerialDataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with OpenSerialDataExplorer.  If not, see <http://www.gnu.org/licenses/>.
****************************************************************************************/

#include <WinHelper.h>
#include <windowsx.h>
#include <objbase.h>
#include <shlobj.h>
#include <stdio.h>
#include <initguid.h>
#include <stdlib.h>
#include <io.h>

/*****************************************************************************************************************
WinHelper is a collection of windows native functions which are not accessible from Java 

build:
prepare the cygwin environment definitn e variable pointing to used JDK
export JDK=/cygdrive/c/IBM/SDP70/runtimes/base_v61/java

now call the compiler/linker to build the library (-lole32 defines the libole32.a lib; -mno-cygwin makes independent from cygwin1.dll,   -D__int64="long long" typedefs forJjava2C translation, see http://www.inonit.com/cygwin/jni/helloWorld/c.html)
gcc WinHelper.cpp -I. -I$JDK/include -I$JDK/include/win32 -lole32 -luuid -mno-cygwin -D__int8=char -D__int16=short -D__int32=int -D__int64="long long" -Wl,--add-stdcall-alias -shared -o WinHelper.dll

"C:\Program Files\Microsoft SDKs\Windows\v7.0\Bin\setenv.cmd" /XP /Release /x86
if not exist "XP32_RETAIL/" mkdir XP32_RETAIL
cl -c -DCRTAPI1=_cdecl -DCRTAPI2=_cdecl -nologo -GS -D_X86_=1  -DWIN32 -D_WIN32 -W3 -D_WINNT -D_WIN32_WINNT=0x0501 -DNTDDI_VERSION=0x05010000 -D_WIN32_IE=0x0600 -DWINVER=0x0501  -D_MT -D_DLL -MD -Zi -Od -DDEBUG /D "NDEBUG" /D "_WINDLL" /D "_AFXDLL" /D "_UNICODE" /D "UNICODE" /I. /Ic:\cygwin\home\winfried\java\include /Fo"XP32_RETAIL\\" /Fd"XP32_RETAIL\\" WinHelper.cpp
link /RELEASE  /INCREMENTAL:NO /NOLOGO -entry:_DllMainCRTStartup@12 -dll -out:XP32_RETAIL\WinHelper.dll XP32_RETAIL\WinHelper.obj ole32.lib uuid.lib
mt -manifest XP32_RETAIL\WinHelper.dll.manifest -outputresource:XP32_RETAIL\WinHelper.dll;2

"C:\Program Files\Microsoft SDKs\Windows\v7.0\Bin\setenv.cmd" /XP /Release /x64
if not exist "SRV2003_X64_RETAIL/" mkdir SRV2003_X64_RETAIL
cl -c -DCRTAPI1=_cdecl -DCRTAPI2=_cdecl -nologo -GS -D_AMD64_=1 -DWIN64 -D_WIN64  -DWIN32 -D_WIN32 -W4 -D_WINNT -D_WIN32_WINNT=0x0502 -DNTDDI_VERSION=0x05020000 -D_WIN32_IE=0x0600 -DWINVER=0x0502  -D_MT -D_DLL -MD -Zi -Od -DDEBUG /D "NDEBUG" /D "_WINDLL" /D "_AFXDLL" /D "_UNICODE" /D "UNICODE" /I. /Ic:\cygwin\home\winfried\java\include /Fo"SRV2003_X64_RETAIL\\" /Fd"SRV2003_X64_RETAIL\\" WinHelper.cpp
link /RELEASE  /INCREMENTAL:NO /NOLOGO -entry:_DllMainCRTStartup -dll -out:SRV2003_X64_RETAIL\WinHelper.dll SRV2003_X64_RETAIL\WinHelper.obj ole32.lib uuid.lib
mt -manifest SRV2003_X64_RETAIL\WinHelper.dll.manifest -outputresource:SRV2003_X64_RETAIL\WinHelper.dll;2


*****************************************************************************************************************/

/*****************************************************************************************************************
Methoid to create a windows shell link (extension lnk) using ole32 and uuid libs (hot-key specification and window type not enabled)
*****************************************************************************************************************/
JNIEXPORT jstring JNICALL Java_osde_utils_WindowsHelper_createDesktopLink
  (JNIEnv *env, jclass cl, jstring jfqShellLinkPath, jstring jfqExecutablePath, jstring jexecutableArguments, jstring jworkingDirectory, jstring jfqIconPath, jint iconPosition, jstring jdescription)
{	
	const char *fqShellLinkPath = env->GetStringUTFChars(jfqShellLinkPath, 0);
	const char *fqExecutablePath = env->GetStringUTFChars(jfqExecutablePath, 0);
	const char *executableArguments = env->GetStringUTFChars(jexecutableArguments, 0);
	const char *workingDirectory = env->GetStringUTFChars(jworkingDirectory, 0);
	const char *fqIconPath = env->GetStringUTFChars(jfqIconPath, 0);
	const char *description = env->GetStringUTFChars(jdescription, 0);
/*	
	printf("fqShellLinkPath = %s\n", fqShellLinkPath); 
	printf("fqExecutablePath = %s\n", fqExecutablePath); 
	printf("executableArguments = %s\n", executableArguments); 
	printf("workingDirectory = %s\n", workingDirectory); 
	printf("fqIconPath = %s\n", fqIconPath); 
	printf("description = %s\n", description); 
*/
    IShellLink *pShellLink;                            // pointer to IShellLink i/f
    HRESULT hres;
    WIN32_FIND_DATA wfd;
    char szGotPath[MAX_PATH];
	char szReturn[MAX_PATH];

    hres = CoInitialize(NULL);
    if (!SUCCEEDED(hres))
    {
        //printf("Could not open the COM library\n");
		env->ReleaseStringUTFChars(jfqShellLinkPath, fqShellLinkPath);
		env->ReleaseStringUTFChars(jfqExecutablePath, fqExecutablePath);
		env->ReleaseStringUTFChars(jexecutableArguments, executableArguments);
		env->ReleaseStringUTFChars(jworkingDirectory, workingDirectory);
		env->ReleaseStringUTFChars(jfqIconPath, fqIconPath);
		env->ReleaseStringUTFChars(jdescription, description);
        return env->NewStringUTF("OSDE_MSGE0045; Could not open the COM library");
    }


    // Get pointer to the IShellLink interface.
    hres = CoCreateInstance(CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER, IID_IShellLink, (LPVOID *)&pShellLink);
    if (SUCCEEDED(hres))
    {
        // Get pointer to the IPersistFile interface, if exist
        IPersistFile *pPersistFile;
        hres = pShellLink->QueryInterface(IID_IPersistFile, (LPVOID *)&pPersistFile);

        if (SUCCEEDED(hres))
        {
            WCHAR wszLinkPath[MAX_PATH];

            // Ensure string is Unicode.
            MultiByteToWideChar(CP_UTF8, 0, fqShellLinkPath, -1, wszLinkPath, MAX_PATH);
            //int MultiByteToWideChar(UINT, DWORD, const CHAR*, int, WCHAR*, int)

            // Load the shell link if exist
            //virtual HRESULT IPersistFile::Load(const WCHAR*, DWORD)
            //wprintf(L"pPersistFile->Load(%s, STGM_READ)\n", wszLinkPath);
            hres = pPersistFile->Load(wszLinkPath, STGM_READ);
            if (SUCCEEDED(hres))
            {
                // Resolve the link.
                hres = pShellLink->Resolve(0, SLR_ANY_MATCH);
                //                  ^
                // Using 0 instead -| of hWnd, as hWnd is only used if
                // interface needs to prompt for more information. Should use
                // hWnd from current console in the long run.

                if (SUCCEEDED(hres))
                {
                    strcpy_s(szGotPath, fqShellLinkPath);

                    hres = pShellLink->GetPath((LPWSTR)szGotPath, MAX_PATH,
                                               (WIN32_FIND_DATA *)&wfd, SLGP_UNCPRIORITY );
                                               
                    if (!SUCCEEDED(hres)) {
                        sprintf_s(&szReturn[0], MAX_PATH,"OSDE_MSGE0044; pShellLink->GetPath(%s) failed!\n", szGotPath);
                        //printf(szReturn);
                    }
                    else if (wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                        sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0043; \"%s\" is a directory!\n", szGotPath);
                        //printf(szReturn);
                    } 
                }
            }
            else  // file does not exist, it can be created
            {
				WCHAR wszExecutablePath[MAX_PATH];
				MultiByteToWideChar(CP_UTF8, 0, fqExecutablePath, -1, wszExecutablePath, MAX_PATH);
                pShellLink->SetPath((LPCWSTR)wszExecutablePath);  // Path to the object we are referring to

				WCHAR wszEcutableArguments[MAX_PATH];
				MultiByteToWideChar(CP_UTF8, 0, executableArguments, -1, wszEcutableArguments, MAX_PATH);
                pShellLink->SetArguments((LPCWSTR)wszEcutableArguments);
 
				WCHAR wszWorkingDirectory[MAX_PATH];
				MultiByteToWideChar(CP_UTF8, 0, workingDirectory, -1, wszWorkingDirectory, MAX_PATH);
				pShellLink->SetWorkingDirectory((LPCWSTR)wszWorkingDirectory);

				WCHAR wszIconPath[MAX_PATH];
				MultiByteToWideChar(CP_UTF8, 0, fqIconPath, -1, wszIconPath, MAX_PATH);
                pShellLink->SetIconLocation((LPCWSTR)wszIconPath, iconPosition); //   The address of a buffer to contain the path of the file containing the icon.

				WCHAR wszDescription[MAX_PATH];
				MultiByteToWideChar(CP_UTF8, 0, description, -1, wszDescription, MAX_PATH);
                pShellLink->SetDescription((LPCWSTR)wszDescription);

                MultiByteToWideChar(CP_UTF8, 0, fqShellLinkPath, -1, wszLinkPath, MAX_PATH);
                pPersistFile->Save(wszLinkPath, TRUE);
                //wprintf(L"wszLinkPath = %s\n", wszLinkPath);
            }
            pPersistFile->Release();
        }
        else 
        {
            sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0041; QueryInterface Error\n");
            //printf(szReturn);
        }
        pShellLink->Release();
    }
    else 
    {
        sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0040; CoCreateInstance Error - hres = %08x\n", hres);
        //printf(szReturn);
    }

	env->ReleaseStringUTFChars(jfqShellLinkPath, fqShellLinkPath);
	env->ReleaseStringUTFChars(jfqExecutablePath, fqExecutablePath);
	env->ReleaseStringUTFChars(jexecutableArguments, executableArguments);
	env->ReleaseStringUTFChars(jworkingDirectory, workingDirectory);
	env->ReleaseStringUTFChars(jfqIconPath, fqIconPath);
	env->ReleaseStringUTFChars(jdescription, description);
	return env->NewStringUTF(szReturn);
}

/*****************************************************************************************************************
Methoid to return the contained full qualified file path from a windows shell link (extension lnk) using ole32 and uuid libs
*****************************************************************************************************************/
JNIEXPORT jstring JNICALL Java_osde_utils_WindowsHelper_getFilePathFromLink
  (JNIEnv *env, jclass cl, jstring jfqShellLinkPath)
{	
	const char *fqShellLinkPath = env->GetStringUTFChars(jfqShellLinkPath, 0);
	char szReturn[MAX_PATH];
    IShellLink *pShellLink;                            // pointer to IShellLink i/f
    HRESULT hres;
    WIN32_FIND_DATA wfd;
    char szGotPath[MAX_PATH];
	
	//printf("fqShellLinkPath = %s\n", fqShellLinkPath); 

    hres = CoInitialize(NULL);
    if (!SUCCEEDED(hres))
    {
        //printf("OSDE_MSGE000x; Could not open the COM library\n");
        env->ReleaseStringUTFChars(jfqShellLinkPath, fqShellLinkPath);
        return env->NewStringUTF("OSDE_MSGE0045; Could not open the COM library");
    }


    // Get pointer to the IShellLink interface.
    hres = CoCreateInstance(CLSID_ShellLink, NULL, CLSCTX_INPROC_SERVER, IID_IShellLink, (LPVOID *)&pShellLink);
    if (SUCCEEDED(hres))
    {
        // Get pointer to the IPersistFile interface, if exist
        IPersistFile *pPersistFile;
        hres = pShellLink->QueryInterface(IID_IPersistFile, (LPVOID *)&pPersistFile);

        if (SUCCEEDED(hres))
        {
            WCHAR wszLinkPath[MAX_PATH];

            // Ensure string is Unicode.
            MultiByteToWideChar(CP_UTF8, 0, fqShellLinkPath, -1, wszLinkPath, MAX_PATH);
            //int MultiByteToWideChar(UINT, DWORD, const CHAR*, int, WCHAR*, int)

            // Load the shell link if exist
            //virtual HRESULT IPersistFile::Load(const WCHAR*, DWORD)
            //wprintf(L"pPersistFile->Load(%s, STGM_READ)\n", wszLinkPath);
            hres = pPersistFile->Load(wszLinkPath, STGM_READ);
            if (SUCCEEDED(hres))
            {
                // Resolve the link.
                hres = pShellLink->Resolve(0, SLR_ANY_MATCH);
                //                  ^
                // Using 0 instead -| of hWnd, as hWnd is only used if
                // interface needs to prompt for more information. Should use
                // hWnd from current console in the long run.

                if (SUCCEEDED(hres))
                {
                    strcpy_s(szGotPath, fqShellLinkPath);

                    hres = pShellLink->GetPath((LPWSTR)szGotPath, MAX_PATH,
                                               (WIN32_FIND_DATA *)&wfd, SLGP_UNCPRIORITY );
                    if (!SUCCEEDED(hres)) {
                        sprintf_s(&szReturn[0], MAX_PATH,"OSDE_MSGE0044; pShellLink->GetPath(%s) failed!\n", szGotPath);
                        //printf(szReturn);
                    }
                    else if (wfd.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) {
                        sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0043; \"%s\" is a directory!\n", szGotPath);
                        //printf(szReturn);
                    } 

                    //wprintf(L"link contained file path = %s\n", szGotPath);
                    int utf8_length = WideCharToMultiByte(CP_UTF8, 0, (LPCWSTR)szGotPath, -1, szReturn, (sizeof(szReturn) - sizeof(char)), NULL, NULL);
                    //printf("link contained file path = %s\n", szReturn);
 
					if (utf8_length == 0) {
                        sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0046; WideCharToMultiByte failed!\n");
                        //printf(szReturn);
					}
                    
                }
            }
            else // file does not exist
            {
                sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0042; IPersistFile Load Error\n");
                //printf(szReturn);
            }
            pPersistFile->Release();
        }
        else {
            sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0041; QueryInterface Error\n");
            //printf(szReturn);
        }
        pShellLink->Release();
    }
    else {
        sprintf_s(&szReturn[0], MAX_PATH, "OSDE_MSGE0040; CoCreateInstance Error - hres = %08x\n", hres);
        //printf(szReturn);
    }

	env->ReleaseStringUTFChars(jfqShellLinkPath, fqShellLinkPath);
	return env->NewStringUTF(szReturn);
}