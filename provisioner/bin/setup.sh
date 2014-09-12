#!/usr/bin/env bash

# Configurable variables
TIMEOUT=3
export COOPR_SERVER_URI=${COOPR_SERVER_URI:-http://localhost:55054}
export COOPR_API_USER=${COOPR_API_USER:-admin}
export COOPR_API_KEY=${COOPR_API_KEY:-1234567890abcdef}
export COOPR_TENANT=${COOPR_TENANT:-superadmin}
export COOPR_HOME=${COOPR_HOME:-/opt/coopr}
export COOPR_RUBY=${COOPR_RUBY:-"${COOPR_HOME}/provisioner/embedded/bin/ruby"}

export COOPR_PROVISIONER_PLUGIN_DIR=${COOPR_HOME}/provisioner/worker/plugins

# TODO: steps
# - register
# - loop through upload scripts
# - sync

function wait_for_plugin_registration () {
  RETRIES=0
  until [[ $(curl --silent --request GET \
    --output /dev/null --write-out "%{http_code}" \
    --header "Coopr-UserID:${COOPR_API_USER}" \
    --header "Coopr-TenantID:${COOPR_TENANT}" \
    ${COOPR_SERVER_URI}/v2/plugins/automatortypes 2> /dev/null) -eq 200 || $RETRIES -gt 60 ]]; do
    sleep 2
    ((RETRIES++))
  done

  if [ $RETRIES -gt 60 ]; then
    echo "ERROR: Provisioner did not successfully register plugins"
    return 1
  fi
}

# Register provisioner
${COOPR_HOME}/provisioner/bin/provisioner.sh register
__ret=$?
[[ ${__ret} -ne 0 ]] && exit 1

wait_for_plugin_registration || exit 1

# Load plugin-bundled data
__skriptz=$(ls -1 ${COOPR_HOME}/provisioner/worker/plugins/*/*/load-bundled-data.sh 2>&1)
if [ "${__skriptz}" != "" ] ; then
  for __i in ${__skriptz} ; do
    ${__i}
    __ret=$?
    [[ ${__ret} -ne 0 ]] && exit 1
  done
else
  echo "Cannot find anything to execute"
  exit 1
fi

# Request sync
curl --silent --request POST \
  --header "Content-Type:application/json" \
  --header "Coopr-UserID:${COOPR_API_USER}" \
  --header "Coopr-ApiKey:${COOPR_API_KEY}" \
  --header "Coopr-TenantID:${COOPR_TENANT}" \
  --connect-timeout ${TIMEOUT} \
  ${COOPR_SERVER_URI}/v2/plugins/sync
__ret=$?
[[ ${__ret} -ne 0 ]] && exit 1
