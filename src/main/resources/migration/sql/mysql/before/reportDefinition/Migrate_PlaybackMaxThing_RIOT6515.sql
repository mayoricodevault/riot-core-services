ALTER TABLE reportDefinition ADD COLUMN playbackMaxThing int(11);
UPDATE reportDefinition SET playbackMaxThing = 100 ;
