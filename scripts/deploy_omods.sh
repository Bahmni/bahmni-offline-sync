# This Source Code Form is subject to the terms of the Mozilla Public License,
# v. 2.0. If a copy of the MPL was not distributed with this file, You can
# obtain one at https://www.bahmni.org/license/mplv2hd.
#
# Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
# graphic logo is a trademark of OpenMRS Inc.

#!/bin/sh -x

TEMP_LOCATION=/tmp/deploy_bahmni_offline_sync
USER=bahmni
#USER=jss
OMOD_LOCATION=/home/$USER/.OpenMRS/modules

sudo rm -f $OMOD_LOCATION/bahmniOfflineSync*.omod

sudo su - $USER -c "cp -f $TEMP_LOCATION/* $OMOD_LOCATION"
