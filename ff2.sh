#!/bin/bash


gradle --daemon clean dist
#./popdb.sh gradle-default.properties MojixRetail
./popdb-more.sh gradle-default.properties ReportTest 20

