DROP TABLE IF EXISTS `thingtypetemplatecategory`;

CREATE TABLE `thingtypetemplatecategory`
(
    id BIGINT(20) PRIMARY KEY NOT NULL AUTO_INCREMENT COMMENT 'ID of category',
    code VARCHAR(100) NOT NULL COMMENT 'The code of category',
    displayOrder INT(11) NOT NULL COMMENT 'Display order of the category',
    name VARCHAR(100) NOT NULL COMMENT 'The name of the category',
    pathIcon VARCHAR(100) NOT NULL COMMENT 'The name icon style'
);

INSERT INTO thingtypetemplatecategory (code, displayOrder, name, pathIcon) VALUES ("CUSTOM", 0, "Custom", "");

ALTER TABLE `thingtypetemplate` ADD COLUMN code varchar(100);
ALTER TABLE `thingtypetemplate` ADD COLUMN displayOrder INT(11);
ALTER TABLE `thingtypetemplate` ADD COLUMN thingTypeTemplateCategory_id BIGINT(20);
UPDATE thingtypetemplate set code=name, displayOrder=0, thingTypeTemplateCategory_id=1;
ALTER TABLE `thingtypetemplate`
  ADD CONSTRAINT FK_thingtypetemplate_category FOREIGN KEY (thingTypeTemplateCategory_id)
  REFERENCES thingtypetemplatecategory (id);


