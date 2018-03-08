#!/bin/bash -x

#
# deleteThingsByCondition.sh -
#
# EXAMPLE: deleteThingsByCondition.sh localhost localhost '{"zone.value.code":"Po1","status.value":"Sold"}'
#
#

# usage: deleteThingsByCondition.sh mongoQuery

#Get ids according query sent and delete things in mongo
#@Echo off
if [[ -z "$1" || -z "$2" || -z "$3" ]]; then
       	echo "Cannot run without parameters"
       	exit
fi

export IDS_COUNT=$(mongo $2/riot_main -u admin -p control123! --authenticationDatabase admin --quiet --eval "
    //get ids for deleting
    print(db.things.find($3).count());

    ")

echo "Are you sure you want to delete $IDS_COUNT things? (YES/NO), query= $3"
read answer
if echo "$answer" | grep -iq "^YES" ;then
    echo "*********************"
    echo "NOTE: DELETING THINGS"

    export IDS=$(mongo $2/riot_main -u admin -p control123! --authenticationDatabase admin --quiet --eval "
    //get ids for deleting
    var ids = db.things.find($3).map(function(x){return x._id;});

    //delete things from mongo according ids got before
    var printResult = [];
    db.thingSnapshotIds.remove({\"_id\" : {\$in : ids}});
    db.thingSnapshots.remove({\"value._id\" : {\$in : ids}});
    db.things.remove({\"_id\" : {\$in : ids}});
    ids.forEach(function(x){printResult.push(''+x)})
    print(printResult);
    ")

    #Delete things from mysql according ids got before
    mysql riot_main -h $1 -uroot -pcontrol123!  <  $4

    echo "Deleted thingIds "
else
    echo "Please be careful using this script."
fi

curl -X POST -H 'Content-type: application/json' --data '{"text":"listThingsByCondition was used in M&S. Please check if this run properly."}' https://hooks.slack.com/services/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
