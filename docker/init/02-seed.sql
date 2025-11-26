-- =============================================
-- TABLAS DE REFERENCIA
-- =============================================

-- Formas farmacéuticas (15 registros)
INSERT INTO pharmaceutical_form (name)
VALUES ('Comprimido'),
       ('Cápsula'),
       ('Jarabe'),
       ('Suspensión'),
       ('Inyectable'),
       ('Crema'),
       ('Ungüento'),
       ('Gotas'),
       ('Parche'),
       ('Supositorio'),
       ('Spray'),
       ('Gel'),
       ('Polvo'),
       ('Solución'),
       ('Tableta');

-- Códigos CIE-10 comunes (50 registros)
INSERT INTO cie10 (code, name)
VALUES ('J00', 'Rinofaringitis aguda'),
       ('J06', 'Infección vías resp sup'),
       ('J18', 'Neumonía'),
       ('J20', 'Bronquitis aguda'),
       ('J45', 'Asma'),
       ('K29', 'Gastritis'),
       ('K30', 'Dispepsia'),
       ('K21', 'Reflujo gastroesofágico'),
       ('I10', 'Hipertensión'),
       ('I25', 'Cardiopatía crónica'),
       ('E11', 'Diabetes tipo 2'),
       ('E78', 'Lipoproteínas'),
       ('M54', 'Dorsalgia'),
       ('M79', 'Tejidos blandos'),
       ('M25', 'Articulares'),
       ('R50', 'Fiebre'),
       ('R51', 'Cefalea'),
       ('R10', 'Dolor abdominal'),
       ('A09', 'Gastroenteritis'),
       ('H10', 'Conjuntivitis'),
       ('N39', 'Trast urinario'),
       ('L20', 'Dermatitis atópica'),
       ('L30', 'Dermatitis'),
       ('F32', 'Depresión'),
       ('F41', 'Ansiedad'),
       ('G43', 'Migraña'),
       ('N95', 'Menopausia'),
       ('Z00', 'Examen general'),
       ('Z23', 'Inmunización'),
       ('B34', 'Infección viral'),
       ('J02', 'Faringitis'),
       ('J03', 'Amigdalitis'),
       ('K52', 'Gastroenteritis no inf'),
       ('M17', 'Gonartrosis'),
       ('M19', 'Artrosis'),
       ('I11', 'Cardiopatía hipertensiva'),
       ('E66', 'Obesidad'),
       ('E04', 'Bocio no tóxico'),
       ('D50', 'Anemia'),
       ('H52', 'Refracción'),
       ('J30', 'Rinitis alérgica'),
       ('L50', 'Urticaria'),
       ('N30', 'Cistitis'),
       ('R05', 'Tos'),
       ('R06', 'Respiración anormal'),
       ('T78', 'Efectos adversos'),
       ('Z01', 'Exámenes especiales'),
       ('Z30', 'Anticoncepción'),
       ('Z71', 'Consejería'),
       ('Z76', 'Uso de servicios salud');

-- Medicamentos (200 registros)
INSERT INTO medication (name, pharmaceutical_form_id, dosage)
SELECT 'Medicamento_' || gs.n,
       (gs.n % 15) + 1,
       CASE (gs.n % 10)
           WHEN 0 THEN '10mg'
           WHEN 1 THEN '25mg'
           WHEN 2 THEN '50mg'
           WHEN 3 THEN '100mg'
           WHEN 4 THEN '250mg'
           WHEN 5 THEN '500mg'
           WHEN 6 THEN '5mg/ml'
           WHEN 7 THEN '1%'
           WHEN 8 THEN '2%'
           ELSE '20mg'
           END
FROM generate_series(1, 200) AS gs(n);

-- =============================================
-- PACIENTES (20,000)
-- =============================================
INSERT INTO patient (rut, name, birth_date, sex, residential_sector, fonasa_tier)
SELECT LPAD((10000000 + gs.n)::TEXT, 8, '0') || '-' || (gs.n % 10)::TEXT,
       'Paciente_' || gs.n,
       DATE '1940-01-01' + (random() * 30000)::INTEGER,
       CASE WHEN random() < 0.52 THEN 'F' ELSE 'M' END,
       CASE (gs.n % 9)
           WHEN 0 THEN 'Sector Norte'
           WHEN 1 THEN 'Sector Sur'
           WHEN 2 THEN 'Sector Este'
           WHEN 3 THEN 'Sector Oeste'
           WHEN 4 THEN 'Sector Noreste'
           WHEN 5 THEN 'Sector Noroeste'
           WHEN 6 THEN 'Sector Sureste'
           WHEN 7 THEN 'Sector Suroeste'
           ELSE 'Sector Centro'
           END,
       CASE (gs.n % 4)
           WHEN 0 THEN 'A'
           WHEN 1 THEN 'B'
           WHEN 2 THEN 'C'
           ELSE 'D'
           END
FROM generate_series(1, 20000) gs(n);

-- =============================================
-- PROFESIONALES (200)
-- =============================================
INSERT INTO healthcare_professional (rut, name, specialty)
SELECT LPAD((20000000 + gs.n)::TEXT, 8, '0') || '-' || ((gs.n % 10))::TEXT,
       'Profesional_' || gs.n,
       CASE (gs.n % 8)
           WHEN 0 THEN 'Medicina General'
           WHEN 1 THEN 'Medicina Interna'
           WHEN 2 THEN 'Pediatría'
           WHEN 3 THEN 'Ginecología'
           WHEN 4 THEN 'Cardiología'
           WHEN 5 THEN 'Traumatología'
           WHEN 6 THEN 'Dermatología'
           ELSE 'Medicina Familiar'
           END
FROM generate_series(1, 200) gs(n);

-- =============================================
-- CONSULTAS (40,000)
-- =============================================
INSERT INTO consultation (date, type, patient_id, professional_id)
SELECT DATE '2023-01-01' + (random() * 700)::INTEGER,
       CASE (gs.n % 4)
           WHEN 0 THEN 'MORBILIDAD'
           WHEN 1 THEN 'CONTROL'
           WHEN 2 THEN 'URGENCIA'
           ELSE 'OTRAS'
           END,
       1 + (random() * 19999)::INTEGER,
       1 + (random() * 199)::INTEGER
FROM generate_series(1, 40000) gs(n);

-- =============================================
-- PRESCRIPCIONES (25,000 EXACTAS)
-- =============================================
INSERT INTO prescription (date, consultation_id)
SELECT date, id
FROM consultation
ORDER BY random()
LIMIT 25000;

-- =============================================
-- DIAGNÓSTICOS (12,000 EXACTOS)
-- =============================================
INSERT INTO diagnosis (consultation_id, cie10_code, description)
SELECT c.id,
       (SELECT code FROM cie10 ORDER BY random() LIMIT 1),
       'Descripción diagnóstico ' || gs.n
FROM consultation c
         JOIN generate_series(1, 12000) gs(n)
              ON true
ORDER BY random()
LIMIT 12000;

-- =============================================
-- MEDICACIONES PRESCRITAS (2,800 EXACTAS)
-- =============================================
INSERT INTO prescription_medication (prescription_id, medication_id, quantity, instructions)
SELECT p.id,
       1 + FLOOR(random() * 200)::INTEGER,
       (ARRAY [7,10,14,15,20,28,30,60,90])[1 + FLOOR(random() * 9)::INTEGER],
       CASE (gs.n % 6)
           WHEN 0 THEN 'Tomar 1 cada 8 horas'
           WHEN 1 THEN 'Tomar 1 cada 12 horas'
           WHEN 2 THEN 'Tomar 1 al día'
           WHEN 3 THEN 'Tomar 1 cada 6 horas'
           WHEN 4 THEN 'Aplicar 2 veces al día'
           ELSE 'Tomar 1 antes de las comidas'
           END
FROM prescription p
         JOIN generate_series(1, 2800) gs(n)
              ON true
ORDER BY random()
LIMIT 2800;

