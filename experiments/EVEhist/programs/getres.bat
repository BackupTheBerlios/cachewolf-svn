@echo off
rem make the working dir the one this file is in
cd %~d0%~p0
mkdir ..\work
mkdir ..\work\attributes
mkdir ..\work\attributes-big
copy ..\resources\*.gif ..\work
copy ..\resources\*.png ..\work
mkdir ..\work\languages
mkdir ..\work\webmapservices
copy ..\res_noewe\languages\*.cfg ..\work\languages
copy ..\res_noewe\webmapservices\*.wms ..\work\webmapservices
copy ..\res_noewe\webmapservices\*.txt ..\work\webmapservices
copy ..\res_noewe\attributes\*.gif ..\work\attributes
copy ..\res_noewe\attributes-big\*.gif ..\work\attributes-big
copy ..\res_noewe\*.def ..\work
copy ..\res_noewe\*.html ..\work
copy ..\res_noewe\*.png ..\work
copy ..\res_noewe\*.gif ..\work
copy ..\res_noewe\*.tpl ..\work
copy ..\res_noewe\*.zip ..\work
pause