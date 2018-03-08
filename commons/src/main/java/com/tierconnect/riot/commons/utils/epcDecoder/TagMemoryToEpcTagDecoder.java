package com.tierconnect.riot.commons.utils.epcDecoder;

import org.apache.log4j.Logger;
import org.omg.CORBA.UserException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.EnumMap;
import java.util.Map;

/*************************************************************************
 * Copyright  [2006] - [2013]  Mojix Incorporated  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Mojix Incorporated .  The intellectual and
 * technical concepts contained herein are proprietary to Mojix
 * Incorporated and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Mojix Incorporated.
 ***************************************************************************/

/**
 * @author GilYarry
 * @RSE GilYarry
 *
 */
public class TagMemoryToEpcTagDecoder {

    public enum TagFormat{TAG_URI, PURE_IDENTITY,RAW_HEX,RAW_DECIMAL}
    private final String COMPANY_PREFIX = "COMPANY_PREFIX";
    private final String SERIAL = "SERIAL";
    private final String GS1_KEY = "GS1KEY";
    private final String GS1_KEY_NC = "GS1KEY_NOCOMPANY_PREFIX";
    private final String GS1_KEY_WS = "GS1KEYSERIAL";
    private final String CHECK_DIGIT = "UPC_CHECKDIGIT";
    private final String SCHEMA = "SCHEMA";
    private final String EPC_TAG_URI = "EPC_TAG_URI";
    private final String EPC_PURE_IDENTITY_URI = "EPC_PURE_IDENTITY_URI";

    private static final int HEX = 16;
    static Logger logger = Logger.getLogger( TagMemoryToEpcTagDecoder.class );


    public Map<TagFormat,String> hexToMap(String hex){
        return hexToMap(hex, true, true, true, true, null);
    }

    public Map<TagFormat,String> hexToMap(String hex, Map<TagFormat,String>map){
        return hexToMap(hex, true, true, true, true,map);
    }

    public Map<TagFormat,String> hexToMap(String hex, boolean includeTagUri, boolean includePureId, boolean includeRawDecimal, boolean includeRawHex){
        return hexToMap(hex, includeTagUri, includePureId, includeRawDecimal, includeRawHex, null);
    }

    //TODO: allow passing in required formats and returning only those
    public Map<TagFormat,String> hexToMap(String hex, boolean includeTagUri, boolean includePureId, boolean includeRawDecimal, boolean includeRawHex,Map<TagFormat,String> map){

        if(map == null){
            map = new EnumMap<TagFormat,String>(TagFormat.class);
        }

        BigIntegerWrapper bi = new BigIntegerWrapper(hex);
        BinaryEncodingScheme scheme = getScheme(bi);

        if(includeTagUri){
            String tagUri =  decodeHex(hex, TagFormat.TAG_URI, scheme, bi);
            map.put(TagFormat.TAG_URI, tagUri);
        }

        if(includePureId){
            String pureIdentity =  decodeHex(hex, TagFormat.PURE_IDENTITY, scheme, bi);
            map.put(TagFormat.PURE_IDENTITY, pureIdentity);
        }

        if(includeRawHex){
			/*
			if(scheme != null){
				String rawHex = "urn:epc:raw:" + scheme.getBitCount() + ".x" + hex.toUpperCase(); //String.format("urn:epc:raw:%s.x%s", scheme.getBitCount(), hex.toUpperCase());
				map.put(TagFormat.RAW_HEX, rawHex);
			}
			else{
				int bitCount = bi.getValue().bitLength();
				bitCount = (int)Math.ceil(((double)bitCount)/4D) * 4;
				String rawHex = "urn:epc:raw:" + bitCount + ".x" + hex.toUpperCase(); //String.format("urn:epc:raw:%s.x%s", scheme.getBitCount(), hex.toUpperCase());
				map.put(TagFormat.RAW_HEX, rawHex);
			}
			*/


            //int bitCount = bi.getValue().bitLength();
            //bitCount = (int)Math.ceil(((double)bitCount)/4D) * 4;
            int bitCount = hex.length() * 4;

            String rawHex = "urn:epc:raw:" + bitCount + ".x" + hex.toUpperCase(); //String.format("urn:epc:raw:%s.x%s", scheme.getBitCount(), hex.toUpperCase());
            map.put(TagFormat.RAW_HEX, rawHex);
        }

        if(includeRawDecimal){
			/*
			if(scheme != null){
				String rawDecimal =  "urn:epc:raw:" + scheme.getBitCount() + "." + bi.getValue().toString(10); //String.format("urn:epc:raw:%s.%s", scheme.getBitCount(), bi.getValue().toString(10));
				map.put(TagFormat.RAW_DECIMAL, rawDecimal);
			}
			else{
				int bitCount = bi.getValue().bitLength();
				bitCount = (int)Math.ceil(((double)bitCount)/4D) * 4;
				String rawDecimal =  "urn:epc:raw:" + bitCount + "." + bi.getValue().toString(10); //String.format("urn:epc:raw:%s.%s", scheme.getBitCount(), bi.getValue().toString(10));
				map.put(TagFormat.RAW_DECIMAL, rawDecimal);
			}
			*/


            //int bitCount = bi.getValue().bitLength();
            //bitCount = (int)Math.ceil(((double)bitCount)/4D) * 4;
            int bitCount = hex.length() * 4;
            String rawDecimal =  "urn:epc:raw:" + bitCount + "." + bi.getValue().toString(10); //String.format("urn:epc:raw:%s.%s", scheme.getBitCount(), bi.getValue().toString(10));
            map.put(TagFormat.RAW_DECIMAL, rawDecimal);
        }

        //com.mojix.ale.core.util.epc.BigIntegerWrapper bi = new com.mojix.ale.core.util.epc.BigIntegerWrapper(hex);
        //BigInteger schemeCode = bi.getBitsRev(0, 7);
        //long l = schemeCode.longValue();
        //com.mojix.ale.core.util.epc.BinaryEncodingScheme scheme = com.mojix.ale.core.util.epc.BinaryEncodingScheme.getLongToSchemeMap().get(l);
        //int bitCount = scheme.getBitCount();

        return map;
    }

