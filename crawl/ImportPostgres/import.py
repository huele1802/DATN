# import json
# import numpy as np
# import psycopg2
# from psycopg2.extras import execute_batch

# # Đọc file JSON
# def read_json_file(file_path):
#     with open(file_path, 'r', encoding='utf-8') as f:
#         data = json.load(f)
#     return data

# # Đọc file embeddings
# def read_embeddings(file_path):
#     return np.load(file_path)

# # Kết nối đến PostgreSQL
# def get_db_connection(db_params):
#     return psycopg2.connect(**db_params)

# # Tạo bảng trong PostgreSQL
# def create_tables(conn):
#     with conn.cursor() as cur:
#         # Tạo bảng hotels
#         cur.execute("""
#             CREATE TABLE IF NOT EXISTS hotels (
#                 id SERIAL PRIMARY KEY,
#                 name TEXT NOT NULL,
#                 address TEXT,
#                 district TEXT,
#                 description TEXT,
#                 hotel_link TEXT,
#                 rating_stars INT DEFAULT 0,
#                 facilities JSONB,
#                 highlights JSONB,
#                 reviews JSONB,
# 	            image_urls JSONB,
#                 room_services JSONB,
#                 slug TEXT UNIQUE,
# 	            coordinates GEOGRAPHY(POINT, 4326)
#             );
#         """)

#         # Tạo bảng hotel_embeddings
#         cur.execute("""
#             CREATE TABLE IF NOT EXISTS hotel_embeddings (
#                 id SERIAL PRIMARY KEY,
#                 hotel_id INTEGER NOT NULL,
#                 embedding VECTOR(768),
#                 CONSTRAINT fk_hotel
#                     FOREIGN KEY (hotel_id)
#                     REFERENCES hotels (id)
#                     ON DELETE CASCADE
#             );
#         """)

#         # Tạo bảng rooms
#         cur.execute("""
#             CREATE TABLE IF NOT EXISTS room_types (
#                 id SERIAL PRIMARY KEY,
#                 hotel_id INTEGER NOT NULL,
#                 name TEXT NOT NULL,
#                 number_of_guests INTEGER,
#                 price INTEGER,
#                 original_price INTEGER, 
#                 taxes_and_fees_under_price BOOLEAN,
#                 CONSTRAINT fk_hotel
#                     FOREIGN KEY (hotel_id)
#                     REFERENCES hotels (id)
#                     ON DELETE CASCADE
#             );
#         """)

#         # Tạo bảng room_embeddings
#         cur.execute("""
#             CREATE TABLE IF NOT EXISTS room_embeddings (
#                 id SERIAL PRIMARY KEY,
#                 room_id INTEGER NOT NULL,
#                 embedding VECTOR(768),
#                 CONSTRAINT fk_room
#                     FOREIGN KEY (room_id)
#                     REFERENCES room_types (id)
#                     ON DELETE CASCADE
#             );
#         """)
        
#         # Tạo bảng rooms
#         cur.execute("""
#             CREATE TABLE IF NOT EXISTS places (
#                 id SERIAL PRIMARY KEY,
#                 title text,
#                 rating FLOAT CHECK (rating >= 0 AND rating <= 5),
#                 address text,
#                 review INTEGER,
#                 slug TEXT UNIQUE,
#                 coordinates GEOGRAPHY(POINT, 4326),
#                 image_url text,
#                 description text,
#                 service JSONB
#             );
#         """)

#         # Tạo bảng room_embeddings
#         cur.execute("""
#             CREATE TABLE IF NOT EXISTS place_embeddings(
#                 id SERIAL PRIMARY KEY,
#                 place_id INTEGER NOT NULL,
#                 embedding VECTOR(768),
#                 CONSTRAINT fk_place
#                     FOREIGN KEY (place_id)
#                     REFERENCES places (id)
#                     ON DELETE CASCADE
#             );
#         """)

#         conn.commit()
#         print("Đã tạo các bảng: hotels, hotel_embeddings, rooms, room_embeddings, places, place_embeddings.")

