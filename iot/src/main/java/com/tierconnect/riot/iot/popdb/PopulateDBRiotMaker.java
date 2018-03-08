package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.utils.CFieldType;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by cfernandez
 * 10/22/2014.
 */
public class PopulateDBRiotMaker {

    public void install(){
        System.out.println("Populating Riot Maker install.....");
        populateCustomFiledTypes();
    }

    public void demo(){
        System.out.println("******* Start populating Riot Maker demo.....");
        populateCustomApplications();
        populateContactObject();
        populateSupplierObject();
        populatePartObject();
        populateDefectCategoryObject();
        populateTagTypeObject();
        populateShiftObject();
        populateTagObject();
        System.out.println("******* End populating Riot Maker demo.....");
    }

    private void populateShiftObject() {
        System.out.println("Populating object=Shift");

        Long shiftObjectTypeID = createCustomObjectType("ncm", "shift", "Shift", "This object is representing all shifts of the application");

        CustomFieldType fieldType_text = CustomFieldTypeService.getInstance().selectByName(CFieldType.TEXT.name());

        Long shiftCodeFieldID = createCustomField("shiftCode","Shift Code", fieldType_text.getId(), shiftObjectTypeID);
        Long shiftLabelFieldID = createCustomField("shiftLabel", "Shift Label", fieldType_text.getId(), shiftObjectTypeID);

        Long shiftInstanceID1 = createCustomObject(shiftObjectTypeID);
        createCustomFieldValue(shiftInstanceID1, shiftCodeFieldID, "1");
        createCustomFieldValue(shiftInstanceID1, shiftLabelFieldID, "1");

        Long shiftInstanceID2 = createCustomObject(shiftObjectTypeID);
        createCustomFieldValue(shiftInstanceID2, shiftCodeFieldID, "2");
        createCustomFieldValue(shiftInstanceID2, shiftLabelFieldID, "2");

        Long shiftInstanceID3 = createCustomObject(shiftObjectTypeID);
        createCustomFieldValue(shiftInstanceID3, shiftCodeFieldID, "3");
        createCustomFieldValue(shiftInstanceID3, shiftLabelFieldID, "3");

    }

    private void populateTagTypeObject() {
        System.out.println("Populating object=TagType");

        Long tagTypeObjectTypeID = createCustomObjectType("ncm", "tagType", "Tag Type", "This object is representing all tag types of the application");

        CustomFieldType fieldType_text = CustomFieldTypeService.getInstance().selectByName(CFieldType.TEXT.name());

        Long tagTypeCodeFieldID = createCustomField("tagTypeCode","Tag Type Code", fieldType_text.getId(), tagTypeObjectTypeID);
        Long tagTypeLabelFieldID = createCustomField("tagTypeLabel", "Tag Type Label", fieldType_text.getId(), tagTypeObjectTypeID);

        Long tagTypeInstanceID1 = createCustomObject(tagTypeObjectTypeID);
        createCustomFieldValue(tagTypeInstanceID1, tagTypeCodeFieldID, "scrap");
        createCustomFieldValue(tagTypeInstanceID1, tagTypeLabelFieldID, "Scrap (Plant Fault)");

        Long tagTypeInstanceID2 = createCustomObject(tagTypeObjectTypeID);
        createCustomFieldValue(tagTypeInstanceID2, tagTypeCodeFieldID, "reject");
        createCustomFieldValue(tagTypeInstanceID2, tagTypeLabelFieldID, "Reject (Supplier Fault)");

        Long tagTypeInstanceID3 = createCustomObject(tagTypeObjectTypeID);
        createCustomFieldValue(tagTypeInstanceID3, tagTypeCodeFieldID, "obsolete");
        createCustomFieldValue(tagTypeInstanceID3, tagTypeLabelFieldID, "Obsolete");
    }

    private void populateDefaultCustomFields() {
        CustomField customField = new CustomField();
        customField.setName("supplierCode");
        customField.setRequired(true);
        CustomFieldType customFieldType = CustomFieldTypeService.getInstance().get(2L);
        customField.setCustomFieldType(customFieldType);
        CustomObjectType customObjectType = CustomObjectTypeService.getInstance().get(1L);
        customField.setCustomObjectType(customObjectType);
        CustomFieldService.getInstance().insert(customField);
    }

