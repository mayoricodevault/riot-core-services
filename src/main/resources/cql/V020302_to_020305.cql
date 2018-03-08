CREATE TABLE exit_report (
  thingType_id bigint,
  group_id     bigint,
  thing_id     bigint,
  time         timestamp,
  value    text,
  PRIMARY KEY(thing_id,time)
);

CREATE INDEX ON exit_report( thingType_id );
CREATE INDEX ON exit_report( group_id );
CREATE INDEX ON exit_report( time );