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

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),
    full_name VARCHAR(255),
    phone_number VARCHAR(20),
    date_of_birth DATE,
    address TEXT,
    avatar_url TEXT,
    role VARCHAR(20) NOT NULL CHECK (role IN ('USER', 'ADMIN')),
    google_id VARCHAR(255) UNIQUE
);

CREATE TABLE search_history (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL,
    query TEXT NOT NULL,
    search_type VARCHAR(50),
    search_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);