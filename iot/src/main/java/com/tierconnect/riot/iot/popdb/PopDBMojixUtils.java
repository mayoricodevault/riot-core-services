package com.tierconnect.riot.iot.popdb;

import java.util.*;
import java.util.logging.Logger;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.RoleService;

import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.fmc.utils.FMCConstants;
import com.tierconnect.riot.iot.fmc.utils.FMCUtils;
//import com.tierconnect.riot.iot.services.FieldValueService;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;

/**
 *
 * @author tcrown
 *
 */
public class PopDBMojixUtils
{

    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( PopDBMojixUtils.class );

    /*
     * for some reason gus does not like the "correct" names in the base popdb
     */
    public static void modifyExistingRecords()
    {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        GroupService.getInstance().update( rootGroup );

        GroupType rootGroupType = GroupTypeService.getInstance().getRootGroupType();
        GroupTypeService.getInstance().update( rootGroupType );

        GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();
        tenantGroupType.setName( "Company" );
        GroupTypeService.getInstance().update( tenantGroupType );

        Role rootRole = RoleService.getInstance().getRootRole();
        rootRole.setName( "Root Administrator" );
        rootRole.setDescription( "Root Administrator" );
        RoleService.getInstance().update( rootRole );

        Role tenantAdminRole = RoleService.getInstance().getTenantAdminRole();
        tenantAdminRole.setName( "Company Administrator" );
        tenantAdminRole.setDescription("The Company Administrator" );
        RoleService.getInstance().update(tenantAdminRole);
    }

    public static ThingType popThingTypeClothingItem(Group group, String name) {
		ThingType tt = PopDBIOTUtils.popThingType( group, null, name );
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "size", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt, "color", "", "", ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "material", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt, "price", "dollar", "$", ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt, "category", "", "", ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "brand", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);

