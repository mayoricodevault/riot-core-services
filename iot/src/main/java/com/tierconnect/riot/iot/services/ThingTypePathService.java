package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.QThingTypePath;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypePath;
import com.tierconnect.riot.iot.utils.DirectedGraph;
import com.tierconnect.riot.iot.utils.PermutationsOfN;
import org.apache.log4j.Logger;

import javax.annotation.Generated;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ThingTypePathService extends ThingTypePathServiceBase {

    private static Logger logger = Logger.getLogger(ThingTypePathService.class);

    public Map<String, String> getMapPathsByThingType(ThingType thingType) {
        BooleanBuilder builder = new BooleanBuilder();
        builder = builder.and(QThingTypePath.thingTypePath.destinyThingType.eq(thingType));
        List<ThingTypePath> thingTypePathList = getThingTypePathDAO().selectAllBy(builder);
        Map<String, String> paths = new HashMap<>(thingTypePathList.size());
        for (ThingTypePath typePath : thingTypePathList) {
            paths.put(typePath.getOriginThingType().getId() + "-" + typePath.getDestinyThingType().getId(),
                    typePath.getPath());
        }
        return paths;
    }

    public ThingTypePath getPathByThingTypes(ThingType thingTypeOrigin, Long thingTypeDestinyID) {
        return getPathByThingTypes(thingTypeOrigin.getId(), thingTypeDestinyID);
    }

    public ThingTypePath getPathByThingTypes(ThingType thingTypeOrigin, ThingType thingTypeDestiny) {
        return getPathByThingTypes(thingTypeOrigin.getId(), thingTypeDestiny.getId());
    }

    public ThingTypePath getPathByThingTypes(Long thingTypeOriginID, Long thingTypeDestinyID) {
        BooleanBuilder builder = new BooleanBuilder();
        builder = builder.and(QThingTypePath.thingTypePath.originThingType.id.eq(thingTypeOriginID));
        builder = builder.and(QThingTypePath.thingTypePath.destinyThingType.id.eq(thingTypeDestinyID));
        List<ThingTypePath> thingTypePathList = getThingTypePathDAO().selectAllBy(builder);
        if (!thingTypePathList.isEmpty()) {
            return thingTypePathList.get(0);
        }
        return null;
    }

    public List<ThingTypePath> getPathByThingTypeOrigin(ThingType thingTypeOrigin) {
        BooleanBuilder builder = new BooleanBuilder();
        builder = builder.and(QThingTypePath.thingTypePath.originThingType.eq(thingTypeOrigin));
        return getThingTypePathDAO().selectAllBy(builder);
    }

    public List<ThingTypePath> getPathByThingTypeDestiny(ThingType thingTypeDestiny) {
        BooleanBuilder builder = new BooleanBuilder();
        builder = builder.and(QThingTypePath.thingTypePath.destinyThingType.eq(thingTypeDestiny));
        return getThingTypePathDAO().selectAllBy(builder);
    }


    /**
     * Get Path By destiny
     *
     * @param thingTypeId      the thing type to get a path
     * @param thingTypeDesName A {@link String} containing the destiny thingType.
     * @return a instance of {@link List}<{@link String}> containing from thingType destiny to thingType origin.
     */
    public List<ThingTypePath> getPathByThingTypeId(Long thingTypeId, List<Long> groupIds) {
        return getPathByThingTypeId(thingTypeId, groupIds, ThingTypeService.getThingTypeDAO().getAllParents());
    }

    /**
     * Get Path By destiny
     *
     * @param thingTypeId      the thing type to get a path
     * @param thingTypeDesName A {@link String} containing the destiny thingType.
     * @param parents          A instance of {@link Long}[] that contains a list of parent thing types.
     * @return a instance of {@link List}<{@link String}> containing from thingType destiny to thingType origin.
     */
    public List<ThingTypePath> getPathByThingTypeId(Long thingTypeId, List<Long> groupIds, Long[] parents) {

        List<ThingTypePath> thingTypePaths = new LinkedList<>();

        //Get All paths
        List<ThingTypePath> paths = getThingTypePathDAO().getPaths();

        //Get recursively all paths for a thing type directed graph  view.
        DirectedGraph thingTypeGraph = ThingTypeService.getInstance().getThingTypeGraphByGroupId(groupIds);

        //Get all paths through parent nodes
        LinkedList<LinkedList<Long>> allPaths = thingTypeGraph.findAllPaths(parents);

        List<LinkedList<Long>> thingTypePathsList;
        if (thingTypeId != null) {
            thingTypePathsList = allPaths.stream()
                    .filter(p -> p.indexOf(thingTypeId) != -1)
                    .collect(Collectors.toList());
        } else {
            thingTypePathsList = allPaths;
        }

        PermutationsOfN<Long> permutationsOfN = new PermutationsOfN<>();
        for (int i = 0; i < thingTypePathsList.size(); i++) {
            List<List<Long>> lists = permutationsOfN.processSubsets(thingTypePathsList.get(i), 2);
            for (int j = 0; j < lists.size(); j++) {
                if (lists.get(j).size() == 2) {
                    Long v = lists.get(j).get(0);
                    Long w = lists.get(j).get(1);
                    ThingTypePath thingTypePath = paths.stream().filter(
                            path -> {
                                try {
                                    if (path.getPath() == null) {
                                        logger.warn("Error when comparing paths: " +
                                                "origin : thingType.id -> " + v +
                                                ", destiny: thingType.id -> " + w +
                                                " with table thingTypePath: Id-> " + path.getId() +
                                                ", the path field value is: " + path.getPath());
                                        return false;
                                    }
                                    return path.getOriginThingType().getId().equals(v) &&
                                            path.getDestinyThingType().getId().equals(w);
                                } catch (Exception ex) {
                                    logger.warn("Error when comparing paths:  " +
                                            "origin : thingType.id -> " + v +
                                            ", destiny: thingType.id -> " + w +
                                            " with  table thingTypePath: Id-> " + path.getId()
                                            + " .The sourceThingType_id or destinyThingType_id " +
                                            "column does not have a valid id.");
                                    return false;
                                }
                            }).findFirst().orElse(null);
                    if (thingTypePath != null && !thingTypePaths.contains(thingTypePath)) {
                        logger.debug(thingTypePath.getPath());
                        thingTypePaths.add(thingTypePath);
                    }
                }
            }
        }
        return thingTypePaths;
    }

}

