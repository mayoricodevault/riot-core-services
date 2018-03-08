package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.mongoTransform.BsonToMap;
import org.apache.commons.lang.StringUtils;
import org.bson.BsonDocument;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.tierconnect.riot.api.assertions.Assertions.isNotBlank;
import static org.apache.commons.lang.StringUtils.*;

//TODO: Create unit test, Victor Angel Chambi Nina. 15/08/2017 (Tuesday, August 15, 2017)
/**
 * Created by achambi on 8/15/17.
 * Class for manage mongo error messages.
 */
public class MongoErrorMessage {


    private int ok;
    private String errorMessage;
    private String code;
    private String codeName;


    private MongoErrorMessage(int ok, String errorMessage, String code, String codeName) {
        this.ok = ok;
        this.errorMessage = isNotBlank(errorMessage) ? errorMessage : StringUtils.EMPTY;
        this.code = isNotBlank(code) ? code : StringUtils.EMPTY;
        this.codeName = isNotBlank(codeName) ? codeName : StringUtils.EMPTY;
    }

    private MongoErrorMessage(int ok, String errorMessage) {
        this(ok, errorMessage, "1", "SyntaxError");
    }

    private MongoErrorMessage(Map<String, Object> errorMap) {
        this.errorMessage = StringUtils.EMPTY;
        this.code = StringUtils.EMPTY;
        this.codeName = StringUtils.EMPTY;
        if (errorMap.get("error") instanceof LinkedHashMap) {
            LinkedHashMap error = (LinkedHashMap) errorMap.get("error");
            if (isNotBlank(error.get("ok")) &&
                    isNumeric(error.get("ok").toString())) {
                this.ok = Integer.parseInt(error.get("ok").toString());
            }
            this.errorMessage = isNotBlank(error.get("errmsg")) ? error.get("errmsg").toString() : this.errorMessage;
            this.code = isNotBlank(error.get("code")) ? error.get("code").toString() : this.code;
            this.codeName = isNotBlank(error.get("codeName")) ? error.get("codeName").toString() : this.codeName;
        }
    }

    public int getOk() {
        return ok;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getCode() {
        return code;
    }

    public static MongoErrorMessage errorMessageHandler(String line) {
        if (line != null) {
            if (line.contains("E QUERY    [thread1] SyntaxError:")) {
                return new MongoErrorMessage(0, "Error in query syntax");
            }
            if (line.contains("\"errmsg\"")) {
                return new MongoErrorMessage(BsonToMap.getMap(BsonDocument.parse(line)));
            }
        } else {
            return new MongoErrorMessage(0, "There was an error reading the file line. line is NULL.");
        }
        return null;
    }
}
