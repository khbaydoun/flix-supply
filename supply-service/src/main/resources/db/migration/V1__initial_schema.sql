-- Supply-side domain: partners operate rides on lines, under a brand.
-- Deliberately models the "brand moves from line level to ride level" problem.

CREATE TABLE brand (
                       id          BIGSERIAL PRIMARY KEY,
                       code        TEXT NOT NULL UNIQUE,          -- FLIXBUS, FLIXTRAIN, KAMIL_KOC
                       name        TEXT NOT NULL
);

CREATE TABLE mobility_partner (
                                  id          BIGSERIAL PRIMARY KEY,
                                  name        TEXT NOT NULL,
                                  country     TEXT NOT NULL,
                                  active      BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE line (
                      id          BIGSERIAL PRIMARY KEY,
                      code        TEXT NOT NULL UNIQUE,          -- N120
                      origin      TEXT NOT NULL,
                      destination TEXT NOT NULL,
                      brand_id    BIGINT REFERENCES brand(id)    -- legacy: brand at LINE level
);

CREATE TABLE ride (
                      id            BIGSERIAL PRIMARY KEY,
                      line_id       BIGINT NOT NULL REFERENCES line(id),
                      partner_id    BIGINT NOT NULL REFERENCES mobility_partner(id),
                      brand_id      BIGINT REFERENCES brand(id),  -- NEW: brand at RIDE level
                      departure_at  TIMESTAMPTZ NOT NULL,
                      arrival_at    TIMESTAMPTZ NOT NULL,
                      capacity      INT NOT NULL,
                      status        TEXT NOT NULL DEFAULT 'SCHEDULED'
);
CREATE INDEX idx_ride_line_departure ON ride (line_id, departure_at);
CREATE INDEX idx_ride_partner ON ride (partner_id);

CREATE TABLE ride_stop (
                           id            BIGSERIAL PRIMARY KEY,
                           ride_id       BIGINT NOT NULL REFERENCES ride(id) ON DELETE CASCADE,
                           sequence_no   INT NOT NULL,
                           station_name  TEXT NOT NULL,
                           scheduled_at  TIMESTAMPTZ NOT NULL,
                           UNIQUE (ride_id, sequence_no)
);

INSERT INTO brand (code, name) VALUES
                                   ('FLIXBUS','FlixBus'), ('FLIXTRAIN','FlixTrain'), ('KAMIL_KOC','Kâmil Koç');

INSERT INTO mobility_partner (name, country) VALUES
                                                 ('Rheinland Reisen GmbH','DE'), ('Anadolu Turizm','TR'), ('Benelux Coaches BV','NL');

INSERT INTO line (code, origin, destination, brand_id) VALUES
                                                           ('N120','Aachen','Köln',1), ('N33','Köln','Berlin',1), ('T15','Berlin','Hamburg',2);

INSERT INTO ride (line_id, partner_id, brand_id, departure_at, arrival_at, capacity) VALUES
                                                                                         (1,1,1, now() + interval '2 h',  now() + interval '3 h',  52),
                                                                                         (1,3,3, now() + interval '6 h',  now() + interval '7 h',  49),   -- same line, different brand
                                                                                         (2,1,1, now() + interval '1 day', now() + interval '1 day 7 h', 52),
                                                                                         (3,2,2, now() + interval '3 h',  now() + interval '5 h', 300);

INSERT INTO ride_stop (ride_id, sequence_no, station_name, scheduled_at) VALUES
                                                                             (1,1,'Aachen Hbf', now() + interval '2 h'),
                                                                             (1,2,'Düren',      now() + interval '2 h 30 min'),
                                                                             (1,3,'Köln Hbf',   now() + interval '3 h'),
                                                                             (2,1,'Aachen Hbf', now() + interval '6 h'),
                                                                             (2,2,'Köln Hbf',   now() + interval '7 h');