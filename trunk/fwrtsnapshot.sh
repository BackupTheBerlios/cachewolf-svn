#!/bin/sh
# $Id$

set -x
v=$(svn info | sed -n '/Revision: /s///p')
printf '/VER_SVN =/s/\$.*\$/$LastChangedRevision: %s $/\nwq\n' $v | \
    ed -s src/CacheWolf/Version.java
javac -target 1.1 -encoding windows-1252 -cp ./lib/CompileEwe.zip:./lib/  -d ./bin/ -deprecation -nowarn ./src/CacheWolf/*.java ./src/CacheWolf/*/*.java ./src/exp/*.java ./src/utils/*.java
/usr/local/bin/ewecl programs/Jewel.ewe -c cwberlios.jnf
rm -rf published
mkdir -p published/dat/attributes
mv programs/CacheWolf/* published/
chmod 755 published/*
chmod 644 published/*/*
chmod 755 published/dat/attributes
printf '1,$g/ 12M/s///\nwq\n' | ed -s published/Jar/CacheWolf.bat
install -c -m 644 work/CacheWolf.ewe published/
install -c -m 644 res_noewe/* published/dat/
install -c -m 644 resources/attributes-big/*.gif published/dat/attributes/
(cd published/dat && find * -type f | sort | \
    /usr/local/bin/cpio -oC512 -Hustar -Mdist >../datfiles.tar)
rm -rf published/dat
chmod 644 published/datfiles.tar
mkdir -p ~/public_html/CacheWolf-BE/r$v
mv published/* ~/public_html/CacheWolf-BE/r$v/
rm -rf bin published
