package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.dao.RoleResourceDAO;
import com.tierconnect.riot.appcore.entities.QResource;
import com.tierconnect.riot.appcore.entities.QRoleResource;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.ResourceType;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.iot.entities.*;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ReportEntryOptionService extends ReportEntryOptionServiceBase 
{

    public static final QResource RESOURCE = QResource.resource;

    @Override
    public ReportEntryOption insert(ReportEntryOption reportEntryOption_) {
        ReportEntryOption reportEntryOption = super.insert(reportEntryOption_);
        createResource(reportEntryOption);
        return reportEntryOption;
    }

    public void createResource(ReportEntryOption reportEntryOption) {
        Resource re=new Resource();
        setAcceptedAttributes(re, reportEntryOption);
        re.setGroup(reportEntryOption.getReportDefinition().getGroup());
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = resourceService.getResourceDAO();
//        Resource module = resourceDAO.selectBy(RESOURCE.name.eq(Resource.REPORTS_MODULE));
//        re.setModule(module.getName());
        Resource parent = resourceDAO.selectBy(RESOURCE.typeId.eq(reportEntryOption.getReportDefinition().getId())
                                                              .and(RESOURCE.type.eq(ResourceType.REPORT_DEFINITION.getId())));
        re.setParent(parent);
        setLabelDescription(reportEntryOption, re);
        re.setType(ResourceType.DATA_ENTRY.getId());
        re.setTreeLevel(parent.getTreeLevel() + 1);
        re.setName(Resource.DATA_ENTRY_FORM_MODULE_PREFIX + reportEntryOption.getId());
        re.setFqname(Resource.DATA_ENTRY_FORM_MODULE_PREFIX + reportEntryOption.getId());
        re.setTypeId(reportEntryOption.getId());
        resourceService.insert(re);
    }

    private void setLabelDescription(ReportEntryOption reportEntryOption, Resource re) {
        re.setLabel(" Data Entry : \"" + reportEntryOption.getName()+ "\"");
        re.setDescription(" Data Entry : \"" + reportEntryOption.getName() + "\"");
    }

    @Override
    public ReportEntryOption update(ReportEntryOption reportEntryOption) {
        ResourceDAO resourceDAO = ResourceService.getInstance().getResourceDAO();
        Resource re = resourceDAO.selectBy(RESOURCE.typeId.eq(reportEntryOption.getId()).and(RESOURCE.type.eq(ResourceType.DATA_ENTRY.getId())));
        re.setGroup(reportEntryOption.getReportDefinition().getGroup());
        setAcceptedAttributes(re, reportEntryOption);
        return super.update(reportEntryOption);
    }

    private void setAcceptedAttributes(Resource re, ReportEntryOption reportEntryOption) {
        Set<String> attributes = new LinkedHashSet();
        attributes.add(reportEntryOption.getNewOption() ? Resource.INSERT_PERMISSION : "");
        attributes.add((reportEntryOption.getEditOption() || reportEntryOption.getAssociate() || reportEntryOption.getDisassociate()) ? Resource.UPDATE_PERMISSION : "");
        attributes.add((reportEntryOption.getDeleteOption()) ? Resource.DELETE_PERMISSION : "");
        attributes.add(reportEntryOption.getRFIDPrint()  ? Resource.PRINT_PERMISSION : "");
        re.setAcceptedAttributes(attributes);
    }

    @Override
    public void delete(ReportEntryOption reportEntryOption) {
        List<ReportEntryOption> reportEntryOptions = reportEntryOption.getReportDefinition().getReportEntryOption();
        for (ReportEntryOption reportEntryOpt : reportEntryOptions) {
            if (reportEntryOpt.getReportEntryOptionProperties() != null) {
                for (ReportEntryOptionProperty reportEntryOptionProp : reportEntryOpt.getReportEntryOptionProperties()){
                    if (reportEntryOpt.getId().equals(reportEntryOption.getId())){
                        EntryFormPropertyDataService.getEntryFormPropertyDataDAO().deleteAllBy(
                                QEntryFormPropertyData.entryFormPropertyData.reportEntryOptionProperty.eq(reportEntryOptionProp));
                    }
                }
            }
        }
        Iterator<ReportEntryOption> i = reportEntryOptions.iterator();
        while (i.hasNext()) {
            if (i.next().getId().equals(reportEntryOption.getId())) {
                i.remove();
            }
        }
        ResourceDAO resourceDAO = ResourceService.getInstance().getResourceDAO();
        Resource re = resourceDAO.selectBy(RESOURCE.typeId.eq(reportEntryOption.getId()).and(RESOURCE.type.eq(ResourceType.DATA_ENTRY.getId())));
        RoleResourceDAO roleResourceDAO = RoleResourceService.getInstance().getRoleResourceDAO();
        roleResourceDAO.deleteAllBy(QRoleResource.roleResource.resource.eq(re));
        resourceDAO.delete(re);
        ReportEntryOptionPropertyService.getInstance().getReportEntryOptionPropertyDAO().deleteAllBy(QReportEntryOptionProperty.reportEntryOptionProperty.reportEntryOption.eq(reportEntryOption));
        reportEntryOption.getReportEntryOptionProperties().clear();
        super.delete(reportEntryOption);
    }

    public ReportEntryOption getByNameAndReportDefinition(String name, String reportName, String reportType){
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().getByNameAndType(reportName, reportType);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportEntryOption.reportEntryOption.name.eq(name));
        be = be.and(QReportEntryOption.reportEntryOption.reportDefinition.eq(reportDefinition));
        return getReportEntryOptionDAO().selectBy(be);
    }
}

