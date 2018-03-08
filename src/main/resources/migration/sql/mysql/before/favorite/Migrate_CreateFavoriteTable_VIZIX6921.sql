CREATE TABLE IF NOT EXISTS  `favorite` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID of the table',
  `date`  bigint(20)  COMMENT 'timestamp of creation',
  `elementId` bigint(20) COMMENT 'Favorite element id',
  `elementName` varchar(255) COMMENT 'Favorite element name',
  `sequence` bigint(20) COMMENT 'Sequence value to order favorites',
  `status` varchar(255) COMMENT 'Favorite status',
  `typeElement` varchar(255) COMMENT 'Favorite type of element',
  `elementGroupId` bigint(20) COMMENT 'group id of the element',
  `user_id` bigint(20) COMMENT 'user favorite element',
  PRIMARY KEY (`id`)
  );

 ALTER TABLE riot_main.favorite
                ADD CONSTRAINT FK_favorite_user FOREIGN KEY (user_id) REFERENCES user0 (id);