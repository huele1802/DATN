import psycopg2
import json

def update_place_image_url_by_slug(slug, new_image_url):
    try:
        conn = psycopg2.connect(
            dbname = "hotelproposal",
            user = "huele",
            password = "eOhfnkPnFeKhPeuGVtFZMsFIIIbc4joN",
            host = "dpg-cvt561idbo4c73cicigg-a.oregon-postgres.render.com",
            port = "5432"
        )
        cursor = conn.cursor()

        update_query = """
            UPDATE places
            SET image_url = %s
            WHERE slug = %s
        """
        cursor.execute(update_query, (new_image_url, slug))
        conn.commit()

        print(f"✅ Đã cập nhật ảnh cho slug '{slug}'")
    except Exception as e:
        print("❌ Lỗi khi cập nhật DB:", e)
    finally:
        if cursor:
            cursor.close()
        if conn:
            conn.close()

# Đọc file JSON
def read_json_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return data
    except Exception as e:
        print(f"Lỗi khi đọc file JSON {file_path}: {e}")
        raise

places_data = read_json_file('output/cleaned/places_img_cleaned.json')
for place in places_data:
    update_place_image_url_by_slug(place['slug'], place['img'])