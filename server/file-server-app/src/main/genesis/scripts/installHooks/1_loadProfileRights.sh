#!/bin/bash

# Source aliases and expand them
source "$GENESIS_HOME/genesis/util/setup.sh"
shopt -s expand_aliases

# Create dump folder
mkdir -p "$GENESIS_HOME/runtime/profileRights/dump"

# Dump table contents
cd "$GENESIS_HOME/runtime/profileRights/dump/"
DumpIt -t "PROFILE_RIGHT PROFILE RIGHT"

# Insert data
{
  SendIt -t "PROFILE_RIGHT" -f "$GENESIS_HOME/file-server/data/PROFILE_RIGHT.csv" -u "PROFILE_RIGHT_BY_RIGHT_CODE_PROFILE_NAME" &&
  SendIt -t "PROFILE" -f "$GENESIS_HOME/file-server/data/PROFILE.csv" -u "PROFILE_BY_NAME" &&
  SendIt -t "RIGHT" -f "$GENESIS_HOME/file-server/data/RIGHT.csv" -u "RIGHT_BY_CODE"
} ||
{
  # Exit code 2 will cause the script to log a delayed status and will run again after a remap
  exit 2
}

STATUS=$?
exit $STATUS