    public String hexToTagUri(String hex){
        return decodeHex(hex, TagFormat.TAG_URI);
    }

    public String hexToPureIdentity(String hex){
        return decodeHex(hex, TagFormat.PURE_IDENTITY);
    }

    protected String decodeHex(String hex, TagFormat format){
        BigIntegerWrapper bi = new BigIntegerWrapper(hex);
        BinaryEncodingScheme scheme = getScheme(bi);
        return decodeHex(hex,format,scheme,bi);
    }

    private BinaryEncodingScheme getScheme(BigIntegerWrapper bi) {
        BigInteger schemeCode = bi.getBitsRev(0, 7);
        long l = schemeCode.longValue();
        BinaryEncodingScheme scheme = BinaryEncodingScheme.getLongToSchemeMap().get(l);
        return scheme;
    }

    protected String decodeHex(String hex, TagFormat format, BinaryEncodingScheme scheme, BigIntegerWrapper bi){
        String decoded = null;
        try{
            decoded = _decodeHex(hex, format, scheme, bi);
        }catch(Exception ex){
            //logger.warn("Could not decode Tag: " + hex, ex);
        }
        return decoded;
    }

    protected String _decodeHex(String hex, TagFormat format, BinaryEncodingScheme scheme, BigIntegerWrapper bi){

        //System.out.println(schemeCode.toString(2));
        StringBuilder sb = new StringBuilder();
        if(TagFormat.TAG_URI == format){
            sb.append("urn:epc:tag:");
        }else if( TagFormat.PURE_IDENTITY == format ){
            sb.append("urn:epc:id:");
        }

        if(scheme == null){
            return null;
        }
        if(scheme != null){

            int bitsCount = bi.bitLength() + bi.getMissingMsbZeros(); //add 2 bits to account for missing zeros on the right
            if(bitsCount > scheme.getBitCount()){
                bi.changeValue( bi.getValue().shiftRight(bitsCount - scheme.getBitCount())   );
                //System.out.println(bi.getValue().toString(2));
            }


            if(TagFormat.PURE_IDENTITY == format){
                sb.append(scheme.getPureIdentitySchemeId());
                sb.append(':');
            }
            else if(TagFormat.TAG_URI == format){
                sb.append(scheme.getUriTagSchemeId());
                sb.append(':');
            }



            if(scheme.getFilterBitsCount() > 0 && TagFormat.TAG_URI == format){
                BigInteger filter = bi.getBitsRev(8, 8 + scheme.getFilterBitsCount() - 1);
                sb.append(filter.intValue());
            }
            //System.out.println(sb.toString());
            int[][] partitionTable = scheme.getPartitionTable();

            if(partitionTable != null){
                if(TagFormat.TAG_URI == format){
                    sb.append('.');
                }
                BigInteger fieldsIndex = bi.getBitsRev(11, 13);


                //Bug 2146
                if(partitionTable.length <= fieldsIndex.intValue()){
                    //in this case the tag is invalid
                    return null;
                }
                int [] partitionFields = partitionTable[fieldsIndex.intValue()];



                int field1BitsLength = partitionFields[0];
                int field1DigitsLength = partitionFields[1];
                int field2BitsLength = partitionFields[2];
                int field2DigitsLength = partitionFields[3];

                int field1FromBitIndex = 14;
                int field1ToBitIndex = field1FromBitIndex + field1BitsLength -1;
                int field2FromBitIndex = field1ToBitIndex + 1;
                int field2ToBitIndex = field2FromBitIndex + field2BitsLength - 1;

                BigInteger field1Value = bi.getBitsRev(field1FromBitIndex, field1ToBitIndex);
                BigInteger field2Value = bi.getBitsRev(field2FromBitIndex, field2ToBitIndex);

                //////////////////// TODO: Find the partition table type and format field as needed. padded, unpadded, String
                formatFieldAndAppend(sb, field1Value,field1DigitsLength,field1BitsLength,scheme.getPartitionTableType(),0);
                sb.append('.');
                formatFieldAndAppend(sb, field2Value,field2DigitsLength,field2BitsLength,scheme.getPartitionTableType(),1);
            }


            AdditionalField[] additionalFields = scheme.getAdditionalFields();
            //System.out.println(Arrays.toString(additionalFields));
            if(additionalFields != null && additionalFields.length > 0){
                if(scheme.getFilterBitsCount() > 0){
                    if(sb.charAt(sb.length()-1) != ':'){
                        sb.append('.');
                    }
                }
                for(int i=0; i<additionalFields.length; i++){
                    AdditionalField field = additionalFields[i];
                    AdditionalField.AdditionalFieldType type = field.getType();


                    int fromBitIndex = field.getFromBitIndex();
                    int toBitIndex = field.getToBitIndex();
                    int size = field.getSize();
                    if(fromBitIndex >=0 && toBitIndex >=0 ){
                        //this is a fixed length field
                        formatAdditionalField(bi, sb, type, fromBitIndex, toBitIndex, size);
                        if(i < additionalFields.length-1){
                            sb.append(".");
                        }
                        //System.out.println(sb.toString());
                    }
                    else{
                        formatAdditionalField(bi, sb, type, fromBitIndex, toBitIndex, size);
                    }
                }
            }
        }

        //BigInteger test2 = bi.getBits(0, 39);
        //System.out.println(test2.toString(2));

        return sb.toString();
    }

