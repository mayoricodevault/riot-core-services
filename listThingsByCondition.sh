#!/bin/bash -x

#
# listThingsByCondition.sh -
#
# EXAMPLE: listThingsByCondition.sh localhost localhost '{"zone.value.code":"Po1","status.value":"Sold"}' fileExternal
#
#

# usage: deleteThingsByCondition.sh mongoQuery

#Get ids according query sent and delete things in mongo
#@Echo off
if [[ -z "$1" || -z "$2" || -z "$3" || -z "$4" ]]; then
       	echo "Cannot run without parameters"
       	exit
fi


echo "Are you sure you want to store things in a file $4? (YES/NO), query= $3"
read answer
if echo "$answer" | grep -iq "^YES" ;then
    echo "*********************"
    echo "NOTE: DELETING THINGS"

    export IDS=$(mongo $2/riot_main -u admin -p control123! --authenticationDatabase admin --quiet --eval "
    //get ids for deleting
    var ids = db.things.find($3).map(function(x){return x._id;});

    //delete things from mongo according ids got before
    var printResult = [];
    ids.forEach(function(x){printResult.push(''+x)})
    print(printResult);
    ")

    #Delete things from mysql according ids got before

    echo "DELETE FROM apc_thing WHERE id in(${IDS})" > $4
else
    echo "Please be careful using this script."
fi

curl -X POST -H 'Content-type: application/json' --data '{"text":"listThingsByCondition was used in M&S. Please check if this run properly."}' https://hooks.slack.com/services/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
