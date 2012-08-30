#!/bin/sh
# Linux-only script to clear PostgreSQL and Linux caches,
# prior to running the db_benchmark.pl benchmarking script
#
# This script must be run as the 'root' user; e.g. via 'sudo'

# Before running this script, be sure to set the following
# value to reflect your current PostgreSQL version:
CURRENT_POSTGRESQL_VERSION=9.1

# -----

REQUIRED_OS_PLATFORM=Linux
REQUIRED_USER=root

OS_PLATFORM=`uname`
if [ "$OS_PLATFORM" != $REQUIRED_OS_PLATFORM ]
  then
    echo "This script must be run on a Linux system ..."
    exit 1
fi

EFFECTIVE_USER=`echo "$(whoami)"`
if [ $EFFECTIVE_USER != $REQUIRED_USER ]
  then
    echo "This script must be run as user $REQUIRED_USER"
    exit 1
fi

START_SCRIPT_DIR=/etc/init.d
POSTGRESQL_START_SCRIPT=$START_SCRIPT_DIR/postgresql-$CURRENT_POSTGRESQL_VERSION

if [ ! -x $POSTGRESQL_START_SCRIPT ]
  then
    echo "Could not find executable PostgreSQL startup script $POSTGRESQL_START_SCRIPT ..."
    exit 1
fi
echo "Stopping PostgreSQL server ..."
$POSTGRESQL_START_SCRIPT stop

SYNC_PATH=`which sync`
if [ "x$SYNC_PATH" == "x" ] 
  then
    echo "sync utility could not be found; could not proceed"
    exit 1
fi

if [ -f /proc/sys/vm/drop_caches ]
  then
    echo "Running sync to write all unsaved cache data to disk ..."
    sync
    echo "Displaying current cache stats ..."
    free -m
    echo "Dropping Linux disk caches ..."
    # See http://www.linuxinsight.com/proc_sys_vm_drop_caches.html
    # and the cautions at http://ubuntuforums.org/showthread.php?t=589975
    echo 3 > /proc/sys/vm/drop_caches
    echo "Displaying cache stats after dropping caches ..."
    free -m
  else
    echo "Could not find necessary file to drop Linux disk caches"
    exit 1
fi

echo "Restarting PostgreSQL server ..."
$POSTGRESQL_START_SCRIPT start