    public String tagToEpc(String tag) {
        return null;
    }


    private void formatFieldAndAppend(StringBuilder sb, BigInteger fieldValue, int fieldDigitsLength, int fieldBitsLength, PartitionTableType partitionTableType, int partitionFieldIndex) {

        if(PartitionTableType.PartitionTable.equals(partitionTableType)){
            if(partitionFieldIndex == 0){
                handlePaddedIntegerValue(sb, fieldValue, fieldDigitsLength);
            }
            if(partitionFieldIndex == 1){
                if(fieldDigitsLength != 0){
                    handlePaddedIntegerValue(sb, fieldValue, fieldDigitsLength);
                }
            }
        }
        if(PartitionTableType.UnpaddedPartitionTable.equals(partitionTableType)){
            long fieldIntValue = fieldValue.longValue();
            sb.append(fieldIntValue);
        }
        if(PartitionTableType.StringPartitionTable.equals(partitionTableType)){
            BigIntegerWrapper biw = new BigIntegerWrapper(fieldValue);
            if(partitionFieldIndex == 0){
                handlePaddedIntegerValue(sb, fieldValue, fieldDigitsLength);
            }
            else{
                handleSevenBitString(biw, sb, fieldBitsLength-1);
            }
        }
    }

    private void handlePaddedIntegerValue(StringBuilder sb,
                                          BigInteger fieldValue, int fieldDigitsLength) {
        long fieldIntValue = fieldValue.longValue();
        String strDecimal = Long.toString(fieldIntValue);
        int paddingSize = fieldDigitsLength - strDecimal.length();
        if(paddingSize  > 0  ){
            for(int i=0; i<paddingSize; i++){
                sb.append('0');
            }
        }
        sb.append(strDecimal);
    }

