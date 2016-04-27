#!/bin/sh -x

TEMP_LOCATION=/tmp/deploy_bahmni_offline_sync
USER=bahmni
#USER=jss
OMOD_LOCATION=/home/$USER/.OpenMRS/modules

sudo rm -f $OMOD_LOCATION/bahmniOfflineSync*.omod

sudo su - $USER -c "cp -f $TEMP_LOCATION/* $OMOD_LOCATION"
