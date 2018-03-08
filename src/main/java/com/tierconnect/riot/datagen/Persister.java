package com.tierconnect.riot.datagen;

import java.util.Set;

/**
 * FOR TIME LAST VALUE DATA
 */
public interface Persister
{

	void start();

	long getMaxId();
	
	void persist( Thing thing );

	void persist( Set<Thing> things );
	
	void end();
}