# # Chèn dữ liệu khách sạn
# def insert_hotels(conn, hotels_data):
#     with conn.cursor() as cur:
#         hotel_insert_query = """
#             INSERT INTO hotels (hotel_link, name, description, facilities, highlights, reviews, rating_stars, address, district, room_services, slug, image_urls, coordinates)
#             VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326))
#             RETURNING id;
#         """
#         hotel_data = []
#         for hotel in hotels_data:
#             hotel_data.append((
#                 hotel.get('hotel_link'),
#                 hotel.get('name'),
#                 hotel.get('description'),
#                 json.dumps(hotel.get('facilities', [])),
#                 json.dumps(hotel.get("highlights", {})),
#                 json.dumps(hotel.get('reviews', {})),
#                 hotel.get('rating_stars', 0),
#                 hotel.get('address'), 
#                 hotel.get('district'),
#                 json.dumps(hotel.get('room_services', {})),
#                 hotel.get('slug'),
#                 json.dumps(hotel.get('image_urls', [])),
#                 hotel.get('longitude'),  # longitude trước
#                 hotel.get('latitude')   # latitude sau
#             ))

#         execute_batch(cur, hotel_insert_query, hotel_data)
#         cur.execute("SELECT id, name FROM hotels;")
#         hotel_ids = {row[1]: row[0] for row in cur.fetchall()}
#         conn.commit()
#         print(f"Đã chèn {len(hotel_data)} khách sạn.")
#         return hotel_ids

# # Chèn embeddings khách sạn
# def insert_hotel_embeddings(conn, hotel_ids, hotel_embeddings):
#     with conn.cursor() as cur:
#         embedding_insert_query = """
#             INSERT INTO hotel_embeddings (hotel_id, embedding)
#             VALUES (%s, %s);
#         """
#         embedding_data = []
#         for hotel_name, hotel_id in hotel_ids.items():
#             # Giả sử embeddings được sắp xếp theo thứ tự của hotels_data
#             idx = list(hotel_ids.keys()).index(hotel_name)
#             embedding = hotel_embeddings[idx]
#             embedding_str = f"[{','.join(map(str, embedding))}]"
#             embedding_data.append((hotel_id, embedding_str))

#         execute_batch(cur, embedding_insert_query, embedding_data)
#         conn.commit()
#         print(f"Đã chèn {len(embedding_data)} embeddings khách sạn.")

# # Chèn dữ liệu phòng
# def insert_rooms(conn, hotels_data, hotel_ids):
#     with conn.cursor() as cur:
#         room_insert_query = """
#             INSERT INTO room_types (hotel_id, name, number_of_guests, price, original_price, taxes_and_fees_under_price)
#             VALUES (%s, %s, %s, %s, %s, %s)
#             RETURNING id;
#         """
#         room_data = []
#         room_id_mapping = []
#         for hotel in hotels_data:
#             if hotel.get("room_types"):
#                 hotel_id = hotel_ids.get(hotel["name"])
#                 for room in hotel["room_types"]:
#                     room_data.append((
#                         hotel_id,
#                         room["name"],
#                         room["number_of_guests"],
#                         room["price"],
#                         room["original_price"],
#                         room['taxes_and_fees_under_price']
#                     ))

#         execute_batch(cur, room_insert_query, room_data)
#         cur.execute("SELECT id, hotel_id, name FROM room_types;")
#         room_ids = [(row[0], row[1], row[2]) for row in cur.fetchall()]
#         conn.commit()
#         print(f"Đã chèn {len(room_data)} phòng.")
#         return room_ids

# # Chèn embeddings phòng
# def insert_room_embeddings(conn, hotels_data, room_ids, room_embeddings):
#     with conn.cursor() as cur:
#         embedding_insert_query = """
#             INSERT INTO room_embeddings (room_id, embedding)
#             VALUES (%s, %s);
#         """
#         embedding_data = []
#         embedding_idx = 0
#         for hotel in hotels_data:
#             if hotel.get("room_types"):
#                 hotel_id = hotel_ids.get(hotel["name"])
#                 for room in hotel["room_types"]:
#                     # Tìm room_id tương ứng
#                     for room_id, h_id, room_name in room_ids:
#                         if room_name == room["name"] and hotel_id == h_id:
#                             embedding = room_embeddings[embedding_idx]
#                             embedding_str = f"[{','.join(map(str, embedding))}]"
#                             embedding_data.append((room_id, embedding_str))
#                             embedding_idx += 1
#                             break

