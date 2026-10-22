#!/bin/sh

if [ $1 -ge 1 ]; then
    ## create certificates
    if ! [ -d /home/installr/.installrd/tls ]; then
        su installr -c '/opt/installr-io/bin/installrd-certs init' >/dev/null
    fi
fi
