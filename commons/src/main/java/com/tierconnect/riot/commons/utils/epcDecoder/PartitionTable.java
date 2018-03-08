package com.tierconnect.riot.commons.utils.epcDecoder; /*************************************************************************
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
public class PartitionTable {
	//first array subscript has 7 elements(the first subscript is the Partition Value)
		//second array subscript has 4 elements: FIRST_SEGMENT_BITS, FIRST_SEGMENT_DIGITS, SECOND_SEGMENT_BITS, SECOND_SEGMENT_DIGITS.  
		public static final int[][]GTIN = new int [][]{
			{40,  12, 4,  1},
			{37,  11, 7,  2},
			{34,  10, 10, 3},
			{30,  9,  14, 4},
			{27,  8,  17, 5},
			{24,  7,  20, 6},
			{20,  6,  24, 7},
		};		
		public static final int[][]SSCC = new int [][]{
			{40,  12, 18, 5},
			{37,  11, 21, 6},
			{34,  10, 24, 7},
			{30,  9,  28, 8},
			{27,  8,  31, 9},
			{24,  7,  34, 10},
			{20,  6,  38, 11},
		};		
		public static final int[][]SGLN = new int [][]{
			{40,  12, 1,  0},
			{37,  11, 4,  1},
			{34,  10, 7,  2},
			{30,  9,  11, 3},
			{27,  8,  14, 4},
			{24,  7,  17, 5},
			{20,  6,  21, 6},
		};
		public static final int[][]GRAI = new int [][]{
			{40,  12, 4,  0},
			{37,  11, 7,  1},
			{34,  10, 10, 2},
			{30,  9,  14, 3},
			{27,  8,  17, 4},
			{24,  7,  20, 5},
			{20,  6,  24, 6},
		};
		public static final int[][]GIAI_96 = new int [][]{
			{40,  12, 42,  13},
			{37,  11, 45,  14},
			{34,  10, 48,  15},
			{30,  9,  52,  16},
			{27,  8,  55,  17},
			{24,  7,  58,  18},
			{20,  6,  62,  19},
		};
		public static final int[][]GIAI_202 = new int [][]{
			{40,  12, 148,  18},
			{37,  11, 151,  19},
			{34,  10, 154,  20},
			{30,  9,  158,  21},
			{27,  8,  161,  22},
			{24,  7,  164,  23},
			{20,  6,  168,  24},
		};
		public static final int[][]GSRN = new int [][]{
			{40,  12, 18, 5},
			{37,  11, 21, 6},
			{34,  10, 24, 7},
			{30,  9,  28, 8},
			{27,  8,  31, 9},
			{24,  7,  34, 10},
			{20,  6,  38, 11},
		};
		public static final int[][]GDTI = new int [][]{
			{40,  12, 1,  0},
			{37,  11, 4,  1},
			{34,  10, 7,  2},
			{30,  9,  11, 3},
			{27,  8,  14, 4},
			{24,  7,  17, 5},
			{20,  6,  21, 6},
		};
}