    public void populateCustomApplications(){
        //createCustomApplication("fleet","Fleet");
        createCustomApplication("ncm","RIoT NCM", true);
        //populateGroupsNCMApplication();
    }

    public void populateSupplierObject(){
        System.out.println("Populating object=Supplier");

        // creating custom object type 'supplier'
        Long supplierObjectTypeID = createCustomObjectType("ncm", "supplier", "Supplier", "This object is representing all suppliers of the application");

        // creating fields for the customObjectType 'supplier'
        CustomFieldType fieldType_text = CustomFieldTypeService.getInstance().selectByName(CFieldType.TEXT.name());
        CustomFieldType fieldType_lookup = CustomFieldTypeService.getInstance().selectByName(CFieldType.LOOKUP.name());

        Long supplierCodeFieldID = createCustomField("supplierCode","Supplier Code", fieldType_text.getId(), supplierObjectTypeID);
        Long supplierNameFieldID = createCustomField("supplierName", "Supplier Name", fieldType_text.getId(), supplierObjectTypeID);

        // creating lookup Field to 'Contact'
        CustomObjectType contact = CustomObjectTypeService.getInstance().selectByCode("contact");
        CustomField customField = CustomFieldService.getInstance().selectByCode("contactPhoneNumber");
        Long displayFieldId = customField.getId();
        Long contactFieldID = createCustomField("contactPhone", "Contact Phone", fieldType_lookup.getId(), supplierObjectTypeID, contact.getId(), displayFieldId);

        List<CustomObject> contacts = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(contact.getId());
        int instanceNumber = contacts.size()-1;

        // creating custom objects (instances) of type supplier
        CustomObject contactInstance = contacts.get(instanceNumber);
        Long supplierInstanceID1 = createCustomObject(supplierObjectTypeID);
        createCustomFieldValue(supplierInstanceID1, supplierCodeFieldID, "SP00001");
        createCustomFieldValue(supplierInstanceID1, supplierNameFieldID, "Trim Design");
        createCustomFieldValue(supplierInstanceID1, contactFieldID, contactInstance.getId().toString());

        instanceNumber = instanceNumber - 1;
        contactInstance = contacts.get(instanceNumber);
        Long supplierInstanceID2 = createCustomObject(supplierObjectTypeID);
        createCustomFieldValue(supplierInstanceID2, supplierCodeFieldID,"SP00002");
        createCustomFieldValue(supplierInstanceID2, supplierNameFieldID, "COOPER STANDARD AUTOMOTIVE");
        createCustomFieldValue(supplierInstanceID2, contactFieldID, contactInstance.getId().toString());

        instanceNumber = instanceNumber - 1;
        contactInstance = contacts.get(instanceNumber);
        Long supplierInstanceID3 = createCustomObject(supplierObjectTypeID);
        createCustomFieldValue(supplierInstanceID3, supplierCodeFieldID,"SP00003");
        createCustomFieldValue(supplierInstanceID3, supplierNameFieldID, "TRW AUTOMOTIVE US LLC");
        createCustomFieldValue(supplierInstanceID3, contactFieldID, contactInstance.getId().toString());
    }

