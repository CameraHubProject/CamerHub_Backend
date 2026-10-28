-- Catalog data only: preserve existing IDs, models and products.
INSERT INTO camerahub.brands(name) VALUES
 ('Canon'),('Sony'),('Nikon'),('Fujifilm'),('Panasonic'),('Olympus'),('OM System'),('Pentax'),
 ('Ricoh'),('Leica'),('Kodak'),('Casio'),('Samsung'),('Minolta'),('Hasselblad'),('Sigma'),
 ('GoPro'),('DJI'),('Insta360'),('Polaroid'),('Phase One'),('Mamiya'),('Yashica'),('Konica'),
 ('Rollei'),('Voigtländer'),('Lomography'),('Blackmagic Design'),('RED'),('ARRI'),('Z CAM'),
 ('Kinefinity'),('AKASO'),('SJCAM')
ON CONFLICT(name) DO NOTHING;
