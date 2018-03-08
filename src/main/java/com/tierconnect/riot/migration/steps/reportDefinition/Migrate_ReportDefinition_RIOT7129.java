package com.tierconnect.riot.migration.steps.reportDefinition;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ReportDefinition_RIOT7129 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ReportDefinition_RIOT7129.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        populateMigrateReportProperty();
    }

    private void populateMigrateReportProperty()
    {

        try{
            BooleanBuilder be = new BooleanBuilder();
            ReportDefinitionService.getInstance().listPaginated( be, null, null );
            for( ReportDefinition reportDefinition : ReportDefinitionService.getInstance().listPaginated( be,  null, null ))
            {
                if(reportDefinition.getReportProperty()!=null && reportDefinition.getReportProperty().size()>0)
                {
                    for( ReportProperty reportProperty: reportDefinition.getReportProperty() )
                    {
                        if(reportProperty.getThingType()!=null)
                        {
                            //ThingType thingType = ThingTypeService.getInstance().get( reportProperty.getThingTypeIdReport() );
//							if(thingType!=null)
//							{
                            for( ThingTypeField thingTypeField : reportProperty.getThingType().getThingTypeFields() )
                            {
                                if( reportProperty.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportProperty.setThingTypeField( thingTypeField );
                                    reportProperty.setParentThingType( null );
                                    ReportPropertyService.getReportPropertyDAO().update( reportProperty );
                                    break;
                                }
                            }
//							}

                        }
                    }
                }
                if(reportDefinition.getReportFilter()!=null && reportDefinition.getReportFilter().size()>0)
                {
                    for( ReportFilter reportFilter: reportDefinition.getReportFilter() )
                    {
                        if(reportFilter.getThingType()!=null)
                        {
                            ThingType thingType = reportFilter.getThingType();
                            for( ThingTypeField thingTypeField : thingType.getThingTypeFields() )
                            {
                                if( reportFilter.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportFilter.setThingTypeField( thingTypeField );
                                    reportFilter.setParentThingType( null );
                                    ReportFilterService.getReportFilterDAO().update( reportFilter );
                                    break;
                                }
                            }

                        }
                    }
                }
                if(reportDefinition.getReportRule()!=null && reportDefinition.getReportRule().size()>0)
                {
                    for( ReportRule reportRule: reportDefinition.getReportRule() )
                    {
                        if(reportRule.getThingType()!=null)
                        {
							/*ThingType thingType = ThingTypeService.getInstance().get( reportRule.getThingTypeIdReport() );
							if(thingType!=null)
							{*/
                            for( ThingTypeField thingTypeField : reportRule.getThingType().getThingTypeFields() )
                            {
                                if( reportRule.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportRule.setThingTypeField( thingTypeField );
                                    reportRule.setParentThingType( null );
                                    ReportRuleService.getReportRuleDAO().update( reportRule );
                                    break;
                                }
                            }
                            //}
                        }
                    }
                }

                if(reportDefinition.getReportGroupBy()!=null && reportDefinition.getReportGroupBy().size()>0)
                {
                    for( ReportGroupBy reportGroupBy: reportDefinition.getReportGroupBy() )
                    {
                        if(reportGroupBy.getThingType()!=null)
                        {
							/*ThingType thingType = ThingTypeService.getInstance().get( reportRule.getThingTypeIdReport() );
							if(thingType!=null)
							{*/
                            for( ThingTypeField thingTypeField : reportGroupBy.getThingType().getThingTypeFields() )
                            {
                                if( reportGroupBy.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportGroupBy.setThingTypeField( thingTypeField );
                                    reportGroupBy.setParentThingType( null );
                                    ReportGroupByService.getReportGroupByDAO().update( reportGroupBy );
                                    break;
                                }
                            }
                            //}
                        }
                    }
                }
            }

        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
