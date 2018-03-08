

db.getCollection('timeseries').drop()
db.getCollection('timeseries').insert( [
{
	"_id" : {
        "id" : NumberLong(1),
        "thingTypeFieldId" : NumberLong(1),
        "segment" : NumberLong(1)
    },
    "thingTypeId" : NumberLong(1),
    "groupId" : NumberLong(3),
    "name" : "001",
    "serialNumber" : "001",
    "fieldName" : "temp",
    "prevEnd" : NumberLong(0),
    "nextStart" : NumberLong(20),
    "time" : [ NumberLong("1"), NumberLong("4"), NumberLong("7"), NumberLong("10"), NumberLong("13"), NumberLong("16"), NumberLong("19"), NumberLong("22") ],
    "value" : [ 78, 79, 80, 81, 74, 72, 70, 73 ]
}
,{
	"_id" : {
        "id" : NumberLong(1),
        "thingTypeFieldId" : NumberLong(2),
        "segment" : NumberLong(1)
    },
    "thingTypeId" : NumberLong(1),
    "groupId" : NumberLong(3),
    "name" : "001",
    "serialNumber" : "001",
    "fieldName" : "city",
    "prevEnd" : NumberLong(0),
    "nextStart" : NumberLong(20),
    "time" : [ NumberLong("2"), NumberLong("11"), NumberLong("17") ],
    "value" : [ "Dallas", "Ft. Worth", "El Paso" ]
}
,{
	"_id" : {
        "id" : NumberLong(1),
        "thingTypeFieldId" : NumberLong(3),
        "segment" : NumberLong(1)
    },
    "thingTypeId" : NumberLong(1),
    "groupId" : NumberLong(3),
    "name" : "001",
    "serialNumber" : "001",
    "fieldName" : "status",
    "prevEnd" : NumberLong(0),
    "nextStart" : NumberLong(20),
    "time" : [ NumberLong("3"), NumberLong("6"), NumberLong("21") ],
    "value" : [ "ready_to_ship", "in_transit", "arrived" ]
}
,{
	"_id" : {
        "id" : NumberLong(2),
        "thingTypeFieldId" : NumberLong(1),
        "segment" : NumberLong(1)
    },
    "thingTypeId" : NumberLong(1),
    "groupId" : NumberLong(3),
    "name" : "002",
    "serialNumber" : "002",
    "fieldName" : "temp",
    "prevEnd" : NumberLong(0),
    "nextStart" : NumberLong(20),
    "time" : [ NumberLong("2"), NumberLong("5"), NumberLong("8"), NumberLong("11"), NumberLong("14"), NumberLong("17"), NumberLong("20"), NumberLong("23") ],
    "value" : [ 48, 50, 51, 52, 53, 57, 59, 60 ]
}
,{
	"_id" : {
        "id" : NumberLong(2),
        "thingTypeFieldId" : NumberLong(2),
        "segment" : NumberLong(1)
    },
    "thingTypeId" : NumberLong(1),
    "groupId" : NumberLong(3),
    "name" : "002",
    "serialNumber" : "002",
    "fieldName" : "city",
    "prevEnd" : NumberLong(0),
    "nextStart" : NumberLong(20),
    "time" : [ NumberLong("4"), NumberLong("9"), NumberLong("15") ],
    "value" : [ "Plymouth", "Ann Arbor", "Lansing" ]
}
,{
	"_id" : {
        "id" : NumberLong(2),
        "thingTypeFieldId" : NumberLong(3),
        "segment" : NumberLong(1)
    },
    "thingTypeId" : NumberLong(1),
    "groupId" : NumberLong(3),
    "name" : "002",
    "serialNumber" : "002",
    "fieldName" : "status",
    "prevEnd" : NumberLong(0),
    "nextStart" : NumberLong(20),
    "time" : [ NumberLong("1"), NumberLong("6"), NumberLong("8"), NumberLong("17") ],
    "value" : [ "ready_to_ship", "loading", "in_transit", "arrived" ]
}
]
)