package com.tierconnect.riot.appcore.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Version;
import com.tierconnect.riot.appcore.services.VersionService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.version.CodeVersion;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.core.test.BaseTest;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.services.BrokerClientHelper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by vealaro on 9/27/16.
 * Modified by achambi on 4/18/17.
 * Modified by julio.rocha on 24-07-17.
 */
public class BaseTestIOT extends BaseTest {

    private DateFormatAndTimeZone dateFormatAndTimeZone = new DateFormatAndTimeZone();
    private ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void initNoTransactionalConfiguration() {
        if (SequenceDAO.getInstance().sequenceContext == null) {
            SequenceDAO.getInstance().initSequences();
        }
    }

    @Override
    protected void previousConfiguration() throws Exception {
        fillVersion();
    }

    private void fillVersion() {
        if (CodeVersion.getInstance().getCodeVersion() == 0) {
            Version lastVertion = getLastVersion();
            CodeVersion.getInstance().setCodeVersion(Integer.parseInt(lastVertion.getDbVersion()));
            CodeVersion.getInstance().setCodeVersionName(lastVertion.getVersionName());
            BrokerClientHelper br = new BrokerClientHelper();
            br.init("APP-pub-" + UUID.randomUUID().toString(), Boolean.valueOf(Configuration.getProperty("broker" +
                    ".connection.wait")));
        }
    }

    private Version getLastVersion() {
        return VersionService.getInstance().listPaginated(null, null, "id:desc").get(0);
    }

    protected Map<String, Object> jsonToMap(String resourcePath) throws IOException {
        InputStream resource = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
        return mapper.readValue(resource, LinkedHashMap.class);
    }

    protected String mapToJson(Map<String, Object> expected) throws JsonProcessingException {
        return mapper.writeValueAsString(expected);
    }

    protected String getIsoDateFormatWithoutZone(Date date) {
        return dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(date);
    }

    protected Date getDate(String isoDate){
        return dateFormatAndTimeZone.parseISOStringToDate(isoDate);
    }
}
