CREATE TABLE hotels (
    id SERIAL PRIMARY KEY,
    name TEXT,
    address TEXT,
	district TEXT,
    description TEXT,
    hotel_link TEXT,
    rating_stars INT DEFAULT 0,
	facilities JSONB,
	highlights JSONB,
	reviews JSONB,
	image_urls JSONB,
	room_services JSONB,
	slug TEXT UNIQUE,
	coordinates GEOGRAPHY(POINT, 4326)
);

CREATE TABLE room_types (
    id SERIAL PRIMARY KEY,
    hotel_id INTEGER NOT NULL,
    name TEXT,
    number_of_guests INTEGER,
    price INTEGER,
	original_price INTEGER, 
	taxes_and_fees_under_price BOOLEAN,
    CONSTRAINT fk_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels (id)
        ON DELETE CASCADE
);

CREATE TABLE places (
	id SERIAL PRIMARY KEY,
	title text,
	rating FLOAT CHECK (rating >= 0 AND rating <= 5),
	address text,
	review INTEGER,
	slug TEXT UNIQUE,
	coordinates GEOGRAPHY(POINT, 4326),
	image_url text,
	description text,
	service JSONB
);

CREATE TABLE hotel_embeddings(
	id SERIAL PRIMARY KEY,
	hotel_id INTEGER NOT NULL,
	embedding VECTOR(768),
	CONSTRAINT fk_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotels (id)
        ON DELETE CASCADE
);

CREATE TABLE place_embeddings(
	id SERIAL PRIMARY KEY,
	place_id INTEGER NOT NULL,
	embedding VECTOR(768),
	CONSTRAINT fk_place
        FOREIGN KEY (place_id)
        REFERENCES places (id)
        ON DELETE CASCADE
)