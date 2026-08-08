CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE skills (
    id SERIAL PRIMARY KEY,
    category_id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT fk_skills_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id)
);

CREATE TABLE learning_data (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    skill_id INTEGER NOT NULL,
    learning_minutes INTEGER NOT NULL DEFAULT 0,
    learning_month DATE NOT NULL,

    CONSTRAINT fk_learning_data_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_learning_data_skill
        FOREIGN KEY (skill_id)
        REFERENCES skills(id),

    CONSTRAINT uq_learning_data
        UNIQUE (user_id, skill_id, learning_month)
);

INSERT INTO categories (name)
VALUES
    ('バックエンド'),
    ('フロントエンド'),
    ('インフラ');

INSERT INTO skills (category_id, name)
VALUES
    ((SELECT id FROM categories WHERE name = 'バックエンド'), 'Ruby'),
    ((SELECT id FROM categories WHERE name = 'バックエンド'), 'Rails'),
    ((SELECT id FROM categories WHERE name = 'バックエンド'), 'MySQL'),

    ((SELECT id FROM categories WHERE name = 'フロントエンド'), 'HTML'),
    ((SELECT id FROM categories WHERE name = 'フロントエンド'), 'CSS'),

    ((SELECT id FROM categories WHERE name = 'インフラ'), 'Heroku'),
    ((SELECT id FROM categories WHERE name = 'インフラ'), 'AWS'),
    ((SELECT id FROM categories WHERE name = 'インフラ'), 'Firebase');