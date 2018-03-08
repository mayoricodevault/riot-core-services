package com.tierconnect.riot.iot.entities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import org.apache.log4j.Logger;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.annotation.Generated;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//todo this class does nothing!!! needs to do the prediction
@Entity
@Table(name = "ml_model")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlModel extends MlModelBase {

    private static Logger logger = Logger.getLogger(MlModel.class);


    //for persistance framework
    public MlModel() {
    }

    public MlModel(MlExtraction extraction,
                   Group group,
                   String name,
                   String comments,
                   String uuid,
                   String sparkJobId,
                   String status) {

        this.extraction = extraction;
        this.group = group;
        this.name = name;
        this.comments = comments;
        this.uuid = uuid;
        this.sparkJobId = sparkJobId;
        this.status = status;

    }


    public Map<String, Object> predict(Map<String, String> params, JobServer server) throws MLModelException {
        if (!extraction.getStatus().equals("FINISHED")) {
            throw new MLModelException("Traning needs to be finished!!!");
        }
        return server.predict(uuid, "ObjectsSoldModel", params);
    }

    public Map toMap() {

        // todo reading metada each time is not efficient -> must change this
        Map<String, Object> metadata = readMetada();

        Map<String, Object> businessModeMap = new HashMap<>();
        businessModeMap.put("id", extraction.getBusinessModel().getId());
        businessModeMap.put("name", extraction.getBusinessModel().getName());

        Map<String, Object> groupMap = new HashMap<>();
        groupMap.put("id", group.getId());
        groupMap.put("name", group.getName());

        Map<String, Object> extractionMap = new HashMap<>();
        extractionMap.put("id", extraction.getId());
        extractionMap.put("name", extraction.getName());


        Map<String, Object> map = publicMap();
        map.put("businessModel", businessModeMap);
        map.put("group", groupMap);
        map.put("extraction", extractionMap);
        map.put("predictors", getPredictorsMetada(metadata));

        return map;
    }

    private List<Map<String, Object>> getPredictorsMetada(Map<String, Object> metadata) {
        if (metadata.containsKey("predictors")) {
            return (List<Map<String, Object>>) metadata.get("predictors");
        }
        else {
            return new ArrayList<>();
        }
    }



    // todo reading metada each tiem is not efficient -> must change
    private Map<String, Object> readMetada() {

        Map<String, Object> metadata = null;

        try {
            if (status.equals(JobServer.JobStatus.FINISHED.getStatusName())) {
                logger.info("job finished - try to read metadata");
                ObjectMapper mapper = new ObjectMapper();
                metadata = mapper.readValue(
                        new File(MlConfiguration.property("trainings.path") + "/" + uuid + ".json"),
                        new TypeReference<Map<String, Object>>() {
                        });
            }
            else {
                logger.info("job not finished - empty metadata");
                metadata = new HashMap<>();
            }
        } catch (IOException e) {
            logger.error(e);
            metadata = new HashMap<>();
        }

        return metadata;
    }
}

