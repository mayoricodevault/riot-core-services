package com.tierconnect.riot.appcore.services;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.utils.XMLResourceBundleControl;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
/**
 * Created by agutierrez on 4/16/15.
 */
public class I18NService {
    static Logger logger = Logger.getLogger(I18NService.class);

    public static final String MESSAGES_BUNDLE = "ApplicationResources";
    public static final String SEPARATOR = "-";

    public String getKey(String key, String localeStr, String module) {
        Locale locale = getLocale(localeStr);
        String resourceDir = ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "i18NDirectory");
        return getString(module, key, locale, resourceDir);
    }

    public String getString(String module, String key, Locale locale, String resourceDir) {
        HashSet<String> keys = new HashSet<>();
        keys.add(key);
        return getStrings(module, keys, locale, resourceDir).get(key);
    }

    public ResourceBundle getResourceBundle(String baseName, Locale locale, String resourcesDir) {
        XMLResourceBundleControl xmlResourceBundleControl = new XMLResourceBundleControl();
        ClassLoader loader = I18NService.class.getClassLoader();
        if (StringUtils.isNotEmpty(resourcesDir)) {
            File file = new File(resourcesDir);
            if (file.exists() && file.isDirectory()) {
                File file2 = new File(resourcesDir + File.separator + "ApplicationResources_en.xml");
                if (file2.exists() && file2.isFile()) {
                    URL[] urls;
                    try {
                        urls = new URL[]{file.toURI().toURL()};
                        loader = new URLClassLoader(urls);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.error("Invalid I18N Directory: " + resourcesDir + " it doesn't contain I18N files");
                }
            } else {
                logger.error("Invalid I18N Directory: " + resourcesDir);
            }
        }
        ResourceBundle rb;
        if (locale == null) {
            rb = ResourceBundle.getBundle(baseName, Locale.getDefault(), loader, xmlResourceBundleControl);
        } else {
            rb = ResourceBundle.getBundle(baseName, locale, loader, xmlResourceBundleControl);
        }
        return rb;
    }

    public Map<String, String> getStrings(String module, Set<String> keys, Locale locale, String resourcesDir) {
        Map<String, String> result = new HashMap<>();
        ResourceBundle parent = getResourceBundle(MESSAGES_BUNDLE, locale, resourcesDir);
        ResourceBundle parentNL = getResourceBundle(MESSAGES_BUNDLE, null, resourcesDir);
        ResourceBundle child;
        ResourceBundle childNL;
        try {
            child = StringUtils.isEmpty(module) ? null : getResourceBundle(MESSAGES_BUNDLE + SEPARATOR + module, locale, resourcesDir);
        } catch (MissingResourceException es) {
            child = null;
        }
        try {
            childNL = StringUtils.isEmpty(module) ? null : getResourceBundle(MESSAGES_BUNDLE + SEPARATOR + module, null, resourcesDir);
        } catch (MissingResourceException es) {
            childNL = null;
        }
        for (String key : keys) {
            result.put(key, null);
            if (result.get(key) == null && child != null) {
                try {
                    result.put(key, child.getString(key));
                } catch (MissingResourceException ex) {
                }
            }
            if (result.get(key) == null && childNL != null) {
                try {
                    result.put(key, childNL.getString(key));
                } catch (MissingResourceException ex) {
                }
            }
            if (result.get(key) == null && parent != null) {
                try {
                    result.put(key, parent.getString(key));
                } catch (MissingResourceException ex) {
                }
            }
            if (result.get(key) == null && parentNL != null) {
                try {
                    result.put(key, parentNL.getString(key));
                } catch (MissingResourceException ex) {
                }
            }
        }
        return result;
    }

    public Map<String, String> getAllStrings(String module, Locale locale, String resourcesDir) {
        ResourceBundle parent = getResourceBundle(MESSAGES_BUNDLE, locale, resourcesDir);
        Set<String> keys = new HashSet<String>(Collections.list(parent.getKeys()));
        ResourceBundle parentNL = getResourceBundle(MESSAGES_BUNDLE, null, resourcesDir);
        keys.addAll(new HashSet<String>(Collections.list(parentNL.getKeys())));
        try {
            ResourceBundle child = StringUtils.isEmpty(module) ? null : getResourceBundle(MESSAGES_BUNDLE + SEPARATOR + module, locale, resourcesDir);
            if (child != null) {
                keys.addAll(new HashSet<String>(Collections.list(child.getKeys())));
                ResourceBundle childNL = StringUtils.isEmpty(module) ? null : getResourceBundle(MESSAGES_BUNDLE + SEPARATOR + module, null, resourcesDir);
                if (childNL != null) {
                    keys.addAll(new HashSet<String>(Collections.list(childNL.getKeys())));
                }
            }
            keys.addAll(new HashSet<String>(Collections.list(parent.getKeys())));
        } catch (MissingResourceException es) {
            logger.error("error in getAllStrings: " + es.getMessage(), es);
        }
        return getStrings(module, keys, locale, resourcesDir);
    }

    public Locale getLocale(String locale) {
        if (StringUtils.isNotEmpty(locale)) {
            return Locale.forLanguageTag(locale);
        } else {
            Locale localeRes = Locale.getDefault();
            try {
                User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
                if (currentUser != null) {
                    String language = ConfigurationService.getAsString(currentUser, "language");
                    if (StringUtils.isNotEmpty(language)) {
                        localeRes = Locale.forLanguageTag(language);
                    }
                } else {
                    String language = ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "language");
                    if (StringUtils.isNotEmpty(language)) {
                        localeRes = Locale.forLanguageTag(language);
                    }
                }
            } catch (Exception ex) {
                logger.error(ex.getMessage());
            }
            return localeRes;
        }
    }

}
