DROP PROCEDURE IF EXISTS riot_main.hazelcastMapFix_;
DELIMITER $$ CREATE PROCEDURE riot_main.hazelcastMapFix_()
    BEGIN
        DECLARE aux1 bigint(20);
        DECLARE aux2 bigint(20);
        DECLARE aux1_new bigint(20);
        DECLARE aux2_new bigint(20);
        SELECT count(*)
        INTO aux1
        FROM information_schema.tables
        WHERE table_schema = 'riot_main'
        AND table_name = 'apc_thingtypemap';
        IF aux1 = 1 THEN
            SELECT count(*)
             INTO aux1_new
            FROM information_schema.tables
            WHERE table_schema = 'riot_main'
            AND table_name = 'thingtypemap';
            IF aux1_new = 0 THEN
                CREATE TABLE IF NOT EXISTS riot_main.thingtypemap (
                    id bigint(20) NOT NULL,
                    child_id bigint(20) DEFAULT NULL,
                    parent_id bigint(20) DEFAULT NULL
                ) AUTO_INCREMENT=100;
                ALTER TABLE riot_main.thingtypemap
                ADD PRIMARY KEY (id),
                ADD KEY FK_thing_typemap_thingtype_c (child_id),
                ADD KEY FK_thing_typemap_thingtype_p (parent_id);
                ALTER TABLE riot_main.thingtypemap
                MODIFY id bigint(20) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=100;
                ALTER TABLE riot_main.thingtypemap
                ADD CONSTRAINT FK_thing_typemap_thingtype_p FOREIGN KEY (parent_id) REFERENCES thingtype (id),
                ADD CONSTRAINT FK_thing_typemap_thingtype_c FOREIGN KEY (child_id) REFERENCES thingtype (id);
                INSERT INTO riot_main.thingtypemap(child_id, parent_id) SELECT a.child_id, a.parent_id FROM  apc_thingtypemap a;
            END IF;
            DROP TABLE riot_main.apc_thingtypemap;
        END IF;

        SELECT count(*)
        INTO aux2
        FROM information_schema.tables
        WHERE table_schema = 'riot_main'
              AND table_name = 'notification_template';
        IF aux2 = 1 THEN
            SELECT count(*)
            INTO aux1_new
            FROM information_schema.tables
            WHERE table_schema = 'riot_main'
                  AND table_name = 'notificationtemplate';
            IF aux2_new = 0 THEN
                CREATE TABLE IF NOT EXISTS riot_main.notificationtemplate (
                    id bigint(20) NOT NULL,
                    templateBody longtext,
                    templateName varchar(255) DEFAULT NULL
                ) AUTO_INCREMENT=100;
                ALTER TABLE riot_main.notificationtemplate
                ADD PRIMARY KEY (id);
                ALTER TABLE riot_main.notificationtemplate
                MODIFY id bigint(20) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=100;
                INSERT INTO riot_main.notificationtemplate(templateBody, templateName) SELECT a.templateBody, a.templateName FROM notification_template a;
            END IF;
            DROP TABLE riot_main.notification_template;
        END IF;
    END  $$ DELIMITER;
CALL riot_main.hazelcastMapFix_();
DROP PROCEDURE riot_main.hazelcastMapFix_;