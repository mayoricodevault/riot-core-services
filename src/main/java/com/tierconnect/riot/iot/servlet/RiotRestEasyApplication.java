	package com.tierconnect.riot.iot.servlet;

/**
 * @author grea
 */
import com.tierconnect.riot.appcore.controllers.*;
import com.tierconnect.riot.appcore.servlet.MasterFilter;
import com.tierconnect.riot.iot.controllers.*;
import com.tierconnect.riot.sdk.servlet.exception.GeneralExceptionMapper;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

    public class RiotRestEasyApplication extends Application {
	protected HashSet<Object> singletons = new HashSet<Object>();

	public RiotRestEasyApplication() {
		singletons.add(new GroupController());
		singletons.add(new GroupResourcesController());
		singletons.add(new ReportDefinitionController());
		singletons.add(new ReportExecutionController());
		singletons.add(new ReportRuleController());
		singletons.add(new ThingController());
//		singletons.add(new ThingFieldController());
		singletons.add(new ThingTypeController());
		singletons.add(new GroupTypeController());
		singletons.add(new UserController());
		singletons.add(new ConnectionTypeController());
		singletons.add(new ConnectionController());
		/*
		singletons.add(new DatasourceController());
		singletons.add(new DatasourceTypeController());
		*/
		singletons.add(new LicenseController());
		singletons.add(new FieldController());
		singletons.add(new RoleController());
		singletons.add(new ResourceController());
		singletons.add(new ShiftController());
		singletons.add(new ThingParentHistoryController());
        singletons.add(new LocalMapController());
//        singletons.add(new LocalMapPointController());
        singletons.add(new LogicalReaderController());
		singletons.add(new ZoneController());
		singletons.add(new ZoneGroupController());
        singletons.add(new ZonePropertyController());
		singletons.add(new ZonePointController());
		singletons.add(new ZoneTypeController());
		singletons.add(new BackgroundProcessController());

        singletons.add(new I18NController());

		singletons.add(new LogController());
        singletons.add(new EdgeboxController());
        singletons.add(new EdgeboxRuleController());

        singletons.add(new CustomApplicationController());
        singletons.add(new CustomObjectController());
        singletons.add(new CustomObjectTypeController());
        singletons.add(new CustomFieldController());
        singletons.add(new CustomFieldTypeController());
        singletons.add(new CustomFieldValueController());
        singletons.add(new FileManagementController());
        singletons.add(new AssociateController());
        singletons.add(new BlockchainController());

		singletons.add(new ThingBridgeController());
	
		singletons.add(new HealthAndStatusController());
		singletons.add(new MongoDBController());
		singletons.add(new ThingTypeTemplateController());
		singletons.add(new ThingTypeFieldTemplateController());
		singletons.add(new DataTypeController());
		singletons.add(new RFIDPrinterController());
		singletons.add(new NotificationTemplateController());

		singletons.add(new TimeSeriesReportPOCController());
		singletons.add(new AttachmentController());
		singletons.add(new ThingsController());
		singletons.add(new DevicesController());

		singletons.add(new MlPredictionsController());
		singletons.add(new MlModelController());
		singletons.add(new MlBusinessModelController());
		singletons.add(new MlExtractionsController());
		singletons.add(new MlUploadJarController());
		singletons.add(new MlDataExtractionController());
		singletons.add(new MlPersistentPredictionController());
		singletons.add(new MlDataPersistentPredictionController());
		singletons.add(new MlResponsesController());

		singletons.add(new SecondLevelCacheController());
		singletons.add(new ParametersController());

		singletons.add(new ActionConfigurationController());

		singletons.add(new SmartContractDefinitionController());
		singletons.add(new SmartContractPartyController());
		singletons.add(new SmartContractConfigController());

		singletons.add(new VersionController());
		singletons.add(new FavoriteController());
		singletons.add(new FolderController());
        singletons.add(new RecentController());
		singletons.add(new ScheduledRuleController());
	}

	@Override
	public Set<Class<?>> getClasses() {
		HashSet<Class<?>> providers = new LinkedHashSet<Class<?>>();
        providers.add(MasterFilter.class);
		providers.add(GeneralExceptionMapper.class);
		return providers;
	}

	@Override
	public Set<Object> getSingletons() {
		return singletons;
	}
}
