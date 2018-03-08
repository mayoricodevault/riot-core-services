package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.iot.entities.exceptions.MLModelException;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by pablo on 6/21/16.
 */
public interface JobServer {

    public enum JobStatus {

        STARTED     ("STARTED"),
        FINISHED    ("FINISHED"),
        ERROR       ("ERROR");

        private final String statusName;

        JobStatus(String statusName) {
            this.statusName = statusName;
        }

        public String getStatusName() {
            return this.statusName;
        }

        public static JobStatus parseJobStatus(String str) {

            switch (str) {
                case "STARTED" : return STARTED;
                case "FINISHED" : return FINISHED;
                case "ERROR" : return ERROR;
            }

            return null;
        }

    }

    Map<String, String> extract (LocalDate start, LocalDate end, String collection, String groupId,
                               List<String> predictors, String sparkExtractionId, String appName) throws MLModelException;

    Map<String, String> train (String extractionUUID, String uuid) throws MLModelException;


    Map<String, Object> predict(String trainingId, String name, Map<String, String> predictorParams) throws MLModelException;

}
