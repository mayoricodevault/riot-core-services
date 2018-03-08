package com.tierconnect.riot.iot.controllers;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.commons.utils.HttpRequestUtil;
import com.tierconnect.riot.commons.utils.RelativeDateUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.reports.ReportAppService;
import com.tierconnect.riot.iot.reports.autoindex.services.IndexInformationMongoService;
import com.tierconnect.riot.iot.reports_integration.ReportFactory;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.utils.ReportExecutionUtils;
import com.tierconnect.riot.iot.utils.ReportUtils;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jose4j.json.internal.json_simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.tierconnect.riot.appcore.controllers.RiotShiroRealm.getOverrideVisibilityCache;
import static com.tierconnect.riot.appcore.services.ConfigurationService.getAsBoolean;
import static com.tierconnect.riot.appcore.utils.VisibilityUtils.getVisibilityGroup;
import static com.tierconnect.riot.commons.Constants.REPORT_LOG_ENABLE;
import static com.tierconnect.riot.commons.Constants.REPORT_LOG_THRESHOLD;
import static com.tierconnect.riot.iot.reports.autoindex.services.ReportLogMongoService.getInstance;
import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Created by pablo on 10/29/14.
 * <p>
 * Controller to execute reports
 */

@SuppressWarnings("ConstantConditions")
@Path("/reportExecution")
@Api("/reportExecution")
public class ReportExecutionController {

    private static Logger logger = Logger.getLogger(ReportExecutionController.class);

    @SuppressWarnings("FieldCanBeLocal")
    private static String MAX_REPORT_RECORDS = "maxReportRecords";

