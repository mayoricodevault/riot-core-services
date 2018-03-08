#!/bin/bash -x
#
# Usage: create_keyspace.sh keyspace [hostname] [port]
#

echo "CREATE KEYSPACE riot_keyspace$1 WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };" | cqlsh $2 $3
