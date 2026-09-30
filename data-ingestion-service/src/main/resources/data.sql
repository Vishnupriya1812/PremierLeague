
INSERT INTO competitions (id, name, country)
SELECT 17, 'Premier League', 'England'
WHERE NOT EXISTS (SELECT 1 FROM competitions WHERE id = 17);
