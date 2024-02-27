@echo off
title **** Compile Website ****
rem   ****    winXP cmd batch file    ****
rem   ****  2009 - Matteo Gattanini   ****
rem @if not "%OS%"=="Windows_NT" goto :EXIT

rem   **** Dependencies ****
rem XSL transformation: call Transform.bat *.xml *.xsl outputfile

rem   **** Internal arguments ****
set bindir=bin
set outdir=html
set outext=html
set imgdir=common/images
set mainxml=main.xml
set sitemapfile=sitemap
REM   **** used programs ****
:: dot, neato, twopi, circo, fdp
set graphviz="%programfiles%\Graphviz2.22\bin\twopi.exe"
goto CREATESITEMAP

echo asll:fff
echo afeag: gag
echo wegfawg : gawgag
echo wrgerges  :gegaeg
echo rgaawg::gerg :rger : rgaewrg: gearg

abra

  :CREATESITEMAP
call Transform.bat %mainxml% site2graphviz.xsl %sitemapfile% "outext=%outext%" "imgdir=%imgdir%"
if errorlevel 1 goto ERROR

set prevcd=%cd%
cd %outdir%\
echo moved to: here %cd%
echo %graphviz% -Tsvg -Tpng -O %sitemapfile%
%graphviz% -Tsvg -Tpng -O %sitemapfile%
if errorlevel 1 goto ERROR
echo ...Ok
cd %prevcd%
echo returned to %cd%
goto TRANSFORMSITE

:TRANSFORMSITE
call Transform.bat %mainxml% site2htmlxpdesktop.xsl main.%outext% "outext=%outext%" "imgdir=%imgdir%"
if errorlevel 1 goto ERROR
goto OPTIMIZEHTML

:OPTIMIZEHTML
echo.
echo **** Optimizing html files...
%bindir%\ahc "%outdir%\" -m"*.htm; *.html" -y+ -l+ -w+ -q- -t- -c- -d- -m- -s- -b-
if errorlevel 1 goto ERROR
echo ...Ok
goto CREATERSS

:CREATERSS
call Transform.bat %mainxml% site2rss.xsl rssnews.xml "outfile=rssnews.xml" "outext=%outext%"
if errorlevel 1 goto ERROR
goto CREATESITEMAPFILE

:CREATESITEMAPFILE
call Transform.bat %mainxml% site2sitemap.xsl sitemap.xml "outext=%outext%"
if errorlevel 1 goto ERROR
goto END

:ERROR
echo (!) Something was wrong, check and retry!
rem sendmail.exe -t < error_mail.txt
pause
exit

:END
rem   ****      Nothing more to do      ****
echo.
echo ...All done
exit
rem   ****        End of file           ****
