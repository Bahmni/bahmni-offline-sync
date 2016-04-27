#!/bin/sh -x
PATH_OF_CURRENT_SCRIPT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
source $PATH_OF_CURRENT_SCRIPT/vagrant_functions.sh
PROJECT_BASE=$PATH_OF_CURRENT_SCRIPT/..
OMOD_LOCATION=/home/$USER/.OpenMRS/modules
USER=bahmni


mvn clean install

run_in_vagrant -f "$PATH_OF_CURRENT_SCRIPT/setup_environment.sh"

run_in_vagrant -f "$PATH_OF_CURRENT_SCRIPT/openmrs_stop.sh"

scp_to_vagrant $PROJECT_BASE/omod/target/bahmniOfflineSync-1.0-SNAPSHOT.omod /tmp/deploy_bahmni_offline_sync

run_in_vagrant -f "$PATH_OF_CURRENT_SCRIPT/deploy_omods.sh"

run_in_vagrant -f "$PATH_OF_CURRENT_SCRIPT/openmrs_start.sh"
