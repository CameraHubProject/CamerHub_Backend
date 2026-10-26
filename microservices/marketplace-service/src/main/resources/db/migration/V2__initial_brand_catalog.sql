-- A new installation needs the same brand choices as the local imported catalog.
-- Seed only an empty catalog: existing admin edits and deletions remain authoritative.
INSERT INTO camerahub.brands(name)
SELECT name FROM (VALUES
 ('Canon'), ('Sony'), ('Nikon'), ('Fujifilm'), ('Panasonic'), ('Olympus'), ('OM System'), ('Pentax'),
 ('Ricoh'), ('Leica'), ('Kodak'), ('Casio'), ('Samsung'), ('Minolta'), ('Hasselblad'), ('Sigma'),
 ('GoPro'), ('DJI'), ('Insta360'), ('Polaroid'), ('Phase One'), ('Mamiya'), ('Yashica'), ('Konica'),
 ('Rollei'), ('Voigtländer'), ('Lomography'), ('Blackmagic Design'), ('RED'), ('ARRI'), ('Z CAM'),
 ('Kinefinity'), ('AKASO'), ('SJCAM'), ('Khác')
) AS initial_brands(name)
WHERE NOT EXISTS (SELECT 1 FROM camerahub.brands);
