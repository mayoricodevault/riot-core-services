package com.tierconnect.riot.commons.utils.epcDecoder;
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
public enum AdditionalField {
	SGTIN_96_SERIAL(AdditionalFieldType.Integer, 38, 0, 37),
	SGTIN_198_SERIAL(AdditionalFieldType.String, 140, 0, 139),
	SSCC_96_RESERVED(AdditionalFieldType.Integer, 24, 0, 23),
	SGLN_96_EXTENSION(AdditionalFieldType.Integer, 41, 0, 40),
	SGLN_195_EXTENSION(AdditionalFieldType.String, 140, 0, 139),
	GRAI_96_SERIAL(AdditionalFieldType.Integer, 38, 0, 37),
	GRAI_170_SERIAL(AdditionalFieldType.String, 112, 0, 111),
	GSRN_96_RESERVED(AdditionalFieldType.Integer, 24, 0, 23),
	GDTI_96_SERIAL(AdditionalFieldType.Integer, 41, 0, 40),
	GDTI_113_SERIAL(AdditionalFieldType.NumericString, 58, 0, 57),
	GID_96_GENERAL_MANAGER_NUMBER(AdditionalFieldType.Integer, 28, 60, 87),
	GID_96_OBJECT_CLASS(AdditionalFieldType.Integer, 24, 36, 59),
	GID_96_SERIAL_NUMBER(AdditionalFieldType.Integer, 36, 0, 35),

	ADI_CAGE(AdditionalFieldType.SixBitCageDodAac, 36, -1, -1),
	ADI_PART_NUMBER_AND_SERIAL_NUMBER(AdditionalFieldType.SixBitVariableString, 6, -1, -1),

	DOD_GOV_MANAGED_ID(AdditionalFieldType.EightBitCageDodAac, 36, 36, 83),
	DOD_SERIAL_NUMBER(AdditionalFieldType.Integer, 36, 0, 35);

	AdditionalFieldType type;
	int size;
	int fromBitIndex;
	int toBitIndex;

	private AdditionalField(AdditionalFieldType type, int size,
			int fromBitIndex, int toBitIndex) {
		this.type = type;
		this.size = size;
		this.fromBitIndex = fromBitIndex;
		this.toBitIndex = toBitIndex;
	}
	public AdditionalFieldType getType() {
		return type;
	}
	public int getSize() {
		return size;
	}
	public int getFromBitIndex() {
		return fromBitIndex;
	}
	public int getToBitIndex() {
		return toBitIndex;
	}

	public enum AdditionalFieldType {
		Integer,String,NumericString,SixBitCageDodAac,SixBitVariableString,Zero, EightBitCageDodAac;
	}

}
