@ECHO OFF
:: Usage: set_field_value_history_GC.bat [hostname]

echo "Executing set_field_value_history_GC.bat"

SET CMD="ALTER TABLE field_value_history WITH GC_GRACE_SECONDS = 1;"

cqlsh %1 -k riot_main -e %CMD%