#         execute_batch(cur, embedding_insert_query, embedding_data)
#         conn.commit()
#         print(f"Đã chèn {len(embedding_data)} embeddings phòng.")

# # Chèn dữ liệu khách sạn
# def insert_places(conn, places_data):
#     with conn.cursor() as cur:
#         place_insert_query = """
#             INSERT INTO places (title, rating, address, coordinates, image_url, description, service, review, slug)
#             VALUES (%s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s, %s, %s, %s, %s)
#             RETURNING place_id;
#         """
#         place_data = []
#         for place in places_data:
#             place_data.append((
#                 place.get('title'),
#                 place.get('rating'),
#                 place.get('address'),
#                 place.get('longitude'),
#                 place.get('latitude'),
#                 place.get('img'),
#                 place.get('description'),
#                 json.dumps(place.get('service', [])),
#                 place.get('review'),
#                 place.get('slug'),
#             ))

#         execute_batch(cur, place_insert_query, place_data)
#         cur.execute("SELECT id, title FROM places;")
#         place_ids = {row[1]: row[0] for row in cur.fetchall()}
#         conn.commit()
#         print(f"Đã chèn {len(place_data)} địa điểm.")
#         return place_ids

# # Chèn embeddings khách sạn
# def insert_place_embeddings(conn, place_ids, place_embeddings):
#     with conn.cursor() as cur:
#         embedding_insert_query = """
#             INSERT INTO place_embeddings (place_id, embedding)
#             VALUES (%s, %s);
#         """
#         embedding_data = []
#         for place_id, place_title in place_ids.items():
#             idx = list(place_ids.keys()).index(place_title)
#             embedding = place_embeddings[idx]
#             embedding_str = f"[{','.join(map(str, embedding))}]"
#             embedding_data.append((place_id, embedding_str))

#         execute_batch(cur, embedding_insert_query, embedding_data)
#         conn.commit()
#         print(f"Đã chèn {len(embedding_data)} embeddings địa điểm.")

# # Hàm chính
# def main(hotel_json_file, place_json_file, hotel_embeddings_file, room_embeddings_file, place_embeddings_file, db_params):
#     # Đọc dữ liệu
#     hotels_data = read_json_file(hotel_json_file)
#     places_data = read_json_file(place_json_file)
#     hotel_embeddings = read_embeddings(hotel_embeddings_file)
#     room_embeddings = read_embeddings(room_embeddings_file)
#     place_embeddings = read_embeddings(place_embeddings_file)

#     # Kết nối database
#     conn = get_db_connection(db_params)

#     try:
#         # Tạo bảng
#         create_tables(conn)

#         # Chèn dữ liệu khách sạn
#         hotel_ids = insert_hotels(conn, hotels_data)
#         # Chèn embeddings khách sạn
#         insert_hotel_embeddings(conn, hotel_ids, hotel_embeddings)

#         # Chèn dữ liệu phòng
#         room_ids = insert_rooms(conn, hotels_data, hotel_ids)
#         # Chèn embeddings phòng
#         insert_room_embeddings(conn, hotels_data, room_ids, room_embeddings)

#         # Chèn dữ liệu địa điểm
#         place_ids = insert_places(conn, places_data)
#         # Chèn embeddings địa điểm
#         insert_place_embeddings(conn, place_ids, place_embeddings)

#     finally:
#         conn.close()
#         print("Đã đóng kết nối database.")

# # Ví dụ sử dụng
# if __name__ == "__main__":
#     hotel_json_file = 'output/cleaned/hotels_cleaned.json'
#     place_json_file = 'output/cleaned/places_cleaned.json'
#     hotel_embeddings_file = 'output/embedding/hotel_embeddings.npy'
#     room_embeddings_file = 'output/embedding/room_embeddings.npy'
#     place_embeddings_file = 'output/embedding/place_embeddings.npy'

