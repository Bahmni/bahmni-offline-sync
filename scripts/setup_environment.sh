#!/bin/sh -x

TEMP_LOCATION=/tmp/deploy_bahmni_offline_sync

if [[ ! -d $TEMP_LOCATION ]]; then
   mkdir $TEMP_LOCATION
fi

rm -rf $TEMP_LOCATION/*