    public void populateDefectCategoryObject(){
        System.out.println("Populating object=defect categories");

        // creating custom object type 'defect category'
        Long defectObjectTypeID = createCustomObjectType("ncm", "defectCategory", "Defect Category", "This object is representing all defects of the application");

        // creating fields for the customObjectType 'defect category'
        CustomFieldType fieldType_text = CustomFieldTypeService.getInstance().selectByName(CFieldType.TEXT.name());
        Long defectCodeFieldID = createCustomField("defectCode","Code", fieldType_text.getId(), defectObjectTypeID);
        Long defectNameFieldID = createCustomField("defectName", "Name", fieldType_text.getId(), defectObjectTypeID);

        // creating custom objects (instances) of type defect category
        Long supplierInstanceID1 = createCustomObject(defectObjectTypeID);
        createCustomFieldValue(supplierInstanceID1, defectCodeFieldID, "DC-001");
        createCustomFieldValue(supplierInstanceID1, defectNameFieldID, "Bent");

        Long supplierInstanceID2 = createCustomObject(defectObjectTypeID);
        createCustomFieldValue(supplierInstanceID2, defectCodeFieldID,"DC-002");
        createCustomFieldValue(supplierInstanceID2, defectNameFieldID, "Cracked");

        Long supplierInstanceID3 = createCustomObject(defectObjectTypeID);
        createCustomFieldValue(supplierInstanceID3, defectCodeFieldID,"DC-003");
        createCustomFieldValue(supplierInstanceID3, defectNameFieldID, "Melted");

        Long supplierInstanceID4 = createCustomObject(defectObjectTypeID);
        createCustomFieldValue(supplierInstanceID4, defectCodeFieldID,"DC-004");
        createCustomFieldValue(supplierInstanceID4, defectNameFieldID, "Scratched");
    }

    public void populateContactObject(){
        System.out.println("Populating object=Contact");

        // creating custom object type 'contact'
        Long contactObjectTypeID = createCustomObjectType("ncm", "contact", "Contact", "This object is representing all contacts of the application");

        // creating fields for the customObjectType 'contact'
        CustomFieldType fieldType_text = CustomFieldTypeService.getInstance().selectByName(CFieldType.TEXT.name());
        Long contactCodeFieldID = createCustomField("contactCode","Contact Code", fieldType_text.getId(), contactObjectTypeID);
        Long contactNameFieldID = createCustomField("contactName", "Contact Name", fieldType_text.getId(), contactObjectTypeID);
        Long contactPhoneNumberFieldID = createCustomField("contactPhoneNumber", "Phone Number", fieldType_text.getId(), contactObjectTypeID);

        // creating custom objects (instances) of type contact
        Long contactInstanceID1 = createCustomObject(contactObjectTypeID);
        createCustomFieldValue(contactInstanceID1, contactCodeFieldID, "C000001");
        createCustomFieldValue(contactInstanceID1, contactNameFieldID, "Pablo Caballero");
        createCustomFieldValue(contactInstanceID1, contactPhoneNumberFieldID, "591-43546333");

        Long contactInstanceID2 = createCustomObject(contactObjectTypeID);
        createCustomFieldValue(contactInstanceID2, contactCodeFieldID,"C000002");
        createCustomFieldValue(contactInstanceID2, contactNameFieldID, "Alberto Saavedra");
        createCustomFieldValue(contactInstanceID2, contactPhoneNumberFieldID, "591-53562333");

        Long contactInstanceID3 = createCustomObject(contactObjectTypeID);
        createCustomFieldValue(contactInstanceID3, contactCodeFieldID,"C000003");
        createCustomFieldValue(contactInstanceID3, contactNameFieldID, "Elmer Zapata");
        createCustomFieldValue(contactInstanceID3, contactPhoneNumberFieldID, "591-22362333");
    }