#     # Thay đổi thông tin kết nối database của bạn
#     db_params = {
#         "dbname": "hotelproposal",
#         "user": "huele",
#         "password": "eOhfnkPnFeKhPeuGVtFZMsFIIIbc4joN",
#         "host": "dpg-cvt561idbo4c73cicigg-a.oregon-postgres.render.com",
#         "port": "5432"
#     }

#     main(hotel_json_file, place_json_file, hotel_embeddings_file, room_embeddings_file, place_embeddings_file, db_params)

import json
import numpy as np
import psycopg2
from psycopg2.extras import execute_batch

# Đọc file JSON
def read_json_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return data
    except Exception as e:
        print(f"Lỗi khi đọc file JSON {file_path}: {e}")
        raise

# Đọc file embeddings
def read_embeddings(file_path):
    try:
        embeddings = np.load(file_path)
        # Kiểm tra kích thước embeddings
        if embeddings.shape[1] != 768:
            raise ValueError(f"Embeddings trong {file_path} phải có 768 chiều, nhưng có {embeddings.shape[1]} chiều.")
        return embeddings
    except Exception as e:
        print(f"Lỗi khi đọc file embeddings {file_path}: {e}")
        raise

# Kết nối đến PostgreSQL
def get_db_connection(db_params):
    try:
        return psycopg2.connect(**db_params)
    except Exception as e:
        print(f"Lỗi khi kết nối database: {e}")
        raise

# Tạo bảng trong PostgreSQL
def create_tables(conn):
    try:
        with conn.cursor() as cur:
            # Tạo bảng hotels
            cur.execute("""
                CREATE TABLE IF NOT EXISTS hotels (
                    id SERIAL PRIMARY KEY,
                    name TEXT NOT NULL,
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
            """)

            # Tạo bảng hotel_embeddings
            cur.execute("""
                CREATE TABLE IF NOT EXISTS hotel_embeddings (
                    id SERIAL PRIMARY KEY,
                    hotel_id INTEGER NOT NULL,
                    embedding VECTOR(768),
                    CONSTRAINT fk_hotel
                        FOREIGN KEY (hotel_id)
                        REFERENCES hotels (id)
                        ON DELETE CASCADE
                );
            """)

            # Tạo bảng room_types
            cur.execute("""
                CREATE TABLE IF NOT EXISTS room_types (
                    id SERIAL PRIMARY KEY,
                    hotel_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    number_of_guests INTEGER,
                    price INTEGER,
                    original_price INTEGER, 
                    taxes_and_fees_under_price BOOLEAN,
                    CONSTRAINT fk_hotel
                        FOREIGN KEY (hotel_id)
                        REFERENCES hotels (id)
                        ON DELETE CASCADE
                );
            """)

            # Tạo bảng room_embeddings
            cur.execute("""
                CREATE TABLE IF NOT EXISTS room_embeddings (
                    id SERIAL PRIMARY KEY,
                    room_id INTEGER NOT NULL,
                    embedding VECTOR(768),
                    CONSTRAINT fk_room
                        FOREIGN KEY (room_id)
                        REFERENCES room_types (id)
                        ON DELETE CASCADE
                );
            """)

            # Tạo bảng places
            cur.execute("""
                CREATE TABLE IF NOT EXISTS places (
                    id SERIAL PRIMARY KEY,
                    title TEXT,
                    rating FLOAT CHECK (rating >= 0 AND rating <= 5),
                    address TEXT,
                    review INTEGER,
                    slug TEXT UNIQUE,
                    coordinates GEOGRAPHY(POINT, 4326),
                    image_url TEXT,
                    description TEXT,
                    service JSONB
                );
            """)

            # Tạo bảng place_embeddings
            cur.execute("""
                CREATE TABLE IF NOT EXISTS place_embeddings (
                    id SERIAL PRIMARY KEY,
                    place_id INTEGER NOT NULL,
                    embedding VECTOR(768),
                    CONSTRAINT fk_place
                        FOREIGN KEY (place_id)
                        REFERENCES places (id)
                        ON DELETE CASCADE
                );
            """)

            conn.commit()
            print("Đã tạo các bảng: hotels, hotel_embeddings, room_types, room_embeddings, places, place_embeddings.")
    except Exception as e:
        print(f"Lỗi khi tạo bảng: {e}")
        conn.rollback()
        raise

