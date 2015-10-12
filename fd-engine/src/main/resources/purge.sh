#!/bin/bash

COUNTER=0

# This is an example script that will kick off the shell and run a batch command
# sub-opitmal from a linux POV, but it serves it's current function

# match (c:ZipCode) set c.key = "us."+c.code;
# match (c:County) set c.key = "us."+c.code;
# drop index on :Entity(code);

trap "exit" INT

    while [  $COUNTER -lt 10000 ]; do
        echo "RUNNING..."
        eval "JAVA_OPTS="-Xmx4G" neo4j-shell -path . -c 'match (e:Entity) where not has(e.code) with e limit 1000000 set e.key = e.callerKeyRef, e.code = e.code;'"
        let COUNTER=COUNTER+1
    done