#!/bin/bash

if [ -z $1 ]; then
  echo "Supply destination user, host, and remote directory, e.g. pi@mypi.local:/path/to/remote/directory."
  exit 1
fi

./pinfeather-monitor-package.sh

scp pinfeather-monitor.tar.gz $1

rm -f pinfeather-monitor.tar.gz
