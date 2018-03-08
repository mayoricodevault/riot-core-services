UPDATE Favorite SET elementGroupId=elementId WHERE typeElement='group';
UPDATE Favorite SET elementGroupId=1 WHERE elementGroupId is NULL;
ALTER TABLE Favorite CHANGE elementGroupId group_id bigint(20);
ALTER TABLE Favorite ADD CONSTRAINT FK_favoriteGroup FOREIGN KEY (group_id) REFERENCES Group0(id);
