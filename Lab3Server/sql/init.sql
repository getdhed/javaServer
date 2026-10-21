-- Выполнять внутри базы lab3_dictionary (UTF8) от пользователя с правом CREATE.
CREATE TABLE IF NOT EXISTS translations (
    id BIGSERIAL PRIMARY KEY,
    word_ru TEXT NOT NULL UNIQUE,
    word_en TEXT NOT NULL UNIQUE,
    CHECK (word_ru = LOWER(word_ru)),
    CHECK (word_en = LOWER(word_en))
);
INSERT INTO translations (word_ru, word_en) VALUES
 ('кот','cat'),('собака','dog'),('дом','house'),('книга','book'),
 ('студент','student'),('университет','university'),('окно','window'),
 ('солнце','sun'),('вода','water'),('друг','friend'),('школа','school'),
 ('яблоко','apple'),('мама','mother'),('папа','father'),('город','city')
ON CONFLICT DO NOTHING;