# Chèn dữ liệu khách sạn
def insert_hotels(conn, hotels_data):
    try:
        with conn.cursor() as cur:
            hotel_insert_query = """
                INSERT INTO hotels (hotel_link, name, description, facilities, highlights, reviews, rating_stars, address, district, room_services, slug, image_urls, coordinates)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326))
                ON CONFLICT (slug) DO NOTHING
                RETURNING id, slug;
            """
            hotel_data = []
            for hotel in hotels_data:
                # Kiểm tra các trường bắt buộc
                if not hotel.get('name'):
                    print(f"Bỏ qua khách sạn thiếu 'name': {hotel}")
                    continue
                longitude = hotel.get('longitude') if hotel.get('longitude') is not None else None
                latitude = hotel.get('latitude') if hotel.get('latitude') is not None else None
                hotel_data.append((
                    hotel.get('hotel_link'),
                    hotel.get('name'),
                    hotel.get('description'),
                    json.dumps(hotel.get('facilities', [])),
                    json.dumps(hotel.get("highlights", {})),
                    json.dumps(hotel.get('reviews', {})),
                    hotel.get('rating_stars', 0),
                    hotel.get('address'),
                    hotel.get('district'),
                    json.dumps(hotel.get('room_services', {})),
                    hotel.get('slug'),
                    json.dumps(hotel.get('image_urls', [])),
                    longitude,  # longitude trước
                    latitude   # latitude sau
                ))

            execute_batch(cur, hotel_insert_query, hotel_data)
            # Lấy danh sách khách sạn đã chèn
            cur.execute("SELECT id, slug FROM hotels WHERE slug = ANY(%s);", ([h[10] for h in hotel_data],))
            hotel_ids = {row[1]: row[0] for row in cur.fetchall()}
            conn.commit()
            print(f"Đã chèn {len(hotel_ids)} khách sạn.")
            return hotel_ids
    except Exception as e:
        print(f"Lỗi khi chèn khách sạn: {e}")
        conn.rollback()
        raise

# Chèn embeddings khách sạn
def insert_hotel_embeddings(conn, hotels_data, hotel_ids, hotel_embeddings):
    try:
        with conn.cursor() as cur:
            embedding_insert_query = """
                INSERT INTO hotel_embeddings (hotel_id, embedding)
                VALUES (%s, %s);
            """
            embedding_data = []

            for idx, hotel in enumerate(hotels_data):
                slug = hotel['slug']
                hotel_id = hotel_ids.get(slug)
                if hotel_id is None:
                    print(f"Không tìm thấy ID cho slug: {slug}")
                    continue
                if idx >= len(hotel_embeddings):
                    print(f"Thiếu embedding cho khách sạn tại index {idx}")
                    continue

                embedding = hotel_embeddings[idx]
                embedding_str = f"[{','.join(map(str, embedding))}]"
                embedding_data.append((hotel_id, embedding_str))

            execute_batch(cur, embedding_insert_query, embedding_data)
            conn.commit()
            print(f"Đã chèn {len(embedding_data)} embeddings khách sạn.")
    except Exception as e:
        print(f"Lỗi khi chèn embeddings khách sạn: {e}")
        conn.rollback()
        raise