    public void populatePartObject(){
        System.out.println("Populating object=Part");

        // creating object
        Long partObjectTypeID = createCustomObjectType("ncm", "part", "Part", "This object is representing all parts of the application");

        // creating fields
        CustomFieldType fieldType_text = CustomFieldTypeService.getInstance().selectByName(CFieldType.TEXT.name());
        CustomFieldType fieldType_lookup = CustomFieldTypeService.getInstance().selectByName(CFieldType.LOOKUP.name());

        Long partNumberFieldID = createCustomField("partNumber", "Part Number", fieldType_text.getId(), partObjectTypeID);
        Long partDescriptionFieldID = createCustomField("partDescription", "Part Description", fieldType_text.getId(), partObjectTypeID);

        // creating lookup Field to 'Supplier'
        CustomObjectType supplier = CustomObjectTypeService.getInstance().selectByCode("supplier");
        CustomField customField = CustomFieldService.getInstance().selectByCode("supplierName");
        Long displayFieldId = customField.getId();
        Long supplierFieldID = createCustomField("supplier", "Supplier", fieldType_lookup.getId(),partObjectTypeID, supplier.getId(), displayFieldId);

        List<CustomObject> suppliers = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(supplier.getId());
        int instanceNumber = suppliers.size()-1;

        // creating instances
        CustomObject supplierInstance = suppliers.get(instanceNumber);
        Long partInstanceID1 = createCustomObject(partObjectTypeID);
        createCustomFieldValue(partInstanceID1, partNumberFieldID, "3L1T-14A005-CJ");
        createCustomFieldValue(partInstanceID1, partDescriptionFieldID, "WIR ASY-BDY MN");
        createCustomFieldValue(partInstanceID1, supplierFieldID, supplierInstance.getId().toString());

        instanceNumber = instanceNumber-1;
        supplierInstance = suppliers.get(instanceNumber);
        Long partInstanceID2 = createCustomObject(partObjectTypeID);
        createCustomFieldValue(partInstanceID2, partNumberFieldID, "3L1T-14A005-FJ");
        createCustomFieldValue(partInstanceID2, partDescriptionFieldID, "WIR ASY-BDY MN");
        createCustomFieldValue(partInstanceID2, supplierFieldID, supplierInstance.getId().toString());

        instanceNumber = instanceNumber-1;
        supplierInstance = suppliers.get(instanceNumber);
        Long partInstanceID3 = createCustomObject(partObjectTypeID);
        createCustomFieldValue(partInstanceID3, partNumberFieldID, "3L1T-14A005-GJ");
        createCustomFieldValue(partInstanceID3, partDescriptionFieldID, "WIR ASY-BDY MN");
        createCustomFieldValue(partInstanceID3, supplierFieldID, supplierInstance.getId().toString());
    }

