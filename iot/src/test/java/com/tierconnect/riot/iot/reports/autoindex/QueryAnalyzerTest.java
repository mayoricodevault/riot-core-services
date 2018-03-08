package com.tierconnect.riot.iot.reports.autoindex;

import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by julio.rocha on 26-04-17.
 */
public class QueryAnalyzerTest {

    public static final String SIMPLE_QUERY = "{\"$and\":[{\"thingType\" : 1}, {\"group\" : {\"$in\" : [1,2,3,4,5,6,7]}}]}";
    public static final String ELEM_MATCH_SIMPLE_QUERY = "{ \"results\": { \"$elemMatch\": { \"$gte\": 80, \"$lt\": 85 } } }";
    public static final String THIS_QUERY = "{\"$and\":[{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}";
    public static final String EQUALITY_QUERY = "{\"$and\":[{\"$or\":[{\"children\":{\"$exists\":false}},{\"children\":null},{\"children\":{\"$size\":0}}]},{\"parent\":{\"$exists\":false}},{\"thingTypeId\":1},{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}";
    public static final String COMPLEX_QUERY_WITH_SUBQUERY = "{\"$and\":[{\"value.thingType\" : 1}, {\"_id\":{\"$in\":db.thingSnapshotIds.find({\"$and\":[{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1469973724000,\"$lte\":1486648926999}}}}]},{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1469973724000,\"$lte\":1486648926999}}}}).map(function(_paramToProject){ return _paramToProject.blinks[0].blink_id})}}]}";
    public static final String COMPLEX_QUERY_MATCH_WITH_SUBQUERY_AND_SPACES = "[{\"$match\":{\"$and\":[{\"value.groupId\":{\"$in\":[1,2,3,4,5,6]}}\n" +
            ",{\"value.thingTypeId\":{\"$in\":[17,19]}}\n" +
            ",{\"_id\":{\"$in\":db.thingSnapshotIds.find({\"$and\":[{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1501560000000,\"$lte\":1502900343999}}}}\n" +
            "]},{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1501560000000,\"$lte\":1502900343999}}}}\n" +
            ").map(function(_paramToProject){ return _paramToProject.blinks[0].blink_id})}},{\"value.status\":{\"$exists\":true}}\n" +
            ",{\"value.zone\":{\"$exists\":true}}\n" +
            "]}},{\"$group\":{\"_id\":{\"status,x\":\"$value.status.value\",\"zone,y\":\"$value.zone.value.name\"},\"count\":{\"$sum\":1}}},{ \"$limit\" : 1000000 }]";
    public static final String COMPLEX_QUERY = "{\"$and\":[{\"value.thingTypeId\":4},{\"value.primary.value\":true},{\"value.type.value\":\"personnel\"},{\"value.groupId\":3},{\"value.thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25]}},{\"time\":{\"$lte\":ISODate(2017-04-28T16:12:41.083-04:00)}}]}";
    public static final String AGGREGATION_QUERY = "[{\"$match\":{\"$and\":[{\"groupId\":{\"$in\":[1234567]}}{\"thingTypeId\":1}]}}{\"$group\":{\"_id\":{\"zoneCodenamex\":\"$zone.value.code\"\"zoneLocalMapidy\":\"$zone.value.facilityMap\"}\"count\":{\"$sum\":1}}}{ \"$limit\" : 1000000 }]";
    public static final String COMPLEX_AGGREGATION_QUERY = "[{\"$match\":{\"$and\":[{\"value.groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"value.thingTypeId\":1},{\"_id\":{\"$in\":db.thingSnapshotIds.find({\"$and\":[{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1483243200639,\"$lte\":1493922882639}}}}]},{\"blinks\":{\"$elemMatch\":{\"time\":{\"$gte\":1483243200639,\"$lte\":1493922882639}}}}).map(function(_paramToProject){ return _paramToProject.blinks[0].blink_id})}}]}},{\"$group\":{\"_id\":{\"zoneCodename,x\":\"$value.zone.value.code\",\"zoneLocalMapid,y\":\"$value.zone.value.facilityMap\"},\"count\":{\"$sum\":1}}},{ \"$limit\" : 1000000 }]";