		return tt;
    }

	public static ThingType popThingTypeShippingOrder(Group group, String name) {
		ThingType tt = PopDBIOTUtils.popThingType(group, null, name);
		tt.setIsParent(true);
		ThingTypeService.getInstance().update(tt);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"countAsset",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_FORMULA.value),
				false,
				"${count(\"\",\"\")}",
				null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"countOpenAsset",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_FORMULA.value),
				false,
				"${count(\"asset_code\",\"status.value=Open\")}",
				null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"owner",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),
				true,
				null,
				null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"status",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),
				true,
				null,
				null);


		return tt;
	}

	public static ThingType popThingTypeAssetMultiLevel(Group group, String name, ThingType shippingOrder) {
		ThingType tt = PopDBIOTUtils.popThingType(group, null, name);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"status",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),
				false,
				null,
				null);
		ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,
				"shippingOrderField",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_THING_TYPE.value),
				false,
				"",
				null);
		thingTypeField.setDataTypeThingTypeId(shippingOrder.getId());
		ThingTypeFieldService.getInstance().update(thingTypeField);
		return tt;
	}

	public static ThingType popThingTypeTag(Group group, String name) {
		ThingType tt = PopDBIOTUtils.popThingType(group, null, name);

		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"active",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN.value),
				false,
				null,
				null);

		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"lastDetectTime",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value),
				false,
				null,
				null);

		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"lastLocateTime",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value),
				false,
				null,
				null);

		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"location",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT.value),
				false,
				null,
				null);

		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"locationXYZ",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ.value),
				false,
				null,
				null);

		ThingTypeFieldService.getInstance().insertThingTypeField(tt,
				"zone",
				"",
				"",
				ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value),
				false,
				null,
				null);
		return tt;
	}

	public static ThingType popThingTypeColour(Group group, String name, String code) {
		ThingType tt = PopDBIOTUtils.popThingType(group, null, name, code);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"Action","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"ColorCode","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"Description","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"node",null);
		return tt;
	}

	public static ThingType popThingTypeUpc(Group group, String name, String code) {
		ThingType tt = PopDBIOTUtils.popThingType(group, null, name, code);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"CreationDate","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"PrimarySizePosition","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"01",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"SVGCIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"P",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UPCNumber","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0787256",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"Price","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0001600",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"ReplenishmentIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"PrimarySizeDescription","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"1SIZE",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"PackHeight","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"190",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"StrokeNumber","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0020",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"CustomMadeIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"N",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"CouponIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"N",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"ReducedSuffix","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"A",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATpersentage2","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"08762",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"SecondarySizePosition","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"00",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"SecondarySizeIndex","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"00",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"PreviousItemNumber","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"06U",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"TransportPackQuantity","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0024",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"AgeRestriction","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"TVTunerIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"Y",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"BoxedHangIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"B",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"QuantityPerPack","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"036",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"DisplaySetQuantity","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0000",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"PrimarySizeIndex","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"01",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"ItemNumber","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"32C",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATBand2","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"PreReducedIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"A",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"RangeCode","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"97",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATpersentage1","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"05264",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"WayStatus","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"l",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"PackLength","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"720",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"ColourCode","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"A1",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATBand3","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"SecondarySizeDescription","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"TransportSetQuantity","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0000",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"TaxCode","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"Y",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"ServiceItemIndicator","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"Y",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"ReducedEffDate","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"21160620",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"Action","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"A",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"DepartmentNumber","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"T01",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATBand1","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATBand4","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"GarmenMultiplies","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0001",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"DisplayPackQuantity","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"0006",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATpersentage3","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"09313",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"UKVATpersentage4","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"01353",null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"StoreDate","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt,"PackWidth","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,"580",null);
		return tt;
	}

	public static ThingType popThingTypeTransaction(Group group, String name, String code) {
		ThingType tt = PopDBIOTUtils.popThingType(group, null, name, code);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"transactionStart","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"siteNo","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"naturalDate","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"upc","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"transactionEnd","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		ThingTypeFieldService.getInstance().insertThingTypeField(
				tt,"filenumber","","",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
				DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value),false,null,null);
		return tt;
	}

	public static ThingType popThingType(Group group, String name, String templateName, boolean autoCreate) {
		return popThingType( group,  name, name.toLowerCase().replace(" ", "_") + "_code",  templateName,  autoCreate);
	}

	public static ThingType popThingType(Group group, String name, String code, String templateName, boolean autoCreate) {
		ThingTypeTemplate template = null;
		try {
			template = ThingTypeTemplateService.getInstance().getByCode(templateName);
			System.out.println(templateName);
			System.out.println(template);
		} catch(NonUniqueResultException e) {
			logger.error("Error to get Template Object : "+templateName);
			throw new UserException("Error to get Template Object :" + templateName,e);
		}
		return ThingTypeService.getInstance().insertThingTypeAndFieldsWithTemplate(
				group, name, code, template,autoCreate);
	}



	public static ThingType popThingTypePeople( Group group , String name)
	{ 	ThingType tt = PopDBIOTUtils.popThingType( group, null, name );
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "aboardType", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "age", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "class", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "family", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), false,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "familyName", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "gender", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "nationality", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), false ,null, null);
		return tt;
	}

	public static ThingType popThingTypeAsset( Group group , String name)
	{ 	ThingType tt = PopDBIOTUtils.popThingType( group, null, name );
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_ADMINISTRATOR, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_CATEGORY_CODE, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField(tt, FMCUtils.SAP_CURRENT_LOCATION, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_MAIN_PLANT, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_MATERIAL_NUM, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_OWNER, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SERIAL_NUM, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYSTEM_STATUS, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_USER_STATUS, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_VALID_FROM_DATE, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_VALID_TO_DATE, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);

		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.EVENT_PLANT, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCConstants.FMC_SCAN_ZONE, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCConstants.FMC_STATUS, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.EVENT_USER, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.EVENT_TYPE, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.EVENT_TAG_ID, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.EVENT_TIME, "millisecond", "ms",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);

		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC_STATUS, "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.EVENT.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.AVAILABLE_RETRIES.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.RETRIES.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.TIME.toString(), "millisecond", "ms",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);

		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.PLANT.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.TAG_ID.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.SCAN_ZONE.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.USER.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);

		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.ERROR_MESSAGE.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.INPUT_MESSAGE.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_JSON.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, FMCUtils.SAP_SYNC.OUTPUT_MESSAGE.toString(), "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_JSON.value), true ,null, null);
		return tt;
	}

	public static ThingType popThingTypeAssetVizix( Group group , String name)
	{ 	ThingType tt = PopDBIOTUtils.popThingType( group, null, name );
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "SKU", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Owner Group", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_GROUP.value), true,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Class", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "SubClass", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Status", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		return tt;
	}

	public static ThingType popThingTypeProductVizix( Group group , String name,ThingType as,ThingType sku)
	{ 	ThingType tt = PopDBIOTUtils.popThingType( group, null, name );

		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Owner Group", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_GROUP.value), true,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Class", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "SubClass", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Status", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Price", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true ,null, null);
		ThingTypeField a=ThingTypeFieldService.getInstance().insertThingTypeField(tt, "Customers", "", "", ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_THING_TYPE.value), true, null, null);
		a.setDataTypeThingTypeId(as.getId());
		ThingTypeField b=ThingTypeFieldService.getInstance().insertThingTypeField(tt, "SKU", "", "", ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_THING_TYPE.value), true, null, null);
		b.setDataTypeThingTypeId(sku.getId());

		return tt;
	}

	public static ThingType popThingTypeFMC( Group group , String name)
	{
		ThingType tt = PopDBIOTUtils.popThingType( group, null, name );
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "lastDetectTime", "millisecond", "ms",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "lastLocateTime", "millisecond", "ms",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), false ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "location", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LONLATALT.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "locationXYZ", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_XYZ.value), true ,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "zone", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), true,null, null);

		return tt;
	}

	public static ThingType popThingTypeSKU( Group group , String name)
	{ 	ThingType tt = PopDBIOTUtils.popThingType( group, null, name );
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Min", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "Max", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), true,null, null);
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "BestSeller", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN.value), true,null, null);

		return tt;
	}

	public static ThingType popThingTypeCustomer( Group group , String name)
	{ 	ThingType tt = PopDBIOTUtils.popThingType( group, null, name );
		ThingTypeFieldService.getInstance().insertThingTypeField( tt, "zone", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), true,null, null);
		tt.setIsParent(true);

		return tt;
	}

	/*
	@Deprecated
	public static Thing instantiateRFIDTag( ThingType rfidtag, Thing parent, int i, String serial, Group group, User createdBy )
	{
		String sn = String.format( "%021d", i );
		Thing thi = ThingService.getInstance().insertOld( rfidtag, sn, sn, group, createdBy, parent );

		Set<ThingTypeField> fields = thi.getThingType().getThingTypeFields();
		for( ThingTypeField field : fields )
		{
			switch( field.getName().toString() )
			{
			case ("logicalReader"):
			{
				switch( i % 4 )
				{
				case (0):
				{
					FieldValueService.insert( thi.getId(), field.getId(),
							new Date(), "Stockroom", field.getTimeSeries() );
				}
					break;
				case (1):
				{
					FieldValueService.insert( thi.getId(), field.getId(),
							new Date(), "Salesfloor", field.getTimeSeries() );
				}
					break;
				case (2):
				{
					FieldValueService.insert( thi.getId(), field.getId(),
							new Date(), "Entrance", field.getTimeSeries() );
				}
					break;
				case (3):
				{
					FieldValueService.insert( thi.getId(), field.getId(),
							new Date(), "POS", field.getTimeSeries() );
				}
					break;
				}
			}
				;
				break;
			case ("lastDetectTime"):
				FieldValueService.insert( thi.getId(), field.getId(),
						new Date(), "" + System.currentTimeMillis(), field.getTimeSeries() );
				break;
			case ("lastLocateTime"):
				FieldValueService.insert( thi.getId(), field.getId(),
						new Date(), "" + System.currentTimeMillis(), field.getTimeSeries() );
				break;
			case ("eNode"):
				FieldValueService.insert( thi.getId(), field.getId(),
						new Date(), "x3ed9371", field.getTimeSeries() );
				break;
			}
		}
		
		return thi;
	}

	@Deprecated
	public static Thing instantiateClothingItem( ThingType jacketThingType, int i, String serial, String name, Group group, User createdBy )
	{
		Thing th = ThingService.getInstance().insertOld(jacketThingType, name, serial, group, createdBy, null);
		Set<ThingTypeField> fields1 = th.getThingType().getThingTypeFields();
		for( ThingTypeField field : fields1 )
		{
			switch( field.getName().toString() )
			{
			case ("size"):
			{
				if( i % 2 == 0 )
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Large", field.getTimeSeries() );
				}
				else
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "X-Large", field.getTimeSeries() );
				}
				break;

			}

			case ("color"):
				if( i % 2 == 0 )
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Black", field.getTimeSeries() );
				}
				else
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Gray", field.getTimeSeries() );
				}
				break;
			case ("material"):
			{
				if( i % 2 == 0 )
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Cotton", field.getTimeSeries() );
				}
				else
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Polyester", field.getTimeSeries() );
				}
				break;
			}
			case ("category"):
                if(jacketThingType.getName().equals("Jackets"))
				FieldValueService.insert( th.getId(), field.getId(),
						new Date(), "Jackets", field.getTimeSeries() );
                else FieldValueService.insert( th.getId(), field.getId(),
						new Date(), "Pants", field.getTimeSeries() );
				break;
			case ("brand"):
			{
				switch( i % 3 )
				{
				case (0):
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Ralph Lauren", field.getTimeSeries() );
					break;
				case (1):
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Calvin Klein", field.getTimeSeries() );
					break;
				case (2):
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Levi's", field.getTimeSeries() );
					break;
				}
				;
				break;

			}
			case ("price"):
			{
				if( i % 2 == 0 )
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "98.00", field.getTimeSeries() );
				}
				else
				{
					FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "99.99", field.getTimeSeries() );
				}
				break;
			}
			}
		}
		
		return th;
	}

    public static Thing instantiatePeople(ThingType people, String serial, String name, Group group, User createdBy)
    {
        Thing th = ThingService.getInstance().insertOld(people, name, serial, group, createdBy, null);
        Set<ThingTypeField> fields1 = th.getThingType().getThingTypeFields();
        for( ThingTypeField field : fields1 )
        {
            switch( field.getName().toString() )
            {
                case ("aboardType"):
                {
                    FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Large", field.getTimeSeries() );
                    break;
                }

                case ("age"):
                {
                    FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Black", field.getTimeSeries() );
                    break;
                }

                case ("class"):
                {
                    FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Cotton", field.getTimeSeries() );
                    break;
                }
                case ("family"):
                    FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Jackets", field.getTimeSeries() );
                    break;
                case ("familyName"):
                {
                    FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "Ralph Lauren", field.getTimeSeries() );
                    break;
                }
                case ("gender"):
                {
                    FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "98.00", field.getTimeSeries() );
                    break;
                }
                case ("nationality"):
                {
                    FieldValueService.insert( th.getId(), field.getId(),
							new Date(), "98.00", field.getTimeSeries() );
                    break;
                }
            }
        }

        return th;
    }
    */
}
