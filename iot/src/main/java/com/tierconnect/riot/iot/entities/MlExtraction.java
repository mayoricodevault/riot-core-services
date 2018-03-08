package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import org.apache.log4j.Logger;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Entity

@Table(name="ml_extraction")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlExtraction extends MlExtractionBase
{

    static Logger logger = Logger.getLogger(MlExtraction.class);
    private static String FINISHED_STATUS = "FINISHED";
    private static List<MlExtractionPredictor> defaultPredictors;

    static {
        defaultPredictors = new ArrayList<>();
        defaultPredictors.add(new MlExtractionPredictor("time", "time"));
        defaultPredictors.add(new MlExtractionPredictor("serial", "value.serialNumber"));
    }
    //default contructor for hibernate
    public MlExtraction() {

    }

    public MlExtraction(MlBusinessModel businessModel,
                        Group group,
                        String name,
                        String comments,
                        LocalDate startDate,
                        LocalDate endDate,
                        List<MlExtractionPredictor> predictors) {

        this.businessModel = businessModel;
        this.group = group;
        this.name = name;
        this.comments = comments;
        this.startDate = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        this.endDate = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        // Generate UUID
        this.uuid = UUID.randomUUID().toString();
        this.predictors = predictors;
        for (MlExtractionPredictor p: this.predictors) {
            p.setExtraction(this);
        }

    }


    public void start(JobServer jobServer) throws MLModelException {
        List<String> predictorsMapping = generatePredictorMappings();
        Map<String, String> response =
                jobServer.extract(
                        startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        group.getId().toString(),
                        "thingSnapshots",
                        predictorsMapping,
                        uuid,
                        businessModel.getAppName()
                );

        status = response.get("status");
        jobId = response.get("jobId");
    }

    private List<String> generatePredictorMappings() {
        List<String> mappings = new ArrayList<>();
        List<MlExtractionPredictor> more = new LinkedList<>();
        more.addAll(this.defaultPredictors);
        more.addAll(this.predictors);
        for (MlExtractionPredictor p: more) {
            mappings.add(p.toConfigurationString());
        }
        return mappings;
    }


    public Map toMap() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        Map<String, Object> businessModelMap = new HashMap<>();
        businessModelMap.put("id", businessModel.getId());
        businessModelMap.put("name", businessModel.getName());


        Map<String, Object> groupMap = new HashMap<>();
        groupMap.put("id", group.getId());
        groupMap.put("name", group.getName());


        Map<String, Object> map = publicMap();
        map.put("id", getId());
        map.put("startDate", dateFormat.format(startDate));
        map.put("endDate", dateFormat.format(endDate));
        map.put("businessModel", businessModelMap);
        map.put("group", groupMap);

        List<Map<String, Object>> predictorsMaps = new ArrayList<>();
        for (MlExtractionPredictor predictor : predictors) {
            predictorsMaps.add(predictor.toMap());
        }

        map.put("inputs", predictorsMaps);


        return map;
    }


}

