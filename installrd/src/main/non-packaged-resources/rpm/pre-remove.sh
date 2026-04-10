#!/bin/sh

if [ $1 -eq 0 ] ; then 
    # Package removal, not upgrade 
    systemctl --no-reload disable 'installrd.service' > /dev/null 2>&1 || : 
    systemctl stop 'installrd.service' > /dev/null 2>&1 || : 
fi
