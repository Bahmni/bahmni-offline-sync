CREATE TABLE event_log
(
  id INT(11) PRIMARY KEY NOT NULL,
  uuid VARCHAR(40),
  timestamp TIMESTAMP NOT NULL,
  object VARCHAR(1000),
  category VARCHAR(255),
  filter VARCHAR(255),
  parent_uuid VARCHAR(40)
);