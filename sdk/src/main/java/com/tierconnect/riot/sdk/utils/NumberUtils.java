package com.tierconnect.riot.sdk.utils;

public class NumberUtils {

	public static boolean equals(Long l1, Long l2) {
		if (l1 == l2) {
			return true;
		}
		if (l1 == null) {
			return false;
		}
		return l1.equals(l2);
	}
	
	public static void main(String[] args) {
		System.out.println(equals(1L, 1L));
		System.out.println(equals(new Long(1), new Long(1)));
		System.out.println(equals(1L, new Long(1)));
		System.out.println(equals(null, null));

		System.out.println("--------------");
		
		System.out.println(equals(null, 1L));
		System.out.println(equals(1L, null));

	}
	
}