# Chèn dữ liệu phòng
def insert_rooms(conn, hotels_data, hotel_ids):
    try:
        with conn.cursor() as cur:
            room_insert_query = """
                INSERT INTO room_types (hotel_id, name, number_of_guests, price, original_price, taxes_and_fees_under_price)
                VALUES (%s, %s, %s, %s, %s, %s)
                RETURNING id, hotel_id, name, number_of_guests;
            """
            room_data = []
            for hotel in hotels_data:
                if hotel.get("room_types") and hotel.get("slug") in hotel_ids:
                    hotel_id = hotel_ids.get(hotel["slug"])
                    for room in hotel["room_types"]:
                        if not room.get("name"):
                            print(f"Bỏ qua phòng thiếu 'name' trong khách sạn {hotel['name']}")
                            continue
                        room_data.append((
                            hotel_id,
                            room["name"],
                            room.get("number_of_guests"),
                            room.get("price"),
                            room.get("original_price"),
                            room.get('taxes_and_fees_under_price')
                        ))

            execute_batch(cur, room_insert_query, room_data)
            # Lấy danh sách phòng đã chèn
            cur.execute("SELECT id, hotel_id, name, number_of_guests, price FROM room_types WHERE hotel_id = ANY(%s);",
                        ([hotel_ids[hotel["slug"]] for hotel in hotels_data if hotel["slug"] in hotel_ids],))
            room_ids = {(row[1], row[2], row[3], row[4]): row[0] for row in cur.fetchall()}
            conn.commit()
            print(f"Đã chèn {len(room_data)} phòng.")
            return room_ids
    except Exception as e:
        print(f"Lỗi khi chèn phòng: {e}")
        conn.rollback()
        raise

# Chèn embeddings phòng
def insert_room_embeddings(conn, hotels_data, hotel_ids, room_ids, room_embeddings):
    try:
        with conn.cursor() as cur:
            embedding_insert_query = """
                INSERT INTO room_embeddings (room_id, embedding)
                VALUES (%s, %s);
            """
            embedding_data = []
            embedding_idx = 0
            for hotel in hotels_data:
                if hotel.get("room_types") and hotel.get("slug") in hotel_ids:
                    hotel_id = hotel_ids.get(hotel["slug"])
                    for room in hotel["room_types"]:
                        if embedding_idx >= len(room_embeddings):
                            print(f"Thiếu embedding cho phòng {room['name']} trong khách sạn {hotel['name']}")
                            break
                        key = (hotel_id, room["name"], room.get("number_of_guests"), room.get("price"))
                        if key in room_ids:
                            embedding = room_embeddings[embedding_idx]
                            embedding_str = f"[{','.join(map(str, embedding))}]"
                            embedding_data.append((room_ids[key], embedding_str))
                            embedding_idx += 1
                        else:
                            print(f"Không tìm thấy room_id cho phòng {room['name']} (guests: {room.get('number_of_guests')}) trong khách sạn {hotel['name']}")

            execute_batch(cur, embedding_insert_query, embedding_data)
            conn.commit()
            print(f"Đã chèn {len(embedding_data)} embeddings phòng.")
    except Exception as e:
        print(f"Lỗi khi chèn embeddings phòng: {e}")
        conn.rollback()
        raise

# Chèn dữ liệu địa điểm
def insert_places(conn, places_data):
    try:
        with conn.cursor() as cur:
            place_insert_query = """
                INSERT INTO places (title, rating, address, coordinates, image_url, description, service, review, slug)
                VALUES (%s, %s, %s, ST_SetSRID(ST_MakePoint(%s, %s), 4326), %s, %s, %s, %s, %s)
                ON CONFLICT (slug) DO NOTHING
                RETURNING id, slug;
            """
            place_data = []
            for place in places_data:
                if not place.get('title'):
                    print(f"Bỏ qua địa điểm thiếu 'title': {place}")
                    continue
                longitude = place.get('longitude') if place.get('longitude') is not None else None
                latitude = place.get('latitude') if place.get('latitude') is not None else None
                place_data.append((
                    place.get('title'),
                    place.get('rating'),
                    place.get('address'),
                    longitude,
                    latitude,
                    place.get('img'),
                    place.get('description'),
                    json.dumps(place.get('service', [])),
                    place.get('review'),
                    place.get('slug'),
                ))

            execute_batch(cur, place_insert_query, place_data)
            cur.execute("SELECT id, slug FROM places WHERE slug = ANY(%s);", ([p[9] for p in place_data],))
            place_ids = {row[1]: row[0] for row in cur.fetchall()}
            conn.commit()
            print(f"Đã chèn {len(place_ids)} địa điểm.")
            return place_ids
    except Exception as e:
        print(f"Lỗi khi chèn địa điểm: {e}")
        conn.rollback()
        raise