    private void formatAdditionalField(BigIntegerWrapper bi, StringBuilder sb, AdditionalField.AdditionalFieldType type, int fromBitIndex, int toBitIndex, int size) {

        if(fromBitIndex >= 0 && toBitIndex >= 0){
            if(AdditionalField.AdditionalFieldType.NumericString.equals(type)){
                BigInteger fieldValue = bi.getBits(fromBitIndex, toBitIndex);
                long fieldIntValue = fieldValue.longValue();
                String numericString = Long.toString(fieldIntValue);
                sb.append(numericString.substring(1));
            }
            else if(AdditionalField.AdditionalFieldType.Integer.equals(type)){
                BigInteger fieldValue = bi.getBits(fromBitIndex, toBitIndex);
                long fieldIntValue = fieldValue.longValue();
                sb.append(fieldIntValue);
            }
            else if(AdditionalField.AdditionalFieldType.String.equals(type)){
                handleSevenBitString(bi, sb, toBitIndex);
            }
            else if( AdditionalField.AdditionalFieldType.EightBitCageDodAac.equals(type) ){
                BigInteger fieldValue = bi.getBits(fromBitIndex, toBitIndex);
                byte[]bytes = fieldValue.toByteArray(); //these are 8 bit encoded so the byte representation is OK
                String str = null;
                try {
                    str = new String(bytes,"UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Could not format additional field "+e);
                }
                sb.append(str);
                //System.out.println(str);
            }

        }
        else{
            //handle a variable length field
            if( AdditionalField.AdditionalFieldType.SixBitCageDodAac.equals(type) ){
                handleSixBitString(bi, sb, 36);
            }
            else if( AdditionalField.AdditionalFieldType.SixBitVariableString.equals(type) ){
                handleSixBitVarLengthString(bi, sb);
            }
        }
    }


    //this solution performs much better. We are slicing the BigInteger into longs of 63 bits (7 * 9)
    //and we then shift and mask them to get each character. This is faster than converting every 7 bits into a BigInteger
    private void handleStringForPartitionField(BigIntegerWrapper bi, StringBuilder sb, int toBitIndex) {

        for(int toIndex = toBitIndex; toIndex >= 0; toIndex -= 63){

            int fromIndex = toIndex - 62; //we need 63 bits which are 7 * 9
            if(fromIndex < 0){
                fromIndex = 0;
            }

            int bitLength = toIndex - fromIndex + 1;
            long value = bi.getBits(fromIndex, toIndex).longValue();
            //System.out.println(BigInteger.valueOf(value).toString(2));
            long mask = 127 ; //000000000001111111

            for(int i=bitLength; i>0; i-=7){
                long currentValue = value >> (i-7);
                int _7_lsb = (int)(currentValue & mask);

                char c = (char)_7_lsb; //+2
                int n = c;
                if(c != 0){
                    switch(c){
                        case 34:
                            sb.append("%22");
                            break;
                        case 37:
                            sb.append("%25");
                            break;
                        case 38:
                            sb.append("%26");
                            break;
                        case 47:
                            sb.append("%2F");
                            break;
                        case 60:
                            sb.append("%3C");
                            break;
                        case 62:
                            sb.append("%3E");
                            break;
                        case 63:
                            sb.append("%3F");
                            break;
                        default:
                            sb.append(c);
                    }

                }else{
                    break;
                }
            }
        }
    }


    //this solution performs much better. We are slicing the BigInteger into longs of 36 bits (6 * 6)
    //and we then shift and mask them to get each character. This is faster than converting every 6 bits into a BigInteger
    private void handleSixBitString(BigIntegerWrapper bi, StringBuilder sb, int bitsCount) {

        int fromMsbBit = 50;

        int missingMsb = bi.getMissingMsbZeros();
        int len = bi.getValue().bitLength();
        //System.out.println(bi.toString(2));

        long value = bi.getValue().shiftRight(len - fromMsbBit  + missingMsb).longValue();
        long mask1 = (1L << 36) -1;
        value = value & mask1;
        //System.out.println(BigInteger.valueOf(value).toString(2));

        long mask2 = (1 << 6) -1;// 000000111111
        for(int fromIndex = bitsCount-6; fromIndex >= 0 ; fromIndex -= 6){
            long shifted = value >> fromIndex;
            int masked = (int)(shifted & mask2);
            switch(masked){
                case 35:
                    sb.append("%23");
                    break;
                case 47:
                    sb.append("%2F");
                    break;
                default:
                    char c = masked > 30 ? (char) (masked) : (char) (masked + 64);
                    sb.append(c);
            }
        }
    }


