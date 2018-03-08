#!/bin/bash

DIR=testReport
diff $DIR/data-$1-1.txt $DIR/data-$1-2.txt > $DIR/diff-$1.txt
