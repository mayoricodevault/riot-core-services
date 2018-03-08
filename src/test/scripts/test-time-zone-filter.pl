#!/usr/bin/perl

# prerequisites popdbmojix retail

# in ALEbridge, set timDistanceFilter t=0, d=10

# set time zone filter t=3


use strict;
use warnings;

system( "mosquitto_pub", "-t", "/v1/edge/dn/ALEB/log4j/logger", "-m", "com.tierconnect.riot.iot.connectors.ale.AleBridge=debug" );
system( "mosquitto_pub", "-t", "/v1/edge/dn/ALEB/log4j/logger", "-m", "com.tierconnect.riot.iot.connectors.ale.AleBridge.SimpleMessageFormat=debug" );
system( "mosquitto_pub", "-t", "/v1/edge/dn/ALEB/log4j/logger", "-m", "com.tierconnect.riot.iot.connectors.TimeZoneFilter=trace" );

my $i;
my $time = 0;
my $tag;
my $x;
my $y;

print "EPOCH,2015-01-28 08:00:00 EST\n";
print "DELT,REL\n";

for( $i = 0; $i < 100; $i++ )
{
        $tag = 1;
	$x = -5 + 1 * $i;
	$y =  9.3;
	print "CS,-118.443969,34.048092,0.0,20.0,ft\n";
	printf( "LOC,%d,%021d,%f,%f,0,LR5,x3ed9371\n", $time, $tag, $x, $y );
	print("\n");

	$time += 1000;
}