    private void handleSixBitVarLengthString(BigIntegerWrapper bi, StringBuilder sb){

        int l1 = (bi.getValue().toString(2)).length();
        int len = bi.getValue().bitLength();
        int missingMsb = bi.getMissingMsbZeros();
        int remainingDataBitLength = len - 48;
        sb.append('.');
        int fieldIndex = 0;

        for( ; remainingDataBitLength > 0; ){

            int shiftBy = remainingDataBitLength - 60;
            int currentChunkBitsCount = 60;
            long mask = 0L;

            if(shiftBy < 0){
                shiftBy = 0;
                mask = (1L << remainingDataBitLength) - 1;
                currentChunkBitsCount = remainingDataBitLength;
            }
            else{
                mask = (1L << 60) - 1;
                currentChunkBitsCount = 60;
            }

            long value = bi.getValue().shiftRight(shiftBy).longValue();
            long maskedValue = value & mask;
            //System.out.println(BigInteger.valueOf(maskedValue).toString(2));

            long mask2 = (1L << 6) -1;// 000000111111
            for(int fromIndex = currentChunkBitsCount-6; fromIndex >= 0 ; fromIndex -= 6){
                long shifted = maskedValue >> fromIndex;
                int masked = (int)(shifted & mask2);
                if(masked == 0){
                    if(fieldIndex == 0){
                        sb.append('.');
                        fieldIndex++;
                    }
                    else{
                        break;
                    }
                }
                else{
                    switch(masked){
                        case 35:
                            sb.append("%23");
                            break;
                        case 47:
                            sb.append("%2F");
                            break;
                        default:
                            char c = masked > 30 ? (char) (masked) : (char) (masked + 64);
                            sb.append(c);
                    }
                }
            }

            remainingDataBitLength -= 60;
        }

    }


    //this solution performs much better. We are slicing the BigInteger into longs of 63 bits (7 * 9)
    //and we then shift and mask them to get each character. This is faster than converting every 7 bits into a BigInteger
    private void handleSevenBitString(BigIntegerWrapper bi, StringBuilder sb, int toBitIndex) {

        for(int toIndex = toBitIndex; toIndex >= 0; toIndex -= 63){

            int fromIndex = toIndex - 62; //we need 63 bits which are 7 * 9
            if(fromIndex < 0){
                fromIndex = 0;
            }
            int bitLength = toIndex - fromIndex + 1;
            long value = bi.getBits(fromIndex, toIndex).longValue();
            //System.out.println(BigInteger.valueOf(value).toString(2));
            long mask = 127 ; //000000000001111111

            for(int i=bitLength; i>0; i-=7){
                long currentValue = value >> (i-7);
                int _7_lsb = (int)(currentValue & mask);

                char c = (char)_7_lsb; //+2
                int n = c;
                if(c != 0){
                    switch(c){
                        case 34:
                            sb.append("%22");
                            break;
                        case 37:
                            sb.append("%25");
                            break;
                        case 38:
                            sb.append("%26");
                            break;
                        case 47:
                            sb.append("%2F");
                            break;
                        case 60:
                            sb.append("%3C");
                            break;
                        case 62:
                            sb.append("%3E");
                            break;
                        case 63:
                            sb.append("%3F");
                            break;
                        default:
                            sb.append(c);
                    }
                }else{
                    break;
                }
            }
        }
    }


    /*
    private void handleStringOld(com.mojix.ale.core.util.epc.BigIntegerWrapper bi, StringBuilder sb, int toBitIndex) {
        for(int lastBitIndex = toBitIndex; lastBitIndex>=6; lastBitIndex -= 7){
            int firstBitIndex = lastBitIndex - 6;
            char c = (char)bi.getBits(firstBitIndex, lastBitIndex).intValue(); //+2
            int n = c;
            if(c != 0){
                sb.append(c);
            }else{
                break;
            }
        }
    }
    */
    public String toRawHex(String hexString) {
        //TODO: validate this is really 96 bit
        int lengthInBytes = hexString.length() * 4;
        return String.format("urn:epc:raw:%s.x%s", lengthInBytes, hexString.toUpperCase());
    }

    public String toRawDecimal(String hexString) {
        //TODO: validate this is really 96 bit
        int lengthInBytes = hexString.length() * 4;
        BigInteger bi = new BigInteger(hexString, HEX);
        return String.format("urn:epc:raw:%s.%s", lengthInBytes, bi.toString());
    }

    public String tagFieldsDecoder(String serialNumber, String field){
        String fieldValue = null;
        try{
        String identity = decodeHex(serialNumber, TagFormat.PURE_IDENTITY);
        String pureIdentity[] = identity.split(":");
        String digits[] = pureIdentity[4].split("\\.");

        if (field == null){
            fieldValue = generateGS1(serialNumber, pureIdentity, digits, false);
        }else{
            field = field.toUpperCase();
            switch (field){
                case COMPANY_PREFIX:
                    fieldValue = digits[0];
                    break;
                case GS1_KEY_WS:
                    if (digits.length ==3 ) {
                        fieldValue = generateGS1(serialNumber, pureIdentity, digits, false) + digits[2];
                    }else{
                        fieldValue = generateGS1(serialNumber, pureIdentity, digits, false);
                    }
                    break;
                case SERIAL:
                    if (digits.length ==3 ) {
                        fieldValue = digits[2];
                    }else{
                        fieldValue="0";
                    }
                    break;
                case GS1_KEY_NC:
                    fieldValue = generateGS1NoCompany(serialNumber, pureIdentity, digits);
                    break;
                case CHECK_DIGIT:
                    fieldValue = generateGS1(serialNumber, pureIdentity, digits, true);
                    break;
                case SCHEMA:
                    fieldValue = pureIdentity[3].toUpperCase();
                    break;
                case EPC_PURE_IDENTITY_URI:
                    fieldValue = identity;
                    break;
                case EPC_TAG_URI:
                    fieldValue = decodeHex(serialNumber, TagFormat.TAG_URI);
                    break;
                case GS1_KEY:
                default:
                    fieldValue = generateGS1(serialNumber, pureIdentity, digits, false);
                    break;

            }
        }

        }catch (Exception e){
            logger.info("Not possible to generate a valid UPC from: " + serialNumber + ", setting default value (00000000)");
            fieldValue ="00000000";
        }
    return fieldValue;
    }

    public String generateGS1(String serialNumber, String pureIdentity[], String digits[], boolean checkDigit){
        String upc = null;
        try {
            String temporalUpc = null;
            String checkDigitValue = null;
            switch (pureIdentity[3]) {
                case "sgtin":
                    temporalUpc = digits[1].substring(0, 1) + digits[0] + digits[1].substring(1);
                    checkDigitValue = getCheckDigit(temporalUpc);
                    break;
                case "grai":
                    temporalUpc = digits[0] + digits[1];
                    checkDigitValue = getCheckDigit(temporalUpc);
                    break;
                case "giai":
                    temporalUpc = digits[0] + digits[1];
                    checkDigitValue="";
                    break;
            }
            if (checkDigit){
                if (checkDigitValue.equals("")){
                    upc = "0";
                }else {
                    upc = checkDigitValue;
                }
            }else {
                upc = temporalUpc + checkDigitValue;
            }
        }catch (Exception e){
            logger.info("Not possible to generate a valid UPC from: " + serialNumber + ", setting default value (00000000)");
            upc = "00000000";
        }

        return upc;
    }

    public String generateGS1NoCompany(String serialNumber, String pureIdentity[], String digits[]){
        String upc = null;
        try {
            String temporalUpc;
            String checkDigit;
            switch (pureIdentity[3]) {
                case "sgtin":
                    temporalUpc = digits[1].substring(0, 1) + digits[0] + digits[1].substring(1);
                    checkDigit = getCheckDigit(temporalUpc);
                    upc = digits[1].substring(1) + checkDigit;
                    break;
                case "grai":
                    temporalUpc = digits[0] + digits[1];
                    checkDigit = getCheckDigit(temporalUpc);
                    upc = digits[1] + checkDigit;
                    break;
                case "giai":
                    upc= digits[1];
            }
        }catch (Exception e){
            logger.info("Not possible to generate a valid UPC from: " + serialNumber + ", setting default value (00000000)");
            upc = "00000000";
        }

        return upc;
    }

    public String getCheckDigit(String gs1Temporal){
        int total = gs1Temporal.length()-1;
        int totalSum = 0;
        for (int i = 0 ; i <= total; i++){
            if (i%2 == 0){
                totalSum = totalSum + ((gs1Temporal.charAt(total-i)-48)*3);
            }else{
                totalSum = totalSum + (gs1Temporal.charAt(total-i)-48);
            }
        }
        int checkDigit = totalSum %10 == 0 ? 0 : 10  - (totalSum % 10);
        return String.valueOf(checkDigit);
    }

}