# Chèn embeddings địa điểm
def insert_place_embeddings(conn, places_data, place_ids, place_embeddings):
    try:
        with conn.cursor() as cur:
            embedding_insert_query = """
                INSERT INTO place_embeddings (place_id, embedding)
                VALUES (%s, %s);
            """
            embedding_data = []
            for idx, place in enumerate(places_data):
                slug = place["slug"]
                place_id = place_ids.get(slug)
                if place_id is None:
                    print(f"Không tìm thấy ID cho slug: {slug}")
                    continue
                if idx >= len(place_embeddings):
                    print(f"Thiếu embedding cho địa điểm {slug}")
                    continue
                embedding = place_embeddings[idx]
                embedding_str = f"[{','.join(map(str, embedding))}]"
                embedding_data.append((place_id, embedding_str))

            execute_batch(cur, embedding_insert_query, embedding_data)
            conn.commit()
            print(f"Đã chèn {len(embedding_data)} embeddings địa điểm.")
    except Exception as e:
        print(f"Lỗi khi chèn embeddings địa điểm: {e}")
        conn.rollback()
        raise

# Hàm chính
def main(hotel_json_file, place_json_file, hotel_embeddings_file, room_embeddings_file, place_embeddings_file, db_params):
    try:
        # Đọc dữ liệu
        hotels_data = read_json_file(hotel_json_file)
        places_data = read_json_file(place_json_file)
        hotel_embeddings = read_embeddings(hotel_embeddings_file)
        room_embeddings = read_embeddings(room_embeddings_file)
        place_embeddings = read_embeddings(place_embeddings_file)

        # Kiểm tra số lượng embeddings
        if len(hotel_embeddings) < len(hotels_data):
            raise ValueError(f"Số lượng embeddings khách sạn ({len(hotel_embeddings)}) nhỏ hơn số khách sạn ({len(hotels_data)})")
        if len(place_embeddings) < len(places_data):
            raise ValueError(f"Số lượng embeddings địa điểm ({len(place_embeddings)}) nhỏ hơn số địa điểm ({len(places_data)})")
        expected_room_count = sum(len(hotel.get('room_types', [])) for hotel in hotels_data)
        if len(room_embeddings) < expected_room_count:
            raise ValueError(f"Số lượng embeddings phòng ({len(room_embeddings)}) nhỏ hơn số phòng ({expected_room_count})")

        # Kết nối database
        conn = get_db_connection(db_params)

        try:
            # Tạo bảng
            create_tables(conn)

            # Chèn dữ liệu khách sạn
            hotel_ids = insert_hotels(conn, hotels_data)
            # Chèn embeddings khách sạn
            insert_hotel_embeddings(conn, hotels_data, hotel_ids, hotel_embeddings)

            # Chèn dữ liệu phòng
            room_ids = insert_rooms(conn, hotels_data, hotel_ids)
            # Chèn embeddings phòng
            insert_room_embeddings(conn, hotels_data, hotel_ids, room_ids, room_embeddings)

            # Chèn dữ liệu địa điểm
            place_ids = insert_places(conn, places_data)
            # Chèn embeddings địa điểm
            insert_place_embeddings(conn, places_data, place_ids, place_embeddings)

        finally:
            conn.close()
            print("Đã đóng kết nối database.")
    except Exception as e:
        print(f"Lỗi trong quá trình thực thi: {e}")
        raise

# Ví dụ sử dụng
if __name__ == "__main__":
    hotel_json_file = 'output/cleaned/hotels_cleaned.json'
    place_json_file = 'output/cleaned/places_img_cleaned.json'
    hotel_embeddings_file = 'output/embedding/hotel_embeddings.npy'
    room_embeddings_file = 'output/embedding/room_embeddings.npy'
    place_embeddings_file = 'output/embedding/place_embeddings.npy'

    db_params = {
        "dbname": "hotelproposal_nkfi",
        "user": "huele",
        "password": "ru9EHspfHTc0JMmWx1W37dpF67pdM5cM",
        "host": "dpg-d0h18j49c44c7397dt2g-a.oregon-postgres.render.com",
        "port": "5432"
    }

    main(hotel_json_file, place_json_file, hotel_embeddings_file, room_embeddings_file, place_embeddings_file, db_params)