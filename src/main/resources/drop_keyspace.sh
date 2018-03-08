#!/bin/bash -x
#
# Usage: drop_keyspace.sh keyspace [host] [port]
#

CMD="DROP KEYSPACE riot_keyspace$1;"

echo $CMD | cqlsh $2 $3
