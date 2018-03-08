package com.tierconnect.riot.appcore.services;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tierconnect.riot.appcore.dao.FieldDAO;
import com.tierconnect.riot.appcore.dao.GroupDAO;
import com.tierconnect.riot.appcore.dao.GroupFieldDAO;
import com.tierconnect.riot.appcore.entities.*;
import org.apache.log4j.Logger;

public class ConfigurationService {
	static Logger logger = Logger.getLogger(ConfigurationService.class);
	static private FieldDAO _fieldDAO;
	static private GroupFieldDAO _groupFieldDAO;
	static private GroupDAO _groupDAO;

    static private ConfigurationService INSTANCE = new ConfigurationService();

    public static ConfigurationService getInstance()
    {
        return INSTANCE;
    }

	public static GroupFieldDAO getGroupFieldDAO() {
		if (_groupFieldDAO == null) {
			_groupFieldDAO = new GroupFieldDAO();
		}
		return _groupFieldDAO;
	}

	public static GroupDAO getGroupDAO() {
		if (_groupDAO == null) {
			_groupDAO = new GroupDAO();
		}
		return _groupDAO;
	}

	public static FieldDAO getFieldDAO() {
		if (_fieldDAO == null) {
			_fieldDAO = new FieldDAO();
		}
		return _fieldDAO;
	}

	public static String getAsString(Group group, String fieldName) {
		return getAsStringMap(group).get(fieldName);
	}

	public static String getAsString(User user, String fieldName) {
		return getAsStringMap(user).get(fieldName);
	}

	public static Long getAsLong(Group group, String fieldName) {
		try {
			return Long.valueOf(getAsStringMap(group).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

	public static Long getAsLong(User user, String fieldName) {
		try {
			return Long.valueOf(getAsStringMap(user).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

	public static Integer getAsInteger(User user, String fieldName) {
		try {
			return Integer.valueOf(getAsStringMap(user).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

	public static Integer getAsInteger(Group group, String fieldName) {
		try {
			return Integer.valueOf(getAsStringMap(group).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

	/**
	 * If you are root then the creation level or other configs should depend on the company where you are creating the object
	 * @param user
	 * @param group
	 * @param fieldName
     * @return
     */
	public static Long getAsLong(User user, Group group, String fieldName) {
		return getAsLong(user, group, fieldName, null);
	}

	public static Long getAsLong(User user, Group group, String fieldName, Group activeGroup) {
		try {
			activeGroup = (activeGroup == null)? user.getActiveGroup() : activeGroup;
			if (activeGroup.getTreeLevel() == 1 && !(group.getTreeLevel() == 1)) {
				return Long.valueOf(getAsStringMap(group.getParentLevel2()).get(fieldName));
			} else {
				return Long.valueOf(getAsStringMap(user).get(fieldName));
			}
		} catch (Exception ex) {
			return null;
		}
	}

	public static Double getAsDouble(Group group, String fieldName) {
		try {
			return Double.valueOf(getAsStringMap(group).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

	public static Double getAsDouble(User user, String fieldName) {
		try {
			return Double.valueOf(getAsStringMap(user).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

	public static BigDecimal getAsBigDecimal(Group group, String fieldName) {
		try {
			return new BigDecimal(getAsStringMap(group).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

	public static BigDecimal getAsBigDecimal(User user, String fieldName) {
		try {
			return new BigDecimal(getAsStringMap(user).get(fieldName));
		} catch (Exception ex) {
			return null;
		}
	}

    public static Boolean getAsBoolean(Group group, String fieldName) {
        try {
            return Boolean.valueOf(getAsStringMap(group).get(fieldName));
        } catch (Exception ex) {
            return null;
        }
    }

    public static Boolean getAsBoolean(User user, String fieldName) {
        try {
			logger.info(" user: " + user);
			logger.info(" fieldName: " + fieldName);
            return  Boolean.valueOf(getAsStringMap(user).get(fieldName));
        } catch (Exception ex) {
			Object userName = user!=null?user.getId():null;
			logger.error("Error getting Boolean value with parameters> User: "+ userName + ", fieldName:"+fieldName, ex);
            return null;
        }
    }

    public static boolean getAsBoolean(Group group, String fieldName, boolean defaultVal) {
        try {
            return Boolean.valueOf(getAsStringMap(group).get(fieldName));
        } catch (Exception ex) {
            return defaultVal;
        }
    }

	public static Map<String, String> getAsStringMap(User user) {		
		Map<String, String> result = getAsStringMap(user.getActiveGroup());
		List<UserField> fields = UserFieldService.getInstance().listByUser(user);
		if( (fields!=null) && (!fields.isEmpty())) {
			for (UserField userField : fields) {
				String fieldName = userField.getField().getName();
				//if (LicenseService.getInstance().isValidField(user, fieldName)) {
				result.put(fieldName, userField.getValue());
				//}
			}
		}
		return result;
	}

	public static Map<String, String> getAsStringMap(Group group) {
		return GroupFieldService.getInstance().listInheritedFieldsByGroupNative(group);
	}

}
