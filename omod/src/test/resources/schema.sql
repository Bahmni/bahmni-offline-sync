-- This Source Code Form is subject to the terms of the Mozilla Public License,
-- v. 2.0. If a copy of the MPL was not distributed with this file, You can
-- obtain one at https://www.bahmni.org/license/mplv2hd.
--
-- Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
-- graphic logo is a trademark of OpenMRS Inc.

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