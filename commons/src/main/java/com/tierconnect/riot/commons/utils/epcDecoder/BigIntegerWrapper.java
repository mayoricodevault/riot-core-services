package com.tierconnect.riot.commons.utils.epcDecoder;

import java.math.BigInteger;
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
public class BigIntegerWrapper {

	private int missingMsbZeros;
	private int bitsCount;
	static final BigInteger zero = BigInteger.ZERO;
	static final BigInteger bigMax = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF", 16); //40 hex, 160 bit
	//static final BigInteger max = BigInteger.valueOf(Long.MAX_VALUE);
	BigInteger wrapped = null;
	static final BigInteger ONE = BigInteger.valueOf(1);

	public BigIntegerWrapper(String hex) {
		wrapped = new BigInteger(hex, 16);
		this.bitsCount = wrapped.bitLength();
		this.missingMsbZeros = hex.length() * 4 - bitsCount;
	}
	
	public BigIntegerWrapper(BigInteger fieldValue) {
		wrapped = fieldValue;
	}

	public void changeValue(BigInteger bi) {
		this.wrapped = bi;
		this.bitsCount = wrapped.bitLength();		
	}

	public BigInteger getBitsRev(int fromMsb, int toMsb){
		int size = this.bitsCount + this.missingMsbZeros;
		int to = size - fromMsb -1;
		int from = size - toMsb -1;
		return getBits(from, to);
	}
	
	//inclusive
	public BigInteger getBits(int fromLsb, int toLsb){
		int bits = (toLsb - fromLsb) + 1;
		BigInteger shifted = wrapped.shiftRight(fromLsb);
		BigInteger bitMask = prepareMask(bits);
		BigInteger result = shifted.and(bitMask);
		return result;
	}

	private BigInteger prepareMask(int bits) {
		return ONE.shiftLeft(bits).add(BigInteger.valueOf(-1));
		//return bigMax.shiftRight(bigMax.bitCount()-bits);		
	}
	
	public static void main(String[] args) {
		/*
		com.mojix.ale.core.util.epc.BigIntegerWrapper biw = new com.mojix.ale.core.util.epc.BigIntegerWrapper(new BigInteger("100000000010111", 2).toString(16));
		BigInteger result = biw.getBits(1, 14);
		System.out.println(result.toString(2));
		*/
		BigIntegerWrapper biw = new BigIntegerWrapper(new BigInteger("100000000010111", 2).toString(16));
		BigInteger result = biw.getBits(1, 15);
		System.out.println(result.toString(2));
	}

	public int bitLength() {
		return this.bitsCount;
	}

	public int getMissingMsbZeros() {
		return missingMsbZeros;
	}

	public void setMissingMsbZeros(int missingMsbZeros) {
		this.missingMsbZeros = missingMsbZeros;
	}

	public BigInteger getValue() {		
		return wrapped;
	}
	
	
	

}
