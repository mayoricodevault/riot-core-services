package com.tierconnect.riot.appcore.popdb;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.ResourceService;

import java.util.HashSet;

/**
 * Populate POPDB Required and IOT
 * Created by angelchambi on 3/22/16.
 */
public class PopulateResources{
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(PopulateResources.class);

    public static HashSet<Resource> populatePopDBRequired(Group group){
        HashSet<Resource> resources = new HashSet<>();
        // make sure we do not get non-appcore classes as well (e.g. vetclinic
        // and samsung)
        logger.info("***************** BEGIN POPULATE RESOURCES *******************");
        ResourceService resourceService = ResourceService.getInstance();
        Resource moduleThings = Resource.getModuleResource(group, "Things", "Things");
        Resource moduleTenants = Resource.getModuleResource(group, "Tenants", "Tenants");
        Resource moduleGateway = Resource.getModuleResource(group, "Gateway", "Gateway");
        Resource moduleModel = Resource.getModuleResource(group, "Model", "Model");
        Resource moduleServices = Resource.getModuleResource(group, "Services", "Services");
        Resource moduleAnalytics = Resource.getModuleResource(group, "Analytics", "Analytics");
        Resource moduleFolder = Resource.getModuleResource(group, "Folder", "Folder");
        Resource moduleSecurity = Resource.getModuleResource(group, "Security", "Security");

        //*********************** PRINCIPAL MENU **********************//
        resources.add(resourceService.insert(moduleThings));
        resources.add(resourceService.insert(moduleTenants));
        resources.add(resourceService.insert(moduleGateway));
        resources.add(resourceService.insert(moduleModel));
        resources.add(resourceService.insert(moduleServices));
        resources.add(resourceService.insert(moduleAnalytics));
        resources.add(resourceService.insert(moduleFolder));
        resources.add(resourceService.insert(moduleSecurity));
        //*********************** PRINCIPAL MENU **********************//


        //***********************  MAIN MODULE THINGS ***************//
        //****** MODULE: Things ******************//
        //****** MODULE: THE POPULATE THIS MODULE IS ON  PopulateResourcesIOT ******************//
        //****** MODULE: Thing Types *************//
        //****** MODULE: THE POPULATE THIS MODULE IS ON  PopulateResourcesIOT ******************//


        //*********************** MAIN MODULE TENANT **********************************//

        //****** MODULE: User******************//
        Resource userResource = resourceService.insert(Resource.getClassResource(group, User.class, moduleTenants));
        resources.add(userResource);
        resources.add(resourceService.insert(Resource.getPropertyResource(group,
                                                                          userResource,
                                                                          "editRoamingGroup",
                                                                          "User Edit Roaming Group",
                                                                          "User Edit Roaming Group")));
        //****** MODULE: Tenant Groups***************//
        resources.add(resourceService.insert(Resource.getClassResource(group, Group.class, moduleTenants)));
        //****** MODULE: Tenant Group Types**********//
        resources.add(resourceService.insert(Resource.getClassResource(group, GroupType.class, moduleTenants)));
        //****** MODULE: Roles **********************//
        resources.add(resourceService.insert(Resource.getClassResource(group, Role.class, moduleTenants)));
        //****** MODULE: Resources ******************//
        resources.add(resourceService.insert(Resource.getClassResource(group, Resource.class, moduleTenants)));

        //****** MODULE: Shifts ******************//
        //****** MODULE: THE POPULATE THIS MODULE IS ON  PopulateResourcesIOT ******************//

        //****** MODULE: Licenses ******************//
        Resource licenseResource = Resource.getClassResource(group, License.class, moduleTenants);
        resources.add(resourceService.insert(licenseResource));

        //IMPORTANT NOT FOUND IN FILE EXCEL (ALDO SAID THAT FIELD IS IN TENANT)
        //****** MODULE: Field ******************//
        resources.add(resourceService.insert(Resource.getClassResource(group, Field.class, moduleTenants)));



        //*********************** MAIN MODULE SERVICES *********************************//

        //****** MODULE: Import & Export ******************//
        //****** MODULE: THE POPULATE THIS MODULE IS ON  PopulateResourcesIOT ******************//

        //****** MODULE: Connections **********************//
        resources.add(resourceService.insert(Resource.getClassResource(group, Connection.class, moduleServices)));
        resources.add(resourceService.insert(Resource.getClassResource(group, ConnectionType.class, moduleServices)));

        //****** MODULE: License Generator ****************//
        resources.add(resourceService.insert(Resource.getPropertyResource(group,
                                                                          licenseResource,
                                                                          "generator",
                                                                          "Generate License",
                                                                          "Generate License",
                                                                          moduleServices,
                                                                          2,
                                                                          "u")));

        //****** MODULE: Health & Status ****************//
        //****** MODULE: THE POPULATE THIS MODULE IS ON  PopulateResourcesIOT ******************//
        //****** MODULE: Logs ***************************//
        //****** MODULE: THE POPULATE THIS MODULE IS ON  PopulateResourcesIOT ******************//


        //****** MODULE: Security ******************//
        resources.add(resourceService.insert(Resource.getModuleResource(group,
            "Password Expiration Policy",
            "passwordExpirationPolicy",
            moduleSecurity)));

        logger.info("***************** END POPULATE RESOURCES *******************");
        return resources;
    }
}
