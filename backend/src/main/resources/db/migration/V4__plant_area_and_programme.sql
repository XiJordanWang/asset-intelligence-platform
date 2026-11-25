-- Where in the plant each asset is used, and the investment programme it was bought under.
ALTER TABLE asset ADD COLUMN area VARCHAR(30);
ALTER TABLE asset ADD COLUMN programme VARCHAR(40);

-- Give existing assets a sensible area from their class (the demo seeder assigns more detailed ones).
UPDATE asset SET area = CASE category
    WHEN 'PLANT_MACHINERY' THEN 'ASSEMBLY'
    WHEN 'IT_EQUIPMENT' THEN 'OFFICES'
    WHEN 'FURNITURE' THEN 'OFFICES'
    WHEN 'VEHICLE' THEN 'WAREHOUSE'
    ELSE 'SITE_SERVICES'
END;