    public void populateTagObject(){
        System.out.println("Populating object=Tag");

        // creating custom object type 'tag'
        Long tagObjectTypeID = createCustomObjectType("ncm", "tag", "Tag", "This object is representing all tags of the application");

        // creating fields for the customObjectType 'tag'
        CustomFieldType fieldType_text = CustomFieldTypeService.getInstance().selectByName(CFieldType.TEXT.name());
        CustomFieldType fieldType_numeric = CustomFieldTypeService.getInstance().selectByName(CFieldType.NUMBER.name());
        CustomFieldType fieldType_autonumeric = CustomFieldTypeService.getInstance().selectByName(CFieldType.AUTONUMERIC.name());
        CustomFieldType fieldType_lookup = CustomFieldTypeService.getInstance().selectByName(CFieldType.LOOKUP.name());
        CustomFieldType fieldType_thing = CustomFieldTypeService.getInstance().selectByName(CFieldType.THING.name());

        Long tagNumberFieldID = createCustomField("tagNumber", "Tag Number", fieldType_autonumeric.getId(), tagObjectTypeID);

        CustomObjectType tagType = CustomObjectTypeService.getInstance().selectByCode("tagType");
        CustomField customFieldTagType = CustomFieldService.getInstance().selectByCode("tagTypeLabel");
        Long displayFieldTagType = customFieldTagType.getId();
        Long tagTypeFieldID = createCustomField("tagType", "Tag Type", fieldType_lookup.getId(), tagObjectTypeID, tagType.getId(), displayFieldTagType);

        Long defectQuantityFieldID = createCustomField("defectQuantity", "Defect Quantity", fieldType_numeric.getId(), tagObjectTypeID);

        // creating lookup Field to 'Part'
        CustomObjectType part = CustomObjectTypeService.getInstance().selectByCode("part");
        CustomField customField = CustomFieldService.getInstance().selectByCode("partNumber");
        Long displayFieldId = customField.getId();
        Long partFieldID = createCustomField("part", "Part", fieldType_lookup.getId(), tagObjectTypeID, part.getId(), displayFieldId);

        // creating lookup Field to 'Defect Category'
        CustomObjectType defect = CustomObjectTypeService.getInstance().selectByCode("defectCategory");
        CustomField defectField = CustomFieldService.getInstance().selectByCode("defectName");
        Long displayFieldId0 = defectField.getId();
        Long defectFieldID = createCustomField("defect", "Defect Category", fieldType_lookup.getId(), tagObjectTypeID, defect.getId(), displayFieldId0);

        // creating lookup field to 'shift'
        CustomObjectType shift = CustomObjectTypeService.getInstance().selectByCode("shift");
        CustomField shiftField = CustomFieldService.getInstance().selectByCode("shiftLabel");
        Long displayFieldId1 = shiftField.getId();
        Long shiftFieldID = createCustomField("shift", "Shift", fieldType_lookup.getId(), tagObjectTypeID, shift.getId(), displayFieldId1);

        //creating text with render (defect notes)
        Long defectNotesFieldID = createCustomField("defectNotes", "Defect Notes", fieldType_text.getId(), tagObjectTypeID, "textarea");

        // creating thing Field
        /*CustomObjectType defect = CustomObjectTypeService.getInstance().selectByCode("defectCategory");
        CustomField defectField = CustomFieldService.getInstance().selectByCode("defectName");
        Long displayFieldId0 = defectField.getId();
        Long defectFieldID = createCustomFieldThing("location", "Location", fieldType_thing.getId(), tagObjectTypeID, defect.getId(), displayFieldId0);*/

        List<CustomObject> parts = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(part.getId());
        List<CustomObject> defects = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(defect.getId());
        List<CustomObject> shifts = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(shift.getId());
        List<CustomObject> tagTypes = CustomObjectService.getInstance().getCustomObjectsByCustomObjectTypeId(tagType.getId());

        int partInstanceNumber = parts.size()-1;
        int defectInstanceNumber = defects.size()-1;

        // creating custom objects (instances) of type tag
        CustomObject partInstance = parts.get(partInstanceNumber);
        CustomObject defectInstance = defects.get(defectInstanceNumber);
        CustomObject shiftInstance = shifts.get(0);
        CustomObject tagTypeInstance = tagTypes.get(0);
        Long tagInstanceID1 = createCustomObject(tagObjectTypeID);
        createCustomFieldValue(tagInstanceID1, tagNumberFieldID, "1");
        createCustomFieldValue(tagInstanceID1, tagTypeFieldID, tagTypeInstance.getId().toString());
        createCustomFieldValue(tagInstanceID1, defectQuantityFieldID, "10");
        createCustomFieldValue(tagInstanceID1, partFieldID, partInstance.getId().toString());
        createCustomFieldValue(tagInstanceID1, defectFieldID, defectInstance.getId().toString());
        createCustomFieldValue(tagInstanceID1, shiftFieldID, shiftInstance.getId().toString());
        createCustomFieldValue(tagInstanceID1, defectNotesFieldID, "This part has been scratched");

        partInstanceNumber = partInstanceNumber-1;
        defectInstanceNumber = defectInstanceNumber-1;
        partInstance = parts.get(partInstanceNumber);
        defectInstance = defects.get(defectInstanceNumber);
        shiftInstance = shifts.get(1);
        tagTypeInstance = tagTypes.get(1);
        Long tagInstanceID2 = createCustomObject(tagObjectTypeID);
        createCustomFieldValue(tagInstanceID2, tagNumberFieldID, "2");
        createCustomFieldValue(tagInstanceID2, tagTypeFieldID, tagTypeInstance.getId().toString());
        createCustomFieldValue(tagInstanceID2, defectQuantityFieldID, "5");
        createCustomFieldValue(tagInstanceID2, partFieldID, partInstance.getId().toString());
        createCustomFieldValue(tagInstanceID2, defectFieldID, defectInstance.getId().toString());
        createCustomFieldValue(tagInstanceID2, shiftFieldID, shiftInstance.getId().toString());
        createCustomFieldValue(tagInstanceID2, defectNotesFieldID, "");

        partInstanceNumber = partInstanceNumber-1;
        defectInstanceNumber = defectInstanceNumber-1;
        partInstance = parts.get(partInstanceNumber);
        defectInstance = defects.get(defectInstanceNumber);
        shiftInstance = shifts.get(2);
        tagTypeInstance = tagTypes.get(2);
        Long tagInstanceID3 = createCustomObject(tagObjectTypeID);
        createCustomFieldValue(tagInstanceID3, tagNumberFieldID, "3");
        createCustomFieldValue(tagInstanceID3, tagTypeFieldID, tagTypeInstance.getId().toString());
        createCustomFieldValue(tagInstanceID3, defectQuantityFieldID, "2");
        createCustomFieldValue(tagInstanceID3, partFieldID, partInstance.getId().toString());
        createCustomFieldValue(tagInstanceID3, defectFieldID, defectInstance.getId().toString());
        createCustomFieldValue(tagInstanceID3, shiftFieldID, shiftInstance.getId().toString());
        createCustomFieldValue(tagInstanceID3, defectNotesFieldID, "");

    }

    public void populateCustomFiledTypes(){
        createCustomFieldType(CFieldType.TEXT.name(), "TEXT", "This type is for text values");
        createCustomFieldType(CFieldType.NUMBER.name(), "NUMBER", "This type is for numeric values");
        createCustomFieldType(CFieldType.AUTONUMERIC.name(), "AUTONUMERIC", "This type is for autonumeric values");
        createCustomFieldType(CFieldType.DATE.name(), "DATE", "This type is for date values");
        createCustomFieldType(CFieldType.BOOLEAN.name(), "BOOLEAN", "This type is for boolean values");
        createCustomFieldType(CFieldType.LOOKUP.name(), "LOOKUP (Business Object)", "This type creates a relationship that links this object to another object");
        createCustomFieldType(CFieldType.THING.name(), "LOOKUP (Thing Type)", "This type Create a relationship that links this object to a Thing object");
    }

    public void createCustomFieldType(String customFieldTypeName, String customFieldTypeDisplayName, String description){
        CustomFieldType customFieldType0 = new CustomFieldType();
        customFieldType0.setName(customFieldTypeName);
        customFieldType0.setDisplayName(customFieldTypeDisplayName);
        customFieldType0.setDescription(description);
        CustomFieldTypeService.getInstance().insert(customFieldType0);
    }

    public Long createCustomObject(Long customObjectTypeID){
        CustomObjectType customObjectType = CustomObjectTypeService.getInstance().get(customObjectTypeID);

        CustomObject supplier0 = new CustomObject();
        supplier0.setCustomObjectType(customObjectType);
        supplier0.setArchived(false);
        CustomObjectService.getInstance().insert(supplier0);

        return supplier0.getId();
    }

    public void createCustomFieldValue(Long customObjectID, Long customFieldID, String value){
        CustomObject customObject = CustomObjectService.getInstance().get(customObjectID);
        CustomField customField = CustomFieldService.getInstance().get(customFieldID);

        CustomFieldValue customFieldValue1 = new CustomFieldValue();
        customFieldValue1.setCustomObject(customObject);
        customFieldValue1.setCustomField(customField);
        customFieldValue1.setValue(value);
        CustomFieldValueService.getInstance().insert(customFieldValue1);
    }

    public Long createCustomField(String customFieldCode, String customFieldName, Long customFieldTypeID, Long customObjectTypeID){
        CustomFieldType customFieldType = CustomFieldTypeService.getInstance().get(customFieldTypeID);
        CustomObjectType customObjectType = CustomObjectTypeService.getInstance().get(customObjectTypeID);

        CustomField customField = new CustomField();
        customField.setCode(customFieldCode);
        customField.setName(customFieldName);
        customField.setRequired(true);
        customField.setCustomFieldType(customFieldType);
        customField.setCustomObjectType(customObjectType);
        CustomFieldService.getInstance().insert(customField);

        return customField.getId();
    }

    //render
    public Long createCustomField(String customFieldCode, String customFieldName, Long customFieldTypeID, Long customObjectTypeID, String render){
        CustomFieldType customFieldType = CustomFieldTypeService.getInstance().get(customFieldTypeID);
        CustomObjectType customObjectType = CustomObjectTypeService.getInstance().get(customObjectTypeID);

        CustomField customField = new CustomField();
        customField.setCode(customFieldCode);
        customField.setName(customFieldName);
        customField.setRequired(true);
        customField.setRender(render);
        customField.setCustomFieldType(customFieldType);
        customField.setCustomObjectType(customObjectType);
        CustomFieldService.getInstance().insert(customField);

        return customField.getId();
    }

    public Long createCustomField(String customFieldCode, String customFieldName, Long customFieldTypeID, Long customObjectTypeID, Long lookupObjectID, Long llokupObjectFieldId){
        CustomFieldType customFieldType = CustomFieldTypeService.getInstance().get(customFieldTypeID);
        CustomObjectType customObjectType = CustomObjectTypeService.getInstance().get(customObjectTypeID);
        CustomObjectType lookupObject = CustomObjectTypeService.getInstance().get(lookupObjectID);
        CustomField lokupObjectDisplayField = CustomFieldService.getInstance().get(llokupObjectFieldId);

        CustomField customField = new CustomField();
        customField.setCode(customFieldCode);
        customField.setName(customFieldName);
        customField.setRequired(true);
        customField.setCustomFieldType(customFieldType);
        customField.setCustomObjectType(customObjectType);
        customField.setLookupObject(lookupObject);
        customField.setLookupObjectField(lokupObjectDisplayField);
        CustomFieldService.getInstance().insert(customField);

        return customField.getId();
    }

    public Long createCustomFieldThing(String customFieldCode, String customFieldName, Long customFieldTypeID, Long customObjectTypeID, Long thingTypeId, Long thingTypeFieldId){
        CustomFieldType customFieldType = CustomFieldTypeService.getInstance().get(customFieldTypeID);
        CustomObjectType customObjectType = CustomObjectTypeService.getInstance().get(customObjectTypeID);
        ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
        ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get(thingTypeFieldId);

        CustomField customField = new CustomField();
        customField.setCode(customFieldCode);
        customField.setName(customFieldName);
        customField.setRequired(true);
        customField.setCustomFieldType(customFieldType);
        customField.setCustomObjectType(customObjectType);
        customField.setThingType(thingType);
        customField.setThingTypeField(thingTypeField);
        CustomFieldService.getInstance().insert(customField);

        return customField.getId();
    }

    public Long createCustomObjectType(String customObjectTypeCode, String customObjectTypeName,
                                       String customObjectTypeDesc){
        CustomObjectType customObject = new CustomObjectType();
        customObject.setCode(customObjectTypeCode);
        customObject.setName(customObjectTypeName);
        customObject.setDescription(customObjectTypeDesc);
        CustomObjectTypeService.getInstance().insert(customObject);
        return customObject.getId();
    }

    public Long createCustomObjectType(String application_code, String customObjectTypeCode, String customObjectTypeName,
                                       String customObjectTypeDesc){

        CustomApplication customApplication = CustomApplicationService.getInstance().selectByCode(application_code);

        CustomObjectType customObject = new CustomObjectType();
        customObject.setCode(customObjectTypeCode);
        customObject.setName(customObjectTypeName);
        customObject.setDescription(customObjectTypeDesc);
        customObject.setCustomApplication(customApplication);
        CustomObjectTypeService.getInstance().insert(customObject);
        return customObject.getId();
    }

    public void createCustomApplication(String code, String name, boolean shotTab){
        CustomApplication customApplication = new CustomApplication();
        customApplication.setCode(code);
        customApplication.setName(name);
        customApplication.setShotTab(shotTab);
        CustomApplicationService.getInstance().insert(customApplication);
    }

    public void populateGroupsNCMApplication(){
        Group rootGroup = GroupService.getInstance().getRootGroup();
        GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();
        Group acme = PopDBUtils.popGroup("ACME Inc", "A", rootGroup, tenantGroupType, "");
        ThingType part = PopDBIOTUtils.popThingType(acme, null, "Parts" );
    }


}