    /**
     * Runs a table detail report
     */
    @POST
    @Path("/{id}")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Execute Report Definition. Used by 'TableDetail' report. Also used" +
            " by 'Map' report when viewing as a table.")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 201, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response executeReportDefinitions(
            @PathParam("id") Long id,
            @DefaultValue("false") @QueryParam("export") Boolean export,
            @QueryParam("startDate") Long startDate,
            @QueryParam("endDate") Long endDate,
            @QueryParam("pageSize") Integer pageSize,
            @QueryParam("relativeDate") String relativeDate,
            @QueryParam("pageNumber") Integer pageNumber,
            @QueryParam("version") int version,
            Map<String, Object> body,
            @QueryParam("now") Date now,
            @ApiParam(value = "Set true if you want Thing Type UDF as object with 'Serial Number' and 'Name'. (Mobile" +
                    " support)")
            @DefaultValue("false")
            @QueryParam("thingTypeUdfAsObject") boolean thingTypeUdfAsObject,
            @Context HttpServletRequest request) {
        try {

            logger.debug("id='" + id + "'");
            logger.debug("export='" + export + "'");
            logger.debug("startDate='" + startDate + "'");
            logger.debug("endDate='" + endDate + "'");
            logger.debug("pageSize='" + pageSize + "'");
            logger.debug("relativeDate='" + relativeDate + "'");
            logger.debug("pageNumber='" + pageNumber + "'");
            logger.debug("body='" + body + "'");
            logger.debug("now='" + now + "'");
            logger.debug("thingTypeUdfAsObject='" + thingTypeUdfAsObject + "'");

            String module = Constants.UI_REPORT;
            if (export) {
                module = Constants.EXPORT_REPORT;
            }

            ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
            if (reportDefinition == null) {
                return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
            }

            long start = System.currentTimeMillis();
            HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
            ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), module);

            EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
            GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);
            now = (now == null) ? new Date() : now;
            DateHelper dh = getRelativeDateHelper(relativeDate, startDate, endDate, now);
            ReportFactory reportFactory = new ReportFactory(getDateFormatAndTimeZone());
            Response response;
            if (export) {
                response = getFileWithResults(
                        body, pageSize, pageNumber, reportDefinition, dh, now, reportFactory, httpRequestUtil
                                .getInfoRequestString());
            } else {
                // Getting user
                User user = (User) SecurityUtils.getSubject().getPrincipal();
                boolean reportLogEnable = getAsBoolean(user.getActiveGroup(), REPORT_LOG_ENABLE);

                Map<String, Object> reportData = reportFactory.getResult(Constants.REPORT_TYPE_TABLE_DETAIL,
                        reportDefinition, body, pageNumber,
                        pageSize, dh.from(), dh.to(), now, true, httpRequestUtil.getInfoRequestString(),
                        request.getRequestURL().toString(), request.getContextPath(), reportLogEnable, false);
                logger.debug("REPORT BEGIN\n" + ReportUtils.getPrettyPrint(reportData) + "\nREPORT END");
                getInstance().insertLog(reportFactory.getReportInfo(),
                        reportDefinition,
                        httpRequestUtil.getInfoRequestMap(),
                        ConfigurationService.getAsLong(user.getActiveGroup(), REPORT_LOG_THRESHOLD),
                        reportLogEnable,
                        user.getId());
                IndexInformationMongoService.getInstance().updateLastRunDate(reportFactory.getIndexName(), new Date());
                response = RestUtils.sendOkResponse(reportData);
            }
            ReportAppService.instance().logEnd(reportDefinition, start, module);
            commit();
            return response;
        } catch (UserException e) {
            logger.error("Report " + id + " :" + e.getMessage(), e);
            if (e.getMessage().contains("Not found zone properties filter in report [")) {
                return RestUtils.sendResponseWithCode(e.getMessage(), 403);
            }
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("Report " + id + " :" + e.getMessage(), e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 500);
        }
    }

    /**
     * Execute a report, return a json response
     */
    @POST
    @Path("/{id}.geojson")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    //@RequiresPermissions(value = { "reportDefinition:r:{id}" })
    @ApiOperation(position = 2, value = "Execute Report Execution and return geoJson data")
    public Response executeReportGeoJson(@PathParam("id") Long id,
                                         @QueryParam("startDate") Long startDate,
                                         @QueryParam("endDate") Long endDate,
                                         @QueryParam("relativeDate") String relativeDate,
                                         @QueryParam("pageSize") Integer pageSize,
                                         @QueryParam("pageNumber") Integer pageNumber,
                                         Map<String, Object> body,
                                         @Context HttpServletRequest request) throws Exception {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        long start = System.currentTimeMillis();
        HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
        ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), Constants
                .UI_REPORT);

        EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);

        // Calculate Relative Date
        Date now = new Date();
        DateHelper dh = getRelativeDateHelper(relativeDate, startDate, endDate, now);

        TimerUtil tu = new TimerUtil();
        tu.mark();
        ReportFactory reportFactory = new ReportFactory(getDateFormatAndTimeZone());
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        boolean reportLogEnable = getAsBoolean(user.getActiveGroup(), REPORT_LOG_ENABLE);
        Map<String, Object> reportData = reportFactory.getResult(reportDefinition, body, pageNumber, pageSize, dh
                .from(), dh.to(), now, true, httpRequestUtil.getInfoRequestString(), request.getRequestURL().toString
                (), request.getContextPath(), reportLogEnable);
        getInstance().insertLog(reportFactory.getReportInfo(),
                reportDefinition,
                httpRequestUtil.getInfoRequestMap(),
                ConfigurationService.getAsLong(user.getActiveGroup(), REPORT_LOG_THRESHOLD),
                reportLogEnable,
                user.getId());
        IndexInformationMongoService.getInstance().updateLastRunDate(reportFactory.getIndexName(), new Date());
        logger.debug("reportData=\n" + ReportUtils.getPrettyPrint(reportData));
        ReportAppService.instance().logEnd(reportDefinition, start, Constants.UI_REPORT);
        commit();
        return RestUtils.sendOkResponse(reportData, false);
    }

    @POST  // todo, should be GET
    @Path("/{id}/locations")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    //@RequiresPermissions(value = { "reportDefinition:r:{id}" })
    @ApiOperation(position = 3, value = "Execute Report Execution and return geoJson data")
    public Response executeReportGeoJsonForLocationHistory(@PathParam("id") Long id,
                                                           @QueryParam("startDate") Long startDate,
                                                           @QueryParam("endDate") Long endDate,
                                                           @QueryParam("relativeDate") String relativeDate,
                                                           @QueryParam("pageSize") Integer pageSize,
                                                           @QueryParam("pageNumber") Integer pageNumber,
                                                           Map<String, Object> body,
                                                           @Context HttpServletRequest request) throws Exception {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        long start = System.currentTimeMillis();
        HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
        ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), Constants
                .UI_REPORT);

        EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);

        Date now = new Date();
        // Calculate Relative Date
        DateHelper dh = getRelativeDateHelper(relativeDate, startDate, endDate, now);
        ReportFactory reportFactory = new ReportFactory(getDateFormatAndTimeZone());
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        boolean reportLogEnable = getAsBoolean(user.getActiveGroup(), REPORT_LOG_ENABLE);
        Map<String, Object> reportData = reportFactory.getResult(Constants.REPORT_TYPE_MAP_HISTORY, reportDefinition,
                body,
                pageNumber, pageSize, dh.from(), dh.to(), now, true, httpRequestUtil.getInfoRequestString(), EMPTY,
                EMPTY, reportLogEnable, false);
        logger.debug("locations reportData=\n" + ReportUtils.getPrettyPrint(reportData));
        getInstance().insertLog(reportFactory.getReportInfo(),
                reportDefinition,
                httpRequestUtil.getInfoRequestMap(),
                ConfigurationService.getAsLong(user.getActiveGroup(), REPORT_LOG_THRESHOLD),
                reportLogEnable,
                user.getId());
        IndexInformationMongoService.getInstance().updateLastRunDate(reportFactory.getIndexName(), new Date());
        ReportAppService.instance().logEnd(reportDefinition, start, Constants.UI_REPORT);
        commit();
        return RestUtils.sendOkResponse(reportData, false);
    }

    @POST
    @Path("/timeSeriesReport/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 4, value = "Run a Time Series report")
    public Response executeSeriesReport(@PathParam("id") Long id, Map<String, Object> body,
                                        @QueryParam("startDate") Long startDate,
                                        @QueryParam("endDate") Long endDate,
                                        @QueryParam("relativeDate") String relativeDate,
                                        @QueryParam("pageSize") Integer pageSize,
                                        @QueryParam("pageNumber") Integer pageNumber,
                                        @Context HttpServletRequest request) {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();

        Map<String, Object> mapResponse;
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        long start = System.currentTimeMillis();
        HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
        ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), Constants
                .UI_REPORT);

        if (pageSize == null) {
            pageSize = ReportExecutionUtils.getPageSizeByDefault(reportDefinition, MAX_REPORT_RECORDS);
        }
        if (pageNumber == null) {
            pageNumber = 1;
        }

        DateHelper dateHelper = getDateHelper(relativeDate, startDate, endDate, new Date());

        mapResponse = ReportUtils.executeTimeSeriesReport(reportDefinition, body, dateHelper.from(), dateHelper.to(),
                pageSize,
                pageNumber);
        ReportAppService.instance().logEnd(reportDefinition, start, Constants.UI_REPORT);
        commit();
        return RestUtils.sendOkResponse(mapResponse);
    }

    /**
     * Runs a time series report
     */
    @POST
    @Path("/timeSeriesReportTable/{id}")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM})
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 5, value = "Run a Time Series report")
    public Response executeTimeSeriesReportTable(@PathParam("id") Long id,
                                                 Map<String, Object> body,
                                                 @QueryParam("startDate") Long startDate,
                                                 @QueryParam("endDate") Long endDate,
                                                 @DefaultValue("false") @QueryParam("export") Boolean export,
                                                 @QueryParam("relativeDate") String relativeDate,
                                                 @QueryParam("pageSize") Integer pageSize,
                                                 @QueryParam("pageNumber") Integer pageNumber,
                                                 @QueryParam("version") int version,
                                                 @Context HttpServletRequest request) throws Exception {

        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        String module = Constants.UI_REPORT;
        if (export) {
            module = Constants.EXPORT_REPORT;
        }
        TimerUtil tu = new TimerUtil();
        tu.mark();

        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        long start = System.currentTimeMillis();
        HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
        ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), module);

        EntityVisibility cVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(cVisibility, reportDefinition);

        DateHelper dh = getRelativeDateHelper(relativeDate, startDate, endDate, new Date());

        if (pageSize == null) {
            pageSize = 100;
        } else {
            if (pageSize == -1) {
                pageSize = 1 << 30;
            }
        }
        if (pageNumber == null) {
            pageNumber = 1;
        }

        tu.mark();
        logger.info("mark1 delt=" + tu.getLastDelt());

        Map<String, Object> reportData;

        logger.info("version=" + version);
        logger.info("rd.name=" + reportDefinition.getName());

        Date now = new Date();

        ReportFactory reportFactory = new ReportFactory(getDateFormatAndTimeZone());
        if (export) {
            try {
                Response exportDataResponse = getFileWithResults(
                        body, pageSize, pageNumber, reportDefinition, dh, now, reportFactory,
                        httpRequestUtil.getInfoRequestString());
                ReportAppService.instance().logEnd(reportDefinition, start, module);
                commit();
                return exportDataResponse;
            } catch (Exception e) {
                logger.error("Report " + id + " :" + e.getMessage(), e);
                ReportAppService.instance().logEnd(reportDefinition, start, module);
                return RestUtils.sendResponseWithCode(e.getMessage(), 500);
            }
        } else {
            // Getting user
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            boolean reportLogEnable = getAsBoolean(user.getActiveGroup(), REPORT_LOG_ENABLE);
            reportData = reportFactory.getResult(
                    reportDefinition,
                    body,
                    pageNumber,
                    pageSize,
                    dh.from(),
                    dh.to(),
                    now,
                    true,
                    httpRequestUtil.getInfoRequestString(),
                    request.getRequestURL().toString(), request.getContextPath(),
                    reportLogEnable);
            logger.debug("REPORT BEGIN\n" + ReportUtils.getPrettyPrint(reportData) + "\nREPORT END");
            ReportAppService.instance().logEnd(reportDefinition, start, module);
            getInstance().insertLog(reportFactory.getReportInfo(),
                    reportDefinition,
                    httpRequestUtil.getInfoRequestMap(),
                    ConfigurationService.getAsLong(user.getActiveGroup(), REPORT_LOG_THRESHOLD),
                    reportLogEnable,
                    user.getId());
            IndexInformationMongoService.getInstance().updateLastRunDate(reportFactory.getIndexName(), new Date());
            commit();
            return RestUtils.sendOkResponse(reportData);
        }
    }

    @SuppressWarnings("unchecked")
    @POST
    @Path("/tableSummary/{id}")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 6, value = "Run a Time Series report")
    public Response executeTableSummaryReport(@PathParam("id") Long id,
                                              @DefaultValue("false") @QueryParam("export") Boolean export,
                                              Map<String, Object> body,
                                              @QueryParam("startDate") Long startDate,
                                              @QueryParam("endDate") Long endDate,
                                              @QueryParam("relativeDate") String relativeDate,
                                              @QueryParam("pageSize") Integer pageSize,
                                              @QueryParam("pageNumber") Integer pageNumber,
                                              @QueryParam("version") int version,
                                              @Context HttpServletRequest request) throws Exception {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        String module = Constants.UI_REPORT;
        if (export) {
            module = Constants.EXPORT_REPORT;
        }
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        long start = System.currentTimeMillis();
        HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
        ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), module);

        EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);

        DateHelper dateHelper = getRelativeDateHelper(relativeDate, startDate, endDate, new Date());

        Map<String, Object> reportData = Collections.EMPTY_MAP;

        logger.info("version=" + version);
        logger.info("rd.name=" + reportDefinition.getName());

        Date now = new Date();
        ReportFactory reportFactory = new ReportFactory(getDateFormatAndTimeZone());
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        boolean reportLogEnable = getAsBoolean(user.getActiveGroup(), REPORT_LOG_ENABLE);
        if (export) {
            try {
                File file = reportFactory.getFileResult(reportDefinition, body, pageNumber, pageSize, dateHelper.from(),
                        dateHelper.to(), now, false, EMPTY);
                return convertToFormatCsv(file);
            } catch (Exception e) {
                logger.error("executeTableSummaryReport", e);
            }
        } else {
            reportData = reportFactory.getResult(reportDefinition, body, pageNumber, pageSize, dateHelper.from(),
                    dateHelper.to(), now, reportLogEnable);
        }
        getInstance().insertLog(reportFactory.getReportInfo(), reportDefinition, httpRequestUtil.getInfoRequestMap(),
                ConfigurationService.getAsLong(user.getActiveGroup(), REPORT_LOG_THRESHOLD), reportLogEnable, user
                        .getId());
        IndexInformationMongoService.getInstance().updateLastRunDate(reportFactory.getIndexName(), new Date());
        ReportAppService.instance().logEnd(reportDefinition, start, module);
        commit();
        return RestUtils.sendOkResponse(reportData);
    }

    private Response convertToFormatCsv(File file) {
        Response.ResponseBuilder rb = Response.ok(file, MediaType.APPLICATION_OCTET_STREAM);
        rb.header("Content-Disposition", "attachment; filename=\"report.csv\"");
        rb.header("Content-Length", file.length());
        return rb.build();
    }

    @POST
    @Path("/mongo/{id}")
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    //@Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 7
            , value = "Run a raw mongo report"
            , notes = "This method get the results of a Mongo Report (Table Script)")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 201, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response executeMongoReport(@ApiParam(value = "ID of report Definition")
                                       @PathParam("id") Long id,
                                       Map<String, Object> values,
                                       @ApiParam(value = "Boolean value true/false to get CSV file or not")
                                       @DefaultValue("false") @QueryParam("export") Boolean export,
                                       @ApiParam(value = "Page Size")
                                       @QueryParam("pageSize") Integer pageSize,
                                       @ApiParam(value = "Page Number")
                                       @QueryParam("pageNumber") Integer pageNumber,
                                       @ApiParam(value = "Start Date")
                                       @QueryParam("startDate") Long startDate,
                                       @ApiParam(value = "end Date")
                                       @QueryParam("endDate") Long endDate,
                                       @ApiParam(value = "now")
                                       @QueryParam("now") Date now,
                                       @ApiParam(value = "relative Date")
                                       @QueryParam("relativeDate") String relativeDate,
                                       @Context HttpServletRequest request) {

        logger.debug("id='" + id + "'");
        logger.debug("export='" + export + "'");
        logger.debug("pageSize='" + pageSize + "'");
        logger.debug("pageNumber='" + pageNumber + "'");

        Map<String, Object> reportData;
        try {
            ReportDefinitionController rdc = new ReportDefinitionController();
            rdc.validateListPermissions();
            String module = Constants.UI_REPORT;
            if (export) {
                module = Constants.EXPORT_REPORT;
            }

            getVisibilityGroup(ReportDefinition.class.getCanonicalName(), null);

            ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
            if (reportDefinition == null) {
                return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
            }
            long start = System.currentTimeMillis();
            HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, values);
            ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), module);

            EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
            GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);


            logger.info("rd.name=" + reportDefinition.getName());
            if (now == null) {
                now = new Date();
            }

            DateHelper dateHelper = getRelativeDateHelper(relativeDate, startDate, endDate, new Date());
            ReportFactory factory = new ReportFactory(getDateFormatAndTimeZone());
            if (export) {
                Response exportDataResponse = getFileWithResults(
                        values, pageSize, pageNumber, reportDefinition, dateHelper, now, factory,
                        httpRequestUtil.getInfoRequestString());
                ReportAppService.instance().logEnd(reportDefinition, start, module);
                commit();
                return exportDataResponse;
            } else {
                reportData = factory.getResult(reportDefinition, values, pageNumber, pageSize, dateHelper.from(),
                        dateHelper.to(), now, true, httpRequestUtil.getInfoRequestString(), request.getRequestURL()
                                .toString(), request.getContextPath(), false);
                ReportAppService.instance().logEnd(reportDefinition, start, module);
                commit();
                return RestUtils.sendOkResponse(reportData);
            }
        } catch (IOException ie) {
            logger.error("IOException " + ie.getMessage(), ie);
            return RestUtils.sendResponseWithCode(ie.getMessage(), 500);
        } catch (UserException e) {
            logger.error("UserException " + e.getMessage(), e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        } catch (Exception e) {
            logger.error("Exception " + e.getMessage(), e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 500);
        } finally {
            commit();
        }
    }

    @POST
    @Path("/mapSummary/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 8, value = "Map Summary Group by Zone")
    public Response executeMapSummary(@PathParam("id") Long id, Map<String, Object> body,
                                      @QueryParam("startDate") Long startDate,
                                      @QueryParam("endDate") Long endDate,
                                      @QueryParam("relativeDate") String relativeDate,
                                      @QueryParam("pageSize") Integer pageSize,
                                      @QueryParam("pageNumber") Integer pageNumber,
                                      @Context HttpServletRequest request) throws Exception {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        long start = System.currentTimeMillis();
        HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
        ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), Constants
                .UI_REPORT);

        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();


        User user = (User) SecurityUtils.getSubject().getPrincipal();
        Group activeGroup = user.getActiveGroup();

        boolean reportLogEnable = getAsBoolean(activeGroup, REPORT_LOG_ENABLE);
        Long threshold = ConfigurationService.getAsLong(activeGroup, REPORT_LOG_THRESHOLD);


        Date now = new Date();

        // Calculate Relative Date
        DateHelper dh = getRelativeDateHelper(relativeDate, startDate, endDate, now);
        ReportFactory reportFactory = new ReportFactory(getDateFormatAndTimeZone());
        Map<String, Object> result = reportFactory.getResult(reportDefinition,
                body,
                pageNumber,
                pageSize,
                dh.from(),
                dh.to(),
                now,
                reportLogEnable);
        logger.debug("RESULT \n" + ReportUtils.getPrettyPrint(result));
        ReportAppService.instance().logEnd(reportDefinition, start, Constants.UI_REPORT);
        logger.info("Report Log Parameters: " + REPORT_LOG_ENABLE + " = " + reportLogEnable + " " +
                REPORT_LOG_THRESHOLD + " = " + threshold);
        getInstance().insertLog(reportFactory.getReportInfo(), reportDefinition,
                httpRequestUtil.getInfoRequestMap(), threshold, reportLogEnable, user.getId());
        IndexInformationMongoService.getInstance().updateLastRunDate(reportFactory.getIndexName(), new Date());
        commit();
        return RestUtils.sendOkResponse(result, false);
    }

    @POST
    @Path("/mapSummary/{id}/summaryByZone/{zoneId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 9, value = "Map Summary for a specific zone")
    public Response executeMapSummaryByZone(@PathParam("id") Long id,
                                            @PathParam("zoneId") Long zoneId,
                                            Map<String, Object> body,
                                            @QueryParam("startDate") Long startDate,
                                            @QueryParam("endDate") Long endDate,
                                            @QueryParam("relativeDate") String relativeDate,
                                            @QueryParam("pageSize") Integer pageSize,
                                            @QueryParam("pageNumber") Integer pageNumber,
                                            @Context HttpServletRequest request) throws Exception {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();

        logger.info("Starting mapSummary by Zone in Thing");

        User user = (User) SecurityUtils.getSubject().getPrincipal();
        Long intervalTimeToRefresh = ConfigurationService.getAsLong(user, "reportTimeOutCache");
        intervalTimeToRefresh = intervalTimeToRefresh != null ? intervalTimeToRefresh : 15000;

        logger.info("Report Time Caching: " + intervalTimeToRefresh);

        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        long start = System.currentTimeMillis();
        HttpRequestUtil httpRequestUtil = new HttpRequestUtil(request, body);
        ReportAppService.instance().logStart(reportDefinition, httpRequestUtil.getInfoRequestJSON(), Constants
                .UI_REPORT);
        body.put("zoneId", zoneId);
        Date now = new Date();
        ReportFactory reportFactory = new ReportFactory(getDateFormatAndTimeZone());
        boolean reportLogEnable = getAsBoolean(user.getActiveGroup(), REPORT_LOG_ENABLE);
        Map<String, Object> resultMap = reportFactory.getResult(Constants.REPORT_TYPE_MAP_SUMMARY_BY_ZONE,
                reportDefinition, body, pageNumber, pageSize, null, null, now, true, httpRequestUtil
                        .getInfoRequestString(), "", "", reportLogEnable, false);
        logger.debug("JSON RESULT NOW: " + ReportUtils.getPrettyPrint(resultMap));
        getInstance().insertLog(reportFactory.getReportInfo(), reportDefinition,
                httpRequestUtil.getInfoRequestMap(), ConfigurationService.getAsLong(user.getActiveGroup(),
                        REPORT_LOG_THRESHOLD), reportLogEnable, user.getId());
        IndexInformationMongoService.getInstance().updateLastRunDate(reportFactory.getIndexName(), new Date());
        ReportAppService.instance().logEnd(reportDefinition, start, Constants.UI_REPORT);
        commit();
        return RestUtils.sendOkResponse(resultMap);
    }

    @GET
    @Path("/zoneGroup/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 10, value = "Get a List of ZoneGroups (AUTO)")
    public Response listZoneGroups(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer
            pageNumber,
                                   @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                           ("extra") String extra,
                                   @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long
                                           visibilityGroupId,
                                   @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                   @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam
                                           ("topId") String topId, @ApiParam(value = "Extends nested properties")
                                   @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested " +
            "properties") @QueryParam("project") String project) {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        ZoneGroupController zgc = new ZoneGroupController();
        return zgc.listZoneGroups(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }

    @GET
    @Path("/zone/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 11, value = "Get a List of Zones (AUTO)")
    public Response listZone(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                             @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                     ("extra") String extra,
                             @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long
                                     visibilityGroupId,
                             @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                             @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam
                                     ("topId") String topId, @ApiParam(value = "Extends nested properties")
                             @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested " +
            "properties") @QueryParam("project") String project) {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        ZoneController zc = new ZoneController();
        return zc.listZones(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }

    @GET
    @Path("/localMap/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 12, value = "Get a List of Local Maps (AUTO)")
    public Response listLocalMaps(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer
            pageNumber,
                                  @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                          ("extra") String extra,
                                  @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long
                                          visibilityGroupId,
                                  @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                  @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @QueryParam
                                          ("topId") String topId, @ApiParam(value = "Extends nested properties")
                                  @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested " +
            "properties") @QueryParam("project") String project) {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        LocalMapController lm = new LocalMapController();
        return lm.listLocalMaps(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }

    @GET
    @Path("/zone/geojson")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 13, value = "Get a List of Zones in GeoJson")
    public Response listZonesGeoJson(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer
            pageNumber,
                                     @QueryParam("order") String order, @QueryParam("where") String where,
                                     @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("")
                                     @QueryParam("upVisibility") String upVisibility,
                                     @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
                                     @QueryParam("topId") String topId) {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        ZoneController zc = new ZoneController();
        return zc.listZonesGeoJson(pageSize, pageNumber, order, where, visibilityGroupId, upVisibility, downVisibility);
    }

    /**
     * Gets property values from a thing that is define by a report definition.
     *
     * @param id      the report definition that contains the property values to
     *                display
     * @param thingId the thing that contains the values
     * @return property values define by the report definition that are
     * associated to a thing
     */
    @SuppressWarnings("deprecation")
    @GET
    @Path("/{id}/thing/{thingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    //@RequiresPermissions(value = { "reportDefinition:r:{id}" })
    @ApiOperation(position = 14, value = "From a report get thing information")
    public Response selectReportDefinitions(@PathParam("id") Long id, @PathParam("thingId") Long thingId) {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);

        EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);

        List<ReportProperty> properties = reportDefinition.getReportProperty();
        List<String> reportProperties = new ArrayList<>();
        for (ReportProperty property : properties) {
            reportProperties.add(property.getPropertyName());
        }

        ThingEx thingEx = ThingExService.getInstance().get(thingId, reportProperties);

        return RestUtils.sendOkResponse(thingEx.publicMap());
    }

    /**
     * This report gets a thing that based on the specs of report definition
     *
     * @param thingId id of the thing
     * @return property values define by the report definition that are
     * associated to a thing
     */
    @GET
    @Path("/{id}/ThingByReport/{thingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 15, value = "Select Thing by Report")
    public Response getThingByReport(@PathParam("id") Long id, @PathParam("thingId") Long thingId, @QueryParam
            ("extra") String extra,
                                     @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested " +
            "properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties")
                                     @QueryParam("project") String project) {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();

        ThingController thingController = new ThingController();
        Group visibilityGroup = getVisibilityGroup(ReportDefinition.class.getCanonicalName(), null);
        getOverrideVisibilityCache().put(Thing.class.getCanonicalName(), visibilityGroup);
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        // TODO AGG CHECK VISIBILITY
        EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition, visibilityGroup);
        return thingController.selectThings(thingId, extra, only, extend, project, false);
    }

    /**
     * This report deletes a thing that based on the specs of report definition
     *
     * @param thingId id of the thing
     * @return property values define by the report definition that are
     * associated to a thing
     */
    @DELETE
    @Path("/{id}/deleteThingByReport/{thingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 16, value = "Delete Thing by Report")
    public Response deleteThingByReport(@PathParam("id") Long id, @PathParam("thingId") Long thingId) {
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        ThingController thingController = new ThingController();
        Group visibilityGroup = getVisibilityGroup(ReportDefinition.class.getCanonicalName(), null);
        getOverrideVisibilityCache().put(Thing.class.getCanonicalName(), visibilityGroup);
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        boolean massiveProcesss = ReportAppService.instance().isMassiveProcessRunning();
        if (massiveProcesss) {
            return RestUtils.sendResponseWithCode("Report has bulk process in progress", 400);
        }
        Thing thing = ThingService.getInstance().get(thingId);
        if (thing == null) {
            return RestUtils.sendBadResponse(String.format("ThingId[%d] not found", thingId));
        }
        // TODO AGG CHECK VISIBILITY
        EntityVisibility entityVisibility = new ThingController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, thing);
        return thingController.deleteThing(thingId);
    }

    private DateHelper getDateHelper(String relativeDate, Long startDate, Long endDate, Date now) {
        if (relativeDate != null && RelativeDateUtil.isValidRelativeDateCode(relativeDate)) {
            return new DateHelper(relativeDate, now);
        } else {
            return isEmpty(relativeDate) || relativeDate.equals("0/0/0") ?
                    new DateHelper.Builder().range(startDate, endDate).build() :
                    new DateHelper.Builder().relative(relativeDate).build();
        }
    }

    private DateHelper getRelativeDateHelper(String relativeDate, Long startDate, Long endDate, Date now) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        String offsetTimeZone = UserService.getInstance().getValueRegionalSettings(user, Constants.TIME_ZONE_CONFIG);
        if (relativeDate != null && RelativeDateUtil.isValidRelativeDateCode(relativeDate)) {
            if (startDate != null) {
                now = new Date(startDate);
            }
            return new DateHelper(relativeDate, now,
                    offsetTimeZone);
        } else {
            return isEmpty(relativeDate) || relativeDate.equals("0/0/0")
                    ? new DateHelper.Builder().range(startDate, endDate).timeZone(offsetTimeZone).build()
                    : new DateHelper.Builder().relative(relativeDate).timeZone(offsetTimeZone).relativePast(true)
                    .build();
        }
    }

    private DateFormatAndTimeZone getDateFormatAndTimeZone() {
        return UserService.getInstance().getDateFormatAndTimeZone((User) SecurityUtils.getSubject().getPrincipal());
    }

    /**
     * This method creates a thing that based on the specs of report definition
     *
     * @param id       the report definition that contains the property values to
     *                 display
     * @param thingMap the thing in {@link Map} format that contains the values of the thing WHEN group.id
     *                 should be of the reportDefinition.
     * @return property values define by the report definition that are
     * associated to a thing
     */
    @PUT
    @Path("/{id}/createThing")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 17, value = "Create Thing by Report")
    @SuppressWarnings("unchecked")
    public Response createThingByReport(@PathParam("id") Long id, Map<String, Object> thingMap) {
        Group visibilityGroup = getVisibilityGroup(ReportDefinition.class.getCanonicalName(), null);
        getOverrideVisibilityCache().put(Thing.class.getCanonicalName(), visibilityGroup);
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }

        //TODO: create the same permissions that Report Definition Controller,Victor Angel Chambi Nina, 21/08/2017
        //TODO: It is a bad idea call from a controller to another controller.
        EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);

        try {

            ThingTypeService.getInstance().validateFormulaFields(
                    (String) thingMap.get("thingTypeCode"),
                    ((Map<String, Object>) thingMap.get("udfs")).keySet());

            if (thingMap.get("name") == null) {
                thingMap.put("name", thingMap.get("serialNumber"));
            }

            // Create a new Thing
            Stack<Long> recursivelyStack = new Stack<>();
            return RestUtils.sendOkResponse(ThingsService.getInstance().create(recursivelyStack,
                    (String) thingMap.get("thingTypeCode"),
                    (String) thingMap.get("group"),
                    (String) thingMap.get("name"),
                    (String) thingMap.get("serialNumber"),
                    (Map<String, Object>) thingMap.get("parent"),
                    (Map<String, Object>) thingMap.get("udfs"),
                    thingMap.get("children"),
                    thingMap.get("childrenUdf"),
                    true,
                    true,
                    new Date(),
                    true,
                    true));
        } catch (UserException e) {
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    /**
     * This method creates a thing that based on the specs of report definition
     *
     * @param id       The report definition that contains the property values to
     *                 display
     * @param thingId  The thing type id to update.
     * @param thingMap the thing in {@link Map} format that contains the values of the thing WHEN group.id
     *                 should be of the reportDefinition.
     * @return property values define by the report definition that are
     * associated to a thing
     */
    @PATCH
    @Path("/{id}/updateThing/{thingId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 18, value = "Bulk Insert of Things")
    @SuppressWarnings("unchecked")
    public Response updateThingByReport(@PathParam("id") Long id,
                                        @PathParam("thingId") Long thingId,
                                        Map<String, Object> thingMap) {

        Group visibilityGroup = getVisibilityGroup(ReportDefinition.class.getCanonicalName(), null);
        getOverrideVisibilityCache().put(Thing.class.getCanonicalName(), visibilityGroup);

        //TODO: Verify this code, Victor Angel Chambi Nina, 25/08/2017.
        ReportDefinitionController rdc = new ReportDefinitionController();
        rdc.validateListPermissions();
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }

        if (ReportAppService.instance().isMassiveProcessRunning()) {
            return RestUtils.sendResponseWithCode("Report has bulk process in progress", 400);
        }

        try {
            // Create a new Thing
            Stack<Long> recursivelyStack = new Stack<>();
            return RestUtils.sendOkResponse(ThingService.getInstance().update(
                    recursivelyStack,
                    thingId,
                    (String) thingMap.get("thingTypeCode"),
                    (String) thingMap.get("group"),
                    (String) thingMap.get("name"),
                    (String) thingMap.get("serialNumber"),
                    (Map<String, Object>) thingMap.get("parent"),
                    (Map<String, Object>) thingMap.get("udfs"),
                    thingMap.get("children"),
                    null,
                    true,
                    true,
                    new Date(),
                    false,
                    currentUser,
                    true));
        } catch (UserException e) {
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    @SuppressWarnings("unchecked")
    @PATCH
    @Path("/{id}/thing/bulkUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    //@RequiresPermissions(value = { "reportDefinition:r:{id}" })
    @ApiOperation(position = 19, value = "Bulk Update of Things")
    public Response bulkUpdateThing(@PathParam("id") Long id, Map<String, Object> thingBulkMap) {
        Map<String, Object> result = new HashMap<>();
        Group visibilityGroup = getVisibilityGroup(ReportDefinition.class.getCanonicalName(), null);
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        getOverrideVisibilityCache().put(Thing.class.getCanonicalName(), visibilityGroup);
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }

        if (ReportAppService.instance().isMassiveProcessRunning()) {
            return RestUtils.sendResponseWithCode("Report has bulk process in progress", 400);
        }
        EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);
        try {
            List things = (List) thingBulkMap.get("things");
            Map<String, Object> udfsThingBulkMap = (Map<String, Object>) thingBulkMap.get("udfs");
            Map<String, Object> udfsReportAppService = ReportAppService.instance().getLstUdfs(udfsThingBulkMap);
            if (things != null && things.size() > 0) {
                ThingService thingService = ThingService.getInstance();
                int quantityThing = 0;
                Map<String, Object> errorThing = new HashMap<>();
                List<String> errorSerial;
                String errorMessage;
                Date transactionDate = new Date();
                //Iterate each things requested to update
                for (Object objectThing : (List) thingBulkMap.get("things")) {
                    Long thingId = Long.parseLong(objectThing.toString());
                    Thing thing = ThingService.getInstance().getThingById(thingId);
                    for (Map.Entry<String, Object> entryUdfs : udfsReportAppService.entrySet()) {
                        if ((thing.getThingType().getId().toString().equals(entryUdfs.getKey())) || (entryUdfs.getKey
                                ().equals("0"))) {
                            //Do the map with the mandatory values for calling thingService.update method based on
                            // the id's of the things selected
                            Map<String, Object> thingMap = thingService.getMapRequestForBulkUpdate(thing,
                                    (Map<String, Object>) entryUdfs.getValue());
                            int udfsSize = ((Map<String, Object>) thingMap.get("udfs")).size();
                            if (udfsSize > 0) {
                                try {
                                    Stack<Long> recursivelyStack = new Stack<>();
                                    thingService.update(
                                            recursivelyStack,
                                            thing,
                                            (String) thingMap.get("thingTypeCode"),
                                            (String) thingMap.get("group"),
                                            (String) thingMap.get("name"),
                                            (String) thingMap.get("serialNumber"),
                                            null,
                                            (Map<String, Object>) thingMap.get("udfs"),
                                            null,
                                            null,
                                            true,
                                            true,
                                            transactionDate,
                                            false,
                                            null,
                                            null,
                                            false,
                                            false,
                                            currentUser, true);
                                    quantityThing++;
                                    errorMessage = null;
                                } catch (UserException e) {
                                    errorMessage = e.getMessage().replace("'" + thingMap.get("serialNumber").toString
                                            () + "'", "");
                                    logger.debug(thingMap.get("serialNumber")
                                            + "-" + thingMap.get("thingTypeCode")
                                            + "-" + e.getMessage());
                                }
                            } else {
                                errorMessage = "No changes detected, the edition was skipped.";
                            }
                            if (errorMessage != null) {
                                errorSerial = (List<String>) errorThing.get(errorMessage);
                                if (errorSerial == null) {
                                    errorSerial = new ArrayList<>();
                                }
                                if (errorSerial.size() < 25) {
                                    errorSerial.add(String.valueOf(thingMap.get("serialNumber")));
                                } else if (errorSerial.size() == 25) {
                                    errorSerial.add("And more...");
                                }
                                errorThing.put(errorMessage, errorSerial);
                            }
                        }
                    }
                }
                result.put("updated", quantityThing);
                result.put("selected", things.size());
                if (errorThing.size() > 0) {
                    result.put("failed", errorThing);
                }
            } else {
                return RestUtils.sendResponseWithCode("You have to enter things to update.", 400);
            }
        } catch (UserException e) {
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return RestUtils.sendOkResponse(result);
    }

    /****
     * Logic of Attachments
     ****/
    @POST
    @Path("/attachment/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 20, value = "Upload File Temporary, the Id's returned should be used in Create/Update " +
            "Thing in the specific UDF of type  'Attachment'")
    public Response uploadFile(
            @ApiParam(value = "File to be uploaded.") MultipartFormDataInput input
            , @ApiParam(value = "Additional comments of the file.")
            @QueryParam("comments") String comment
            , @ApiParam(value = "Operation over file, it accepts: 'override'")
            @QueryParam("operationOverFile") String operationOverFile
            , @ApiParam(value = "ID of the thing") @QueryParam("thingId") Long thingId
            , @ApiParam(value = "ID of the thingTypeFieldID") @QueryParam("thingTypeFieldId") Long thingTypeFieldId
            , @ApiParam(value = "Path of Attachments configured in the Thing Type") @QueryParam("pathAttachments")
                    String pathAttachments
            , @ApiParam(value = "Hierarchical name of the group") @QueryParam("hierarchicalNameGroup") String
                    hierarchicalNameGroup
            , @ApiParam(value = "ID's of temporary attachments (Optional)") @QueryParam("attachmentTempIds") String
                    attachmentTempIds
    ) {
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        try {
            //get the folder path of the attachments
            String folderAttachments = AttachmentService.getInstance().getPathDirectory(
                    pathAttachments
                    , hierarchicalNameGroup
                    , thingId
                    , thingTypeFieldId);
            //Save in temporary table
            return RestUtils.sendOkResponse(AttachmentService.getInstance().saveFileInTempDB(
                    comment
                    , user.getId().toString()
                    , input, operationOverFile
                    , folderAttachments
                    , attachmentTempIds));
        } catch (Exception e) {
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    @GET
    @Path("/attachment/download")
    @RequiresAuthentication
    @ApiOperation(position = 21, value = "Download File, it returns the data of the file in order to be downloaded in" +
            " UI")
    public Response getFile(
            @ApiParam(value = "Path of the file, example: D:\\logs\\log1.log")
            @QueryParam("pathFile") String pathFile) throws Exception {
        AttachmentController c = new AttachmentController();
        return c.getFile(pathFile);
    }

    /****
     * Logic of Attachments
     ****/

    private void commit() {
        logger.info("Commit in controller");
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        logger.info("Transaction");
        try {
            if (transaction.isActive()) {
                try {
                    //session.flush();
                    session.clear();
                    transaction.commit();
                    logger.info("transaction.commit() executed");
                } catch (NullPointerException npe) {
                    logger.warn("Nothing to clear in session");
                    HibernateDAOUtils.rollback(transaction);
                }
            }
        } catch (Exception e) {
            logger.error("Error in commit manually", e);
            if (transaction.isActive()) {
                logger.info("Active Rollback");
                HibernateDAOUtils.rollback(transaction);
            }
        }
    }

    @PATCH
    @Path("/{id}/thing/massiveBulkProcess")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 22, value = "Bulk Process of Things (Update|Delete) ",
            notes = "This method creates an instance of bulk process for a bunch of Things gotten by \"filters\" and " +
                    "<br>" +
                    "changes are done over list of \"udfs\"<br>"
                    + "<font face=\"courier\">{\n<br>" +
                    " &nbsp;\"filters\": {\n<br>" +
                    " &nbsp;&nbsp;\"Thing Type\": \"1\",\n<br>" +
                    " &nbsp;&nbsp;\"relativeDate\": \"NOW\",\n<br>" +
                    " &nbsp;&nbsp;\"pageSize\": 15,\n<br>" +
                    " &nbsp;&nbsp;\"pageNumber\": 1\n<br>" +
                    " &nbsp;},\n<br>" +
                    " &nbsp;\"udfs\": {\n<br>" +
                    " &nbsp;&nbsp;\"eNode\": {\n<br>" +
                    " &nbsp;&nbsp;\"eNode\": {\n<br>" +
                    " &nbsp;&nbsp;\"value\": \"ENODE\",\n<br>" +
                    " &nbsp;&nbsp;\"thingTypeId\": 1\n<br>" +
                    " &nbsp;&nbsp;}\n<br>" +
                    " &nbsp;}\n" +
                    "  }\n" +
                    "}</font>")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 201, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            })
    public Response massiveBulkProcess(
            @ApiParam(value = "Report Definition ID") @PathParam("id") Long id,
            @ApiParam(value = "Filter Start Date") @QueryParam("startDate") Long startDate,
            @ApiParam(value = "Filter End Date") @QueryParam("endDate") Long endDate,
            @ApiParam(value = "Page Number") @QueryParam("pageNumber") Integer pageNumber,
            @ApiParam(value = "Page Size of the Report, put -1 for all registers") @QueryParam("pageSize") Integer
                    pageSize,
            @ApiParam(value = "Relative Date Code. Example: LAST_WEEK") @QueryParam("relativeDate") String relativeDate,
            @ApiParam(value = "Actual Date") @QueryParam("now") Date now,
            @ApiParam(value = "Check JSON example in notes of the End Point") Map<String, Object> body,
            @ApiParam(value = "Massive Process Type: UPDATE, DELETE") @QueryParam("typeProcess") String typeProcess,
            @ApiParam(value = "Total records for being affected by Massive Process") @QueryParam("totalRecords") Long
                    totalRecords) {
        try {

            logger.debug("id='" + id + "'");
            logger.debug("startDate='" + startDate + "'");
            logger.debug("endDate='" + endDate + "'");
            logger.debug("pageSize='" + pageSize + "'");
            logger.debug("relativeDate='" + relativeDate + "'");
            logger.debug("pageNumber='" + pageNumber + "'");
            logger.debug("body='" + body + "'");
            logger.debug("typeProcess='" + typeProcess + "'");
            logger.debug("totalRecords='" + totalRecords + "'");

            if (id != null) {
                ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
                if (reportDefinition == null) {
                    return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found.", id));
                }
                EntityVisibility entityVisibility = new ReportDefinitionController().getEntityVisibility();
                GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);
                if (now == null) {
                    now = new Date();
                }
                DateHelper dateHelper = getRelativeDateHelper(relativeDate, startDate, endDate, now);
                Subject subject = SecurityUtils.getSubject();
                User currentUser = (User) subject.getPrincipal();
                //Execute logic of massive bulk process
                Map<String, Object> result = ReportAppService.instance().massiveBulkProcess(reportDefinition,
                        typeProcess,
                        dateHelper,
                        now,
                        currentUser,
                        pageNumber,
                        pageSize,
                        body,
                        totalRecords);
                commit();
                return RestUtils.sendOkResponse(result);
            } else {
                commit();
                return RestUtils.sendBadResponse("ReportDefinitionId should be valid.");
            }
        } catch (UserException e) {
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    @POST
    @Path("/{id}/actionExecution/{actionId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 23, value = "Execute action by ID",
            notes = "<h3>Example execution 1</h3>\n" +
                    "<p>{\n" +
                    "    <br /> &emsp; \"body\": 1 \n" +
                    "    <br />}</p>" +
                    "<h3>Example execution 2</h3>\n" +
                    "<p>{\n" +
                    "    <br /> &emsp; \"body\": \"text\" \n" +
                    "    <br />}</p>" +
                    "<h3>Example execution 3</h3>\n" +
                    "<p>{\n" +
                    "    <br /> &emsp; \"body\": {\"property\":\"value\"} \n" +
                    "    <br />}</p>" +
                    "<h3>Example execution 4</h3>\n" +
                    "<p>{\n" +
                    "    <br /> &emsp; \"body\": [ {\"property\":\"value1\"} , {\"property\":\"value2\"} ] \n" +
                    "    <br />}</p>")
    public Response executeAction(@ApiParam(value = "report definition ID")
                                  @PathParam("id") Long reportId,
                                  @ApiParam(value = "action configuration ID")
                                  @PathParam("actionId") Long actionId,
                                  Map<String, Object> body) {
        ActionConfiguration actionConfiguration = ActionConfigurationService.getInstance()
                .getActionConfigurationActive(actionId);
        if (actionConfiguration == null) {
            return RestUtils.sendBadResponse(String.format("ActionConfigurationId[%d] not found", actionId));
        }
        ReportActions reportActions = ReportActionsService.getInstance().getReportActionByActionIDandReportID
                (actionId, reportId);
        if (reportActions == null) {
            return RestUtils.sendBadResponse(String.format("ActionConfigurationId[%d] and ReportDefinitionId[%d] not " +
                    "found", actionId, reportId));
        }
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        LogExecutionActionService logExecutionActionService = LogExecutionActionService.getInstance();
        String actionCodeResponse = logExecutionActionService.executeAction(actionConfiguration, getBody(body), user, reportId);
        return RestUtils.sendOkResponse(actionCodeResponse);
    }

    private String getBody(Map<String, Object> map) {
        Object body = map.get("body");
        if (body instanceof Map) {
            JSONObject jsonObject = new JSONObject((Map) body);
            return jsonObject.toJSONString();
        }
        return String.valueOf(body);
    }

    /**
     * Method to export results to CSV
     *
     * @param body             Dynamic Filters
     * @param pageSize         Page Size of report.
     * @param pageNumber       Page Number of report
     * @param reportDefinition report Definition ID
     * @param dh               Date helper.
     * @param now              current date
     * @param factory          Report factory
     * @param requestInfo      Request Info, IP, user, token...etc in String , NO JSON String
     * @return Instance of {@link Response}.
     * @throws Exception If exception exists.
     */
    private Response getFileWithResults(Map<String, Object> body, Integer pageSize, Integer pageNumber,
                                        ReportDefinition reportDefinition,
                                        DateHelper dh, Date now, ReportFactory factory, String requestInfo) throws
            Exception {
        File file = factory.getFileResult(reportDefinition, body, pageNumber, pageSize, dh.from(), dh.to(), now,
                false, requestInfo);
        Response.ResponseBuilder rb = Response.ok(file, MediaType.APPLICATION_OCTET_STREAM);
        rb.header("Content-Disposition", "attachment; filename=\"report.csv\"");
        rb.header("Content-Length", file.length());
        return rb.build();
    }
}
