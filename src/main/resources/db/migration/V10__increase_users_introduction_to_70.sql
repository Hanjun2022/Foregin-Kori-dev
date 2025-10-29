-- Expand users.introduction to 70 chars
ALTER TABLE users
ALTER COLUMN introduction TYPE varchar(70);
