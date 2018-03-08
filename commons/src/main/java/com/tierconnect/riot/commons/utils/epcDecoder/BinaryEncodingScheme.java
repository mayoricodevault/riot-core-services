package com.tierconnect.riot.commons.utils.epcDecoder;

import java.math.BigInteger;
import java.util.HashMap;
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
public enum BinaryEncodingScheme {

	SGTIN_96("110000",96,"sgtin-96","sgtin",3,PartitionTable.GTIN, PartitionTableType.PartitionTable, AdditionalField.SGTIN_96_SERIAL),
	SGTIN_198("110110",198,"sgtin-198","sgtin",3,PartitionTable.GTIN, PartitionTableType.PartitionTable, AdditionalField.SGTIN_198_SERIAL),

	SSCC_96("110001",96,"sscc-96","sscc",3,PartitionTable.SSCC, PartitionTableType.PartitionTable), //, com.mojix.ale.core.util.epc.AdditionalField.SSCC_96_RESERVED

	SGLN_96("110010",96,"sgln-96","sgln",3,PartitionTable.SGLN, PartitionTableType.PartitionTable, AdditionalField.SGLN_96_EXTENSION),
	SGLN_195("111001",195,"sgln-195","sgln",3,PartitionTable.SGLN, PartitionTableType.PartitionTable, AdditionalField.SGLN_195_EXTENSION),

	GRAI_96("110011",96,"grai-96","grai",3,PartitionTable.GRAI, PartitionTableType.PartitionTable, AdditionalField.GRAI_96_SERIAL),
	GRAI_170("110111",170,"grai-170","grai",3,PartitionTable.GRAI, PartitionTableType.PartitionTable, AdditionalField.GRAI_170_SERIAL),

	GIAI_96("110100",96,"giai-96","giai",3,PartitionTable.GIAI_96, PartitionTableType.UnpaddedPartitionTable),
	GIAI_202("111000",202,"giai-202","giai",3,PartitionTable.GIAI_202, PartitionTableType.StringPartitionTable),

	GSRN_96("101101",96,"gsrn-96","gsrn",3,PartitionTable.GSRN, PartitionTableType.PartitionTable), //, com.mojix.ale.core.util.epc.AdditionalField.GSRN_96_RESERVED

	GDTI_96("101100",96,"gdti-96","gdti",3,PartitionTable.GDTI, PartitionTableType.PartitionTable, AdditionalField.GDTI_96_SERIAL),
	GDTI_113("111010",113,"gdti-113","gdti",3,PartitionTable.GDTI, PartitionTableType.PartitionTable, AdditionalField.GDTI_113_SERIAL),

	GID_96("110101",96,"gid-96","gid",0,null, null, AdditionalField.GID_96_GENERAL_MANAGER_NUMBER, AdditionalField.GID_96_OBJECT_CLASS, AdditionalField.GID_96_SERIAL_NUMBER),

	USDoD_96("101111",96,"usdod-96","usdod",4,null, null, AdditionalField.DOD_GOV_MANAGED_ID, AdditionalField.DOD_SERIAL_NUMBER),

	ADI_Var("111011",434,"adi-var","adi",6,null, null, AdditionalField.ADI_CAGE, AdditionalField.ADI_PART_NUMBER_AND_SERIAL_NUMBER);

	//USDoD_96 what is the code?

	String binaryCode;
	int bitCount;
	String uriTagSchemeId;
	private AdditionalField[] additionalFields;
	private int[][] partitionTable;
	private PartitionTableType partitionTableType;
	private int filterBitsCount;
	private String pureIdentitySchemeId;

	private BinaryEncodingScheme(String binaryCode, int bitCount, String uriTagSchemeId, String pureIdentitySchemeId, int filterBitsCount, int[][]partitionTable, PartitionTableType partitionTableType, AdditionalField ... additionalFields) {
		this.binaryCode = binaryCode;
		this.bitCount = bitCount;
		this.uriTagSchemeId = uriTagSchemeId;
		this.additionalFields = additionalFields;
		this.partitionTable = partitionTable;
		this.partitionTableType = partitionTableType;
		this.filterBitsCount = filterBitsCount;
		this.pureIdentitySchemeId = pureIdentitySchemeId;
	}

	public String getBinaryCode() {
		return binaryCode;
	}

	public int getBitCount() {
		return bitCount;
	}

	public String getUriTagSchemeId() {
		return uriTagSchemeId;
	}

	public String getPureIdentitySchemeId() {
		return pureIdentitySchemeId;
	}

	public AdditionalField[] getAdditionalFields() {
		return additionalFields;
	}

	public int[][] getPartitionTable() {
		return partitionTable;
	}

	public PartitionTableType getPartitionTableType() {
		return partitionTableType;
	}

	public int getFilterBitsCount() {
		return filterBitsCount;
	}

	static Map<String, BinaryEncodingScheme> codeToSchemeMap = new HashMap<String, BinaryEncodingScheme>();
	static{
		for(BinaryEncodingScheme scheme : BinaryEncodingScheme.values()){
			codeToSchemeMap.put(scheme.getBinaryCode(), scheme);
		}
	}

	static Map<Long, BinaryEncodingScheme> longToSchemeMap = new HashMap<Long, BinaryEncodingScheme>();
	static{
		for(BinaryEncodingScheme scheme : BinaryEncodingScheme.values()){
			String binCode = scheme.getBinaryCode();
			long value = (new BigInteger(binCode , 2)).longValue();
			//System.out.println(value);
			longToSchemeMap.put(value, scheme);
		}
	}
	public static Map<String, BinaryEncodingScheme> getCodeToSchemeMap() {
		return codeToSchemeMap;
	}

	public static void setCodeToSchemeMap(
			Map<String, BinaryEncodingScheme> codeToSchemeMap) {
		BinaryEncodingScheme.codeToSchemeMap = codeToSchemeMap;
	}

	public static Map<Long, BinaryEncodingScheme> getLongToSchemeMap() {
		return longToSchemeMap;
	}

	public static void setLongToSchemeMap(
			Map<Long, BinaryEncodingScheme> longToSchemeMap) {
		BinaryEncodingScheme.longToSchemeMap = longToSchemeMap;
	}



}