    @Test
    public void testSimpleQuery() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                SIMPLE_QUERY,
                "serialNumber"
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}], rangeFields=[QueryField{fieldName='group', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[QueryField{fieldName='thingType', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testElemMatchSimpleQuery() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                ELEM_MATCH_SIMPLE_QUERY,
                "serialNumber"
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}], rangeFields=[QueryField{fieldName='results', fieldType=RANGE, operator='$gte', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testThisQuery() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                THIS_QUERY,
                "serialNumber"
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}], rangeFields=[QueryField{fieldName='thingTypeId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testEqualityQuery() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                EQUALITY_QUERY,
                "serialNumber"
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}], rangeFields=[QueryField{fieldName='children', fieldType=RANGE, operator='$exists', cardinality=0, dataType='null', nestedLevel=3}, QueryField{fieldName='thingTypeId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='parent', fieldType=RANGE, operator='$exists', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[QueryField{fieldName='children', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='thingTypeId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testComplexQueryWithSubquery() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_QUERY_WITH_SUBQUERY,
                "serialNumber"
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}], rangeFields=[QueryField{fieldName='_id', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[QueryField{fieldName='value.thingType', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testComplexQueryWithSubqueryAndSpaces() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_QUERY_MATCH_WITH_SUBQUERY_AND_SPACES,
                "serialNumber"
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', " +
                        "cardinality=0, dataType='null', nestedLevel=0}], rangeFields=[QueryField{fieldName='v" +
                        "alue.thingTypeId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', " +
                        "nestedLevel=2}, QueryField{fieldName='_id', fieldType=RANGE, operator='$in', " +
                        "cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='value.status', " +
                        "fieldType=RANGE, operator='$exists', cardinality=0, dataType='null', nestedLevel=2}, " +
                        "QueryField{fieldName='value.zone', fieldType=RANGE, operator='$exists', cardinality=0, " +
                        "dataType='null', nestedLevel=2}, QueryField{fieldName='value.groupId', fieldType=RANGE, " +
                        "operator='$in', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testSimpleQueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                SIMPLE_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='group', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='thingType', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}, QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testElemMatchSimpleQueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                ELEM_MATCH_SIMPLE_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='results', fieldType=RANGE, operator='$gte', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testThisQueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                THIS_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='thingTypeId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testEqualityQueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                EQUALITY_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='children', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='thingTypeId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}, QueryField{fieldName='groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='parent', fieldType=RANGE, operator='$exists', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testComplexQueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_QUERY,
                "time"
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='value.thingTypeId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}, QueryField{fieldName='value.primary.value', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}, QueryField{fieldName='value.type.value', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}, QueryField{fieldName='value.groupId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}, QueryField{fieldName='time', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testComplexQueryWithSubqueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_QUERY_WITH_SUBQUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='_id', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='value.thingType', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}, QueryField{fieldName='serialNumber', fieldType=SORT, operator='sort', cardinality=0, dataType='null', nestedLevel=0}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testSimpleQueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                SIMPLE_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"serialNumber\":{\"dataType\":null,\"cardinality\":0},\"thingType\":{\"dataType\":null,\"cardinality\":0},\"group\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testElemMatchSimpleQueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                ELEM_MATCH_SIMPLE_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"serialNumber\":{\"dataType\":null,\"cardinality\":0},\"results\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testThisQueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                THIS_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"thingTypeId\":{\"dataType\":null,\"cardinality\":0},\"serialNumber\":{\"dataType\":null,\"cardinality\":0},\"groupId\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testEqualityQueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                EQUALITY_QUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"parent\":{\"dataType\":null,\"cardinality\":0},\"thingTypeId\":{\"dataType\":null,\"cardinality\":0},\"serialNumber\":{\"dataType\":null,\"cardinality\":0},\"children\":{\"dataType\":null,\"cardinality\":0},\"groupId\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testComplexQueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_QUERY,
                "time"
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"value.primary.value\":{\"dataType\":null,\"cardinality\":0},\"value.thingTypeId\":{\"dataType\":null,\"cardinality\":0},\"time\":{\"dataType\":null,\"cardinality\":0},\"value.groupId\":{\"dataType\":null,\"cardinality\":0},\"value.type.value\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testComplexQueryWithSubqueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_QUERY_WITH_SUBQUERY,
                "serialNumber"
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"serialNumber\":{\"dataType\":null,\"cardinality\":0},\"value.thingType\":{\"dataType\":null,\"cardinality\":0},\"_id\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testSimpleAggretationQuery() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                AGGREGATION_QUERY,
                null
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[], rangeFields=[QueryField{fieldName='groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[QueryField{fieldName='thingTypeId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testSimpleAggretationQueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                AGGREGATION_QUERY,
                null
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='thingTypeId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testSimpleAggretationQueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                AGGREGATION_QUERY,
                null
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"thingTypeId\":{\"dataType\":null,\"cardinality\":0},\"groupId\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testComplexAggretationQuery() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_AGGREGATION_QUERY,
                null
        );
        queryAnalyzer.extractAndClassificate();
        assertEquals(
                "{sortFields=[], rangeFields=[QueryField{fieldName='_id', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='value.groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}], exactFields=[QueryField{fieldName='value.thingTypeId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}]}",
                queryAnalyzer.getFieldsForIndex().toString()
        );
    }

    @Test
    public void testComplexAggretationQueryMergedFields() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_AGGREGATION_QUERY,
                null
        );
        queryAnalyzer.execute();
        assertEquals(
                "[QueryField{fieldName='_id', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='value.groupId', fieldType=RANGE, operator='$in', cardinality=0, dataType='null', nestedLevel=2}, QueryField{fieldName='value.thingTypeId', fieldType=EQUALITY, operator='$eq', cardinality=0, dataType='null', nestedLevel=1}]",
                queryAnalyzer.getMergedFields().toString()
        );
    }

    @Test
    public void testComplexAggretationQueryWithParametersToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                COMPLEX_AGGREGATION_QUERY,
                null
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"value.thingTypeId\":{\"dataType\":null,\"cardinality\":0},\"_id\":{\"dataType\":null,\"cardinality\":0},\"value.groupId\":{\"dataType\":null,\"cardinality\":0}}",
                queryAnalyzer.getJsonFields().toString()
        );
    }

    @Test
    public void testProjectionParametersWithJustParentToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                "[{\"$match\":{\"$and\":[{\"TheftCandidate.value\":true},{\"groupId\":{\"$in\":[1,4,5,6,7,8]}},{\"thingTypeId\":3},{\"UPCCode\":{\"$exists\":true}},{\"TheftCandidate\":{\"$exists\":true}}]}},{\"$group\":{\"_id\":{\"UPCCode,x\":\"$UPCCode.value\",\"TheftCandidate,y\":\"$TheftCandidate.value\"},\"count\":{\"$sum\":1}}},{ \"$limit\" : 1000000 }]",
                null
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"UPCCode\":1,\"thingTypeId\":1,\"TheftCandidate\":1,\"groupId\":1}",
                queryAnalyzer.getJsonProjection().toString()
        );
    }

    @Test
    public void testProjectionParametersWithJustParentSnapshotsToMongo() throws ParseException {
        QueryAnalyzer queryAnalyzer = new QueryAnalyzer(
                "[{\"$match\":{\"$and\":[{\"value.TheftCandidate.value\":true},{\"value.groupId\":{\"$in\":[1,4,5,6,7,8]}},{\"value.thingTypeId\":3},{\"value.UPCCode\":{\"$exists\":true}},{\"value.TheftCandidate\":{\"$exists\":true}}]}},{\"$group\":{\"_id\":{\"UPCCode,x\":\"$value.UPCCode.value\",\"TheftCandidate,y\":\"$value.TheftCandidate.value\"},\"count\":{\"$sum\":1}}},{ \"$limit\" : 1000000 }]",
                null
        );
        queryAnalyzer.execute();
        assertEquals(
                "{\"value.TheftCandidate\":1,\"value.UPCCode\":1,\"value.thingTypeId\":1,\"value.groupId\":1}",
                queryAnalyzer.getJsonProjection().toString()
        );
    }
}