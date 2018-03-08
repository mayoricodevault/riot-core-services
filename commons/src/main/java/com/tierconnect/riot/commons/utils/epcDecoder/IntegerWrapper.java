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
public class IntegerWrapper {
	
	private final int [] memory;
	private final int BITS_PER_INT = 32;
	
	public IntegerWrapper(final int maximumBits){
		double bitCasted = maximumBits;
		Double maxNumber = Math.ceil(bitCasted / BITS_PER_INT);
		int maxNumberOfIntegers = maxNumber.intValue();
		memory = new int[maxNumberOfIntegers];
	}
	
	public long getBitsAsLong(int fromBit, int toBit){
		long result = 0;
		for(int bitLocation=fromBit; bitLocation<=toBit; bitLocation++){
			int integerIndex = (int)(bitLocation / 32);
			int bitLocationInInteger = bitLocation % 32;
			long bitValue = (memory[integerIndex] >> bitLocationInInteger) & 1; //get the LSB after shifting to the right
			result += (bitValue << (bitLocation - fromBit)); //now shift to the left the required number of positions and add to the result			
		}
		return result;
	}
	
	public String getBitsAsBinaryString(int fromBit, int toBit){
		//long result = 0;
		StringBuilder sb = new StringBuilder();
		for(int bitLocation=fromBit; bitLocation<=toBit; bitLocation++){
			int integerIndex = (int)(bitLocation / 32);
			int bitLocationInInteger = bitLocation % 32;
			long bitValue = (memory[integerIndex] >> bitLocationInInteger) & 1; //get the LSB after shifting to the right
			//result += (bitValue << (bitLocation - fromBit)); //now shift to the left the required number of positions and add to the result
			sb.append(bitValue);
		}
		return sb.reverse().toString();
	}
	
	public long [] getBitsAsArrayOfLong(int fromBit, int toBit){
		int bitCount = toBit - fromBit + 1;
		int integersRequired = (int) Math.ceil(bitCount/32D);
		long [] longs = new long[integersRequired];
		
		for(int currentIntIndex = 0, startBitIndex = fromBit; startBitIndex <= toBit; startBitIndex+=32,currentIntIndex++){
			int endBitIndex =  currentIntIndex == integersRequired-1 ? toBit : startBitIndex + 31;
			longs[currentIntIndex] = getBitsAsLong(startBitIndex, endBitIndex);
		}
		return longs;
	}
	
	//I am intentionally making this one a mutable object to allow reuse and avoid reconstruction
	public void setValue(String hexValue) {
		
		// TODO Auto-generated method stub
		int length = hexValue.length();
		double lengthCasted = length;
		//maximum 8 hexadecimal digits per Integer (signed)
		Double maxNumber = Math.ceil(lengthCasted/8);
		int numberOfIntegers = maxNumber.intValue();
		for(int integerIndex = 0; integerIndex <numberOfIntegers; integerIndex++){
			int toHexIndex = length - integerIndex * 8;
			int fromHexIndex = toHexIndex - 8 < 0 ? 0 : toHexIndex - 8;
			
			//for 1M tags takes 75ms
			String part = hexValue.substring(fromHexIndex, toHexIndex);
			//for 1M tags takes 220ms
			int signedInt = (int)Long.valueOf(part, 16).intValue(); //hexToSignedInt(part);
			memory[integerIndex] = signedInt;
			
			//BigInteger bi = new BigInteger(part,16); 
			//String bs = bi.toString(2);
			//System.out.println(bs);
			//System.out.println(Integer.toBinaryString(memory[integerIndex]));
			
		}
	}

	//convert up to 4 hex digits to a signed integer 
	public static int hexToSignedInt(String hex) {
		Long converted = 0L;
		for(int i=hex.length()-1; i>=0; i--){
			char hexChar = hex.charAt(i);
			int intValue = hexChar;
			intValue = intValue <= 57 ? intValue - 48 : intValue <= 97 ? intValue -55 : intValue -87;
			//System.out.println(intValue);
			int hexPosition = hex.length() - i -1;
			//150 ms per 1000000 tags
			converted += intValue << 4 * hexPosition;
			//System.out.printf("%s[%s]%n", converted, Integer.toBinaryString(converted));
		}
		return converted.intValue();
	}
	
	public static void main(String[] args) {
		//test1();
		//test2();
		//test3();
		//test4();
		//test5();
		//test6();
		//test7();
		perf1();
	}
	
	
	private static void test1() {
		int converted = hexToSignedInt("FFFFFFFF");		
	}

	public static void test2(){
		//this is 32 bits + 32 bits + 32 bits + 32 bits = 128 bits 
		String hex1 = "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" ;
		IntegerWrapper iw = new IntegerWrapper(256);
		long startTime = System.currentTimeMillis();
		for(int i=0; i<100000000; i++){
			iw.setValue(hex1);
		}
		long endTime = System.currentTimeMillis();
		long totalProcessingTime = endTime - startTime;
		System.out.println("Processing Time: " + totalProcessingTime);		
	}
	
	public static void test3(){
		//this is 32 bits + 32 bits + 32 bits + 32 bits = 128 bits 
		String hex1 = "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" ;
		int[]results = new int[100000000];
		long startTime = System.currentTimeMillis();
		
		for(int i=0; i<100000000; i++){
			BigInteger bi = new BigInteger(hex1,16);
		}
		
		long endTime = System.currentTimeMillis();
		long totalProcessingTime = endTime - startTime;
		System.out.println(results[0]);
		System.out.println("Processing Time: " + totalProcessingTime);		
	}
	
	public static void test4(){
		//this is 32 bits + 32 bits + 32 bits + 32 bits = 128 bits 
		String hex1 = "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" ;
		IntegerWrapper iw = new IntegerWrapper(256);
		long startTime = System.currentTimeMillis();
		for(int i=0; i<100000000; i++){
			iw.setValue(hex1);
			long result = iw.getBitsAsLong(0, 44);
		}
		long endTime = System.currentTimeMillis();
		long totalProcessingTime = endTime - startTime;
		System.out.println("Processing Time: " + totalProcessingTime);		
	}
	
	public static void test5(){
		//this is 32 bits + 32 bits + 32 bits + 32 bits = 128 bits 
		/*
		String s1 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s2 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s3 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s4 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		*/
		
		/*
		String s1 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s2 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s3 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s4 = "10001010" + "10001111" + "10001001" + "10001011"; //32 bit
		*/
		
		String s1 = "10001000" + "10000100" + "10000010" + "10000001"; //32 bit
		String s2 = "10001000" + "10000100" + "10000010" + "10000001"; //32 bit
		String s3 = "10000000" + "11000000" + "10100000" + "10010000"; //32 bit
		String s4 = "10001000" + "10000100" + "10000010" + "10000101"; //32 bit
		
		int fromBit = 0;
		int toBit = 62;
		
		String bin = s1+s2+s3+s4;
		BigInteger bi = new BigInteger(bin,2);
		IntegerWrapper iw = new IntegerWrapper(256);
		String hex = bi.toString(16);
		iw.setValue(hex);
		long result = iw.getBitsAsLong(fromBit, toBit);
		BigInteger resultBi = new BigInteger(Long.toString(result)); 
		
		String resultBinaryString = resultBi.toString(2);
		String expectedResult = bin.substring(bin.length()-toBit-1, bin.length()-fromBit);
		int lastLeadingZeroIndex = -1;
		for(int i=0; i<expectedResult.length(); i++){
			if(expectedResult.charAt(i) == '0'){
				lastLeadingZeroIndex = i;
			}
			else{
				break;
			}
		}
		expectedResult = expectedResult.substring(lastLeadingZeroIndex+1, expectedResult.length());
		
		if(resultBinaryString.equals(expectedResult)){
			System.out.println("Success");
		}
		else{
			System.out.println("Failure");
		}
		
		System.out.println(resultBinaryString);
		System.out.println(expectedResult);
	}
	
	public static void test6(){
		//this is 32 bits + 32 bits + 32 bits + 32 bits = 128 bits 
		/*
		String s1 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s2 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s3 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s4 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		*/
		
		/*
		String s1 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s2 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s3 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s4 = "10001010" + "10001111" + "10001001" + "10001011"; //32 bit
		*/
		/*
		String s1 = "10001000" + "10000100" + "10000010" + "10000001"; //32 bit
		String s2 = "10001000" + "10000100" + "10000010" + "10000001"; //32 bit
		String s3 = "10000000" + "11000000" + "10100000" + "10010000"; //32 bit
		String s4 = "10001000" + "10000100" + "10000010" + "10000101"; //32 bit
		*/
		
		String s1 = "11111111" + "11111111" + "11111111" + "11111111"; //32 bit
		String s2 = "11111111" + "11111111" + "11111111" + "11111111"; //32 bit
		String s3 = "11111111" + "11111111" + "11111111" + "11111111"; //32 bit
		String s4 = "11111111" + "11111111" + "11111111" + "11111111"; //32 bit
		
		
		int fromBit = 0;
		int toBit = 127;
		
		String bin = s1+s2+s3+s4;
		BigInteger bi = new BigInteger(bin,2);
		IntegerWrapper iw = new IntegerWrapper(128);
		String hex = bi.toString(16);
		iw.setValue(hex);
		

		String resultBinaryString = iw.getBitsAsBinaryString(fromBit, toBit);
		
		
		String expectedResult = bin.substring(bin.length()-toBit-1, bin.length()-fromBit);
		int lastLeadingZeroIndex = -1;
		for(int i=0; i<expectedResult.length(); i++){
			if(expectedResult.charAt(i) == '0'){
				lastLeadingZeroIndex = i;
			}
			else{
				break;
			}
		}
		expectedResult = expectedResult.substring(lastLeadingZeroIndex+1, expectedResult.length());
		
		if(resultBinaryString.equals(expectedResult)){
			System.out.println("Success");
		}
		else{
			System.out.println("Failure");
		}
		
		System.out.println(resultBinaryString);
		System.out.println(expectedResult);
	}

	public static void test7(){
		//this is 32 bits + 32 bits + 32 bits + 32 bits = 128 bits 
		/*
		String s1 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s2 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s3 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s4 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		*/
		
		/*
		String s1 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s2 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s3 = "10001000" + "10001000" + "10001000" + "10001000"; //32 bit
		String s4 = "10001010" + "10001111" + "10001001" + "10001011"; //32 bit
		*/
		/*
		String s1 = "10001000" + "10000100" + "10000010" + "10000001"; //32 bit
		String s2 = "10001000" + "10000100" + "10000010" + "10000001"; //32 bit
		String s3 = "10000000" + "11000000" + "10100000" + "10010000"; //32 bit
		String s4 = "10001000" + "10000100" + "10000010" + "10000101"; //32 bit
		*/
		
		String s1 = "11111111" + "11111111" + "11111111" + "11111111"; //32 bit
		String s2 = "11111111" + "11111111" + "11111111" + "11111111"; //32 bit
		String s3 = "11111111" + "11111111" + "11111111" + "11111111"; //32 bit
		String s4 = "11111111" + "11111111" + "11111111" + "11111110"; //32 bit
		
		
		int fromBit = 0;
		int toBit = 127;
		
		String bin = s1+s2+s3+s4;
		BigInteger bi = new BigInteger(bin,2);
		IntegerWrapper iw = new IntegerWrapper(128);
		String hex = bi.toString(16);
		iw.setValue(hex);
		

		long[] longs = iw.getBitsAsArrayOfLong(fromBit, toBit);
		String resultBinaryString = "";
		
		for(int i=longs.length-1; i>=0; i--){
			String binaryString = Long.toBinaryString(longs[i]);
			resultBinaryString += binaryString;
		}
		
		String expectedResult = bin.substring(bin.length()-toBit-1, bin.length()-fromBit);
		int lastLeadingZeroIndex = -1;
		for(int i=0; i<expectedResult.length(); i++){
			if(expectedResult.charAt(i) == '0'){
				lastLeadingZeroIndex = i;
			}
			else{
				break;
			}
		}
		expectedResult = expectedResult.substring(lastLeadingZeroIndex+1, expectedResult.length());
		
		if(resultBinaryString.equals(expectedResult)){
			System.out.println("Success");
		}
		else{
			System.out.println("Failure");
		}
		
		System.out.println(resultBinaryString);
		System.out.println(expectedResult);
	}

	public static void perf1(){
		//this is 32 bits + 32 bits + 32 bits + 32 bits = 128 bits 
		String hex1 = "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" + "FFFFFFFF" ;
		IntegerWrapper iw = new IntegerWrapper(128);
		long startTime = System.currentTimeMillis();
		
		//String lastBits = null;
		for(int i=0; i<1000000; i++){
			
			iw.setValue(hex1);
			//iw.getBitsAsArrayOfLong(0, 127);
			//iw.getBitsAsBinaryString(0, 127);
			//iw.getBitsAsLong(0, 127);
			
			//BigInteger bi = new BigInteger(hex1,16);
			//BigInteger result = bi.and(bitMask);
			//String binaryString1 = result.toString(2);
			
			//String binaryString2 = bi.toString(2);
			//String subs = binaryString2.substring(0, 126);			
		}
		long endTime = System.currentTimeMillis();
		long totalProcessingTime = endTime - startTime;
		//System.out.println(lastBits);
		System.out.println("Processing Time: " + totalProcessingTime);	
	}
	
}
