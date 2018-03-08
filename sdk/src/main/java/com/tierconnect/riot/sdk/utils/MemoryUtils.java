package com.tierconnect.riot.sdk.utils;

public class MemoryUtils
{
	public static double used;
	public static double free;
	public static double total;
	public static double max;

	public static String get()
	{
		long mb = 1024 * 1024;

		Runtime runtime = Runtime.getRuntime();

		used = (runtime.totalMemory() - runtime.freeMemory()) / mb;

		free = runtime.freeMemory() / mb;

		total = runtime.totalMemory() / mb;

		max = runtime.maxMemory() / mb;

		return String.format( "memory usage (in MB): used=%.1f free=%.1f total=%.1f max=%.1f", used, free, total, max );
	}
}
