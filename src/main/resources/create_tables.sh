#!/bin/bash -x
#
# Usage: create_tables.sh keyspace [hostname] [port]
#

my_dir="$(dirname "$0")"

cqlsh -f $my_dir/cassandra_db_init.cql -k riot_keyspace$1 $2 $3
