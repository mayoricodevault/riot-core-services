#!/bin/bash -x
#
# Usage: set_field_value_history_GC [hostname]
#

CMD="ALTER TABLE field_value_history WITH GC_GRACE_SECONDS = 1;"

cqlsh -e "$CMD" $1
