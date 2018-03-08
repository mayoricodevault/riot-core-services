package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;

import java.util.Map;
import java.util.UUID;

/**
 * Created by pablo on 8/2/16.
 *
 * Algorithm used to train a dataset and create a model. Not persisted
 */
public class MlAlgorithm {
    private MlExtraction mlExtraction;

    public MlAlgorithm(MlExtraction mlExtraction) {
        this.mlExtraction = mlExtraction;
    }

    public MlModel train (JobServer server, String name, String comments, Group group)  throws MLModelException {
        String uuid = UUID.randomUUID().toString();
        Map<String, String> response = server.train(
                mlExtraction.getUuid(),
                uuid
        );
        System.out.println("!!!@@____---------------> " + response);
        String status =  JobServer.JobStatus.parseJobStatus(response.get("status")).getStatusName();
        String sparkJobId = response.get("jobId");


        return new MlModel( mlExtraction, group, name, comments,  uuid, sparkJobId, status);
    }
}
