#!/bin/sh


for f in config.yaml installrd.mv.db; do
    if [ -f /home/installr/.installrd/${f}.rpmsave ]; then
        mv /home/installr/.installrd/${f}.rpmsave /home/installr/.installrd/${f}
    fi
done
systemctl try-restart 'installrd.service' >/dev/null 2>&1 || :
