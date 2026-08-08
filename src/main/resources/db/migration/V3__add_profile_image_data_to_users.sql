ALTER TABLE users
ADD COLUMN image_data BYTEA,
ADD COLUMN image_content_type VARCHAR(100);