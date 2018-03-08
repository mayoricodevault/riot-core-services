package com.tierconnect.riot.iot.reports.autoindex.entities;


/**
 * Created by julio.rocha on 26-04-17.
 */
public class QueryField implements Comparable<QueryField> {
    private String fieldName;
    private FieldType fieldType;
    private String operator;
    private Long cardinality;
    private String dataType;
    private Integer nestedLevel;

    public enum FieldType {
        EQUALITY(1),
        SORT(2),
        RANGE(3);
        public final Integer value;

        FieldType(Integer value) {
            this.value = value;
        }

        public static boolean isEquality(FieldType fieldType) {
            return EQUALITY.value.equals(fieldType.value);
        }

        public static boolean isSORT(FieldType fieldType) {
            return SORT.value.equals(fieldType.value);
        }

        public static boolean isRANGE(FieldType fieldType) {
            return RANGE.value.equals(fieldType.value);
        }
    }

    public QueryField(String fieldName, FieldType fieldType, String operator, Integer nestedLevel) {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.operator = operator;
        this.nestedLevel = nestedLevel;
        this.cardinality = 0L;
    }

    public String getFieldName() {
        return fieldName;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public String getOperator() {
        return operator;
    }

    public synchronized Long getCardinality() {
        return cardinality;
    }

    public synchronized void setCardinality(Long cardinality) {
        this.cardinality = cardinality;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        if (dataType != null) {
            this.dataType = dataType;
        }
    }

    public Integer getNestedLevel() {
        return nestedLevel;
    }

    @Override
    public int hashCode() {
        return 31 * fieldName.hashCode() + fieldType.value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        QueryField qf = (QueryField) obj;
        return this.fieldName.equals(qf.fieldName) && this.fieldType.equals(qf.fieldType);
    }

    @Override
    public int compareTo(QueryField qf) {
        if (FieldType.isEquality(this.fieldType) && FieldType.isEquality(qf.fieldType)) {
            return this.getCardinality().compareTo(qf.getCardinality()); //ASC order
        }

        if (FieldType.isSORT(this.fieldType) && FieldType.isSORT(qf.fieldType)) {
            return 0; //order no according apparition
        }

        if (FieldType.isRANGE(this.fieldType) && FieldType.isRANGE(qf.fieldType)) {
            return this.getCardinality().compareTo(qf.getCardinality()) * (-1); //DESC order
        }

        return this.fieldType.value.compareTo(qf.fieldType.value) * (-1); //DESC by type
    }

    public static boolean isArray(String f) {
        return isArrayOfObjects(f) || isNativeArray(f);
    }

    public static boolean isNativeArray(String f) {
        return "native_array".equals(f);
    }

    public static boolean isArrayOfObjects(String f) {
        return "object_array".equals(f);
    }

    public static boolean isSortOrEquals(QueryField qf){
        return FieldType.isSORT(qf.fieldType) || FieldType.isEquality(qf.fieldType);
    }

    public static boolean isObject(String f) {
        return "object".equals(f);
    }

    @Override
    public String toString() {
        return "QueryField{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldType=" + fieldType +
                ", operator='" + operator + '\'' +
                ", cardinality=" + cardinality +
                ", dataType='" + dataType + '\'' +
                ", nestedLevel=" + nestedLevel +
                '}';
    }
}
