#!/usr/bin/perl
use strict;
use warnings;

my $i;
my $time = 0;
my $tag;
my $x;
my $y;
my $lr;
my $enode;

print "EPOCH,2015-01-28 08:00:00 EST\n";
print "DELT,REL\n";

print "CS,-118.443969,34.048092,0.0,20.0,ft\n";
for( $i = 0; $i < 10; $i++ )
{
	$time += 0;
	$tag = $i;
	$x = 10 * $i;
	$y = 10 * $i;
	$lr = "LR5";
	$enode = "x3ed9371";
	#print "CS,-118.443969,34.048092,0.0,20.0,ft\n";
	printf( "LOC,%d,%021d,%f,%f,0,%s,%s\n", $time, $tag, $x, $y, $lr, $enode );
	print("\n");
}
