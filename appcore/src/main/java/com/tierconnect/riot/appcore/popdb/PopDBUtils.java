package com.tierconnect.riot.appcore.popdb;

import java.io.*;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import org.apache.log4j.Logger;

public class PopDBUtils {

    private final static Logger logger = Logger.getLogger(PopDBUtils.class);

    static NameGenerator ng = new NameGenerator();
    static Map<String, Integer> userCount = new HashMap<>();

    public static GroupType popGroupType(String name, Group parentGroup, GroupType parentGroupType, String
            description) {
        System.out.println("******* Populating GroupType: name: " + name + "; description: " + description);
        GroupType groupType = new GroupType();
        groupType.setGroup(parentGroup);
        groupType.setName(name);
        groupType.setCode(Normalizer.normalize(name.replaceAll(" ", ""), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", ""));
        groupType.setDescription(description);
        groupType.setParent(parentGroupType);
        GroupTypeService.getInstance().insert(groupType);
        return groupType;
    }

    public static Group popGroup(String name, String code, Group parent, GroupType type, String description) {
        System.out.println("******* Populating Group: name: " + name + "; description: " + description);
        Group group = new Group();
        group.setName(name);
        group.setCode(code);
        group.setDescription(description);
        group.setParent(parent);
        group.setGroupType(type);
        GroupService.getInstance().insert(group);
        return group;
    }

    public static Role popRole(String name, String description, Collection<Resource> resources, Group group,
                               GroupType groupType) {
        System.out.println("******* Populating Role: name: " + name + "; description: " + description);
        Role role = new Role(name, description);
        role.setGroup(group);
        role.setGroupTypeCeiling(groupType);
        role = RoleService.getInstance().insert(role);

        if (resources != null) {
            for (Resource resource : resources) {
                RoleResourceService.getInstance().insert(role, resource, resource.getAcceptedAttributes());
            }
        }

        return role;
    }

    public static User popUser(String name, Group group, Role role) {
        return popUser(name, name, group, role);
    }

    public static User popUser(String name, String password, Group group, Role role) {
        System.out.println("******* Populating User: name: " + name);
        User user = new User(name);
        String[] names = ng.getName();
        user.setFirstName(names[0]);
        user.setLastName(names[2]);
        user.setPassword(password);
        user.setGroup(group);
        StringBuilder email = new StringBuilder();
        email.append(user.getFirstName().toLowerCase()).append(".").append(user.getLastName().toLowerCase());
        if (userCount.containsKey(email.toString())) {
            int count = userCount.get(email.toString());
            email.append("").append(count);
            userCount.put(email.toString(), count + 1);
        } else {
            userCount.put(email.toString(), 1);
        }
        email.append("@company.com");
        user.setEmail(email.toString());
        UserService.getInstance().insert(user);

        // RoleService.getInstance().update(role);

        if (role != null) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            UserRoleService.getInstance().insert(userRole);
        }

        return user;
    }

    public static Field popField(String name, String description, Group group) {
        Field field = new Field();
        field.setName(name);
        field.setDescription(description);
        field.setGroup(group);
        FieldService.getInstance().insert(field);
        return FieldService.getInstance().selectByName(name);
    }

//    public static void popUserRole(User user, Role role) {
//        UserRole userRole = new UserRole();
//        userRole.setUser(user);
//        userRole.setRole(role);
//        UserRoleService.getInstance().insert(userRole);
//    }

    public static Field popFieldService(String oldName, String newName, String description, Group group, String
            module, String type, Long editLevel, boolean userEditable) {
        Field fs = FieldService.getInstance().selectByName(oldName);
        boolean insert = false;
        if (fs == null) {
            fs = new Field();
            insert = true;
        }
        fs.setName(newName);
        fs.setDescription(description);
        fs.setGroup(group);
        fs.setModule(module);
        fs.setType(type);
        fs.setEditLevel(editLevel);
        fs.setUserEditable(userEditable);
        if (insert) {
            FieldService.getInstance().insert(fs);
        } else {
            FieldService.getInstance().update(fs);
        }
        return fs;
    }

    public static Field popFieldWithParentService(String oldName, String newName, String description, Group group, String
            module, String type, Long editLevel, boolean userEditable, Field parentField) {
        Field fs = FieldService.getInstance().selectByName(oldName);
        boolean insert = false;
        if (fs == null) {
            fs = new Field();
            insert = true;
        }
        fs.setName(newName);
        fs.setDescription(description);
        fs.setGroup(group);
        fs.setModule(module);
        fs.setType(type);
        fs.setEditLevel(editLevel);
        fs.setUserEditable(userEditable);
        fs.setParentField(parentField);
        if (insert) {
            FieldService.getInstance().insert(fs);
        } else {
            FieldService.getInstance().update(fs);
        }
        return fs;
    }

    public static void migrateFieldService(String oldName, String newName, String description, Group group, String
			module, String type, Long editLevel, boolean userEditable, String value) {
        Field f = FieldService.getInstance().selectByName(newName);
        if (f == null) {
            f = PopDBUtils.popFieldService(oldName, newName, description, group, module, type, editLevel, userEditable);
            PopDBUtils.popGroupField(group, f, value);
        } else {
            PopDBUtils.popFieldService(oldName, newName, description, group, module, type, editLevel, userEditable);
            PopDBUtils.popGroupField(group, f, value);
        }

    }

    public static void popGroupField(Group group, Field field, String value) {
        GroupFieldService groupFieldService = GroupFieldService.getInstance();
        GroupField groupField = groupFieldService.selectByGroupField(group, field);
        boolean insert = false;
        if (groupField == null) {
            groupField = new GroupField();
            insert = true;
        }
        groupField.setGroup(group);
        groupField.setField(field);
        groupField.setValue(value);
        if (insert) {
            groupFieldService.insert(groupField);
        } else {
            groupFieldService.update(groupField);
        }
    }

    public static void popGroupResource(Group group) {
        if (group != null) {
            GroupResources groupResources = new GroupResources();
            groupResources.setGroup(group);
            groupResources.setImageTemplateName(group.getCode());
            GroupResourcesService.getInstance().insert(groupResources);
        }
    }

    public static void popDBVersion(){

        Integer currentCodeVersion = 0;
        String currentCodeVersionName = "NO-VERSION";
        List migrationSteps = null;
        try{
            InputStream is = PopDBRequired.class.getResourceAsStream("/migration/migrationSteps.json");
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                if (sb.length() > 0) {
                    String input = sb.toString();
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> definition = objectMapper.readValue(input, new HashMap<String, Object>().getClass());
                    currentCodeVersion = (Integer) definition.get("codeVersionNumber");
                    currentCodeVersionName = (String) definition.get("codeVersionName");

                    List<Map<String, Object>> steps = (List<Map<String, Object>>) definition.get("steps");
                    migrationSteps = ((List)steps.stream().skip(steps.size()-1).findFirst().get().get("migrate"));
                }
            }
            Version version = new Version();
            String computerUser, computerName, computerIP;
            try {
                computerUser = System.getProperty("user.name");
                computerName = InetAddress.getLocalHost().getHostName();
                computerIP = InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                computerUser = System.getProperty("user.name");
                computerName = "Hostname can not be resolved";
                computerIP = "Hostname can not be resolved";
            }

            version.setComputerUser(computerUser);
            version.setComputerName(computerName);
            version.setComputerIP(computerIP);

            version.setInstallTime(new Date());

            version.setDbVersion(currentCodeVersion+"");
            version.setVersionName(currentCodeVersionName);
            version.setVersionDesc("First install by PopDB");
            version.setGitBranch(Configuration.getProperty("git.branch"));


            VersionService.getInstance().insert(version);

            if(migrationSteps!= null){
                migrationSteps.forEach(migrationStep -> {
                    MigrationStepResult migration = new MigrationStepResult();

                    migration.setMigrationPath((String) ((Map)migrationStep).get("path"));
                    migration.setMigrationResult("POPDB-INCLUDED");
                    migration.setDescription((String) ((Map)migrationStep).get("description"));
                    migration.setMessage("Migration included in PopDB installation");
                    migration.setVersion(version);

                    MigrationStepResultService.getMigrationStepResultDAO().insert(migration);

                });
            }


        }catch (Exception e){
            logger.error("Unable to register version", e);
        }
    }
}
