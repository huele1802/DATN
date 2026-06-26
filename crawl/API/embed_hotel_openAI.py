import os

from openai import OpenAI
import json
import numpy as np
import time
from tqdm import tqdm

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# ----- Chuyển đổi dữ liệu khách sạn thành văn bản -----
def hotel_to_text(hotel):
    parts = []
    parts.append(f"{hotel['name']} là khách sạn {str(hotel['rating_stars']) + ' sao' if hotel['rating_stars'] else ''} nằm tại địa chỉ {hotel['address']}. {hotel['description']}")
    
    if hotel.get("facilities"):
        parts.append("Tiện nghi: " + ", ".join(hotel["facilities"]))

    if hotel.get("highlights"):
        highlight_texts = []
        for key, values in hotel["highlights"].items():
            highlight_texts.append(f"{key}: {', '.join(values)}")
        parts.append("Điểm nổi bật: " + " | ".join(highlight_texts))

    if hotel.get("room_services"):
        room_service_texts = []
        for section, items in hotel["room_services"].items():
            room_service_texts.append(f"{section}: {', '.join(items)}")
        parts.append("Dịch vụ phòng: " + " | ".join(room_service_texts))

    return ".".join(parts)

def room_to_text(room):
    return f"Phòng {room['name']} - Cho {room['number_of_guests']} khách - Giá: {room['price']} VNĐ - Giá gốc: {room['original_price']} VNĐ"

# ----- Đọc JSON -----
def read_json_file(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        raise RuntimeError(f"Lỗi khi đọc file JSON: {e}")

# ----- Chuyển text -> embedding dùng OpenAI -----
def get_openai_embedding(text, model="text-embedding-3-large", retry_delay=2, max_retries=5):
    for attempt in range(max_retries):
        try:
            response = client.embeddings.create(input=[text], model=model)
            return response.data[0].embedding
        except Exception as e:
            print(f"Lỗi embedding (lần thử {attempt + 1}): {e}")
            time.sleep(retry_delay + attempt)  # tăng dần thời gian chờ
    raise RuntimeError("❌ Quá số lần retry embedding")

def text_to_embeddings_openai(texts, delay_every=20, delay_seconds=1):
    embeddings = []
    for i, text in enumerate(tqdm(texts, desc="Embedding")):
        embedding = get_openai_embedding(text)
        embeddings.append(embedding)
        if (i + 1) % delay_every == 0:
            time.sleep(delay_seconds)
    return np.array(embeddings)

# ----- Lưu numpy -----
def save_embeddings(embeddings, output_file):
    np.save(output_file, embeddings)

# ----- Embedding cho khách sạn -----
def generate_hotel_embeddings(data, output_file):
    hotel_texts = [hotel_to_text(hotel) for hotel in data]
    embeddings = text_to_embeddings_openai(hotel_texts)
    save_embeddings(embeddings, output_file)
    print(f"✅ Đã tạo và lưu {len(embeddings)} hotel embeddings vào {output_file}")

# ----- Embedding cho phòng -----
def generate_room_embeddings(data, output_file):
    room_texts = []
    for hotel in data:
        if hotel.get("room_types"):
            for room in hotel["room_types"]:
                room_texts.append(room_to_text(room))
    embeddings = text_to_embeddings_openai(room_texts, 40)
    save_embeddings(embeddings, output_file)
    print(f"✅ Đã tạo và lưu {len(embeddings)} room embeddings vào {output_file}")

# ----- Hàm chính -----
def main(json_file, hotel_output_file, room_output_file):
    data = read_json_file(json_file)
    generate_hotel_embeddings(data, hotel_output_file)
    generate_room_embeddings(data, room_output_file)

# ----- Chạy thử -----
if __name__ == "__main__":
    json_file = 'output3105/cleaned/hotels_cleaned.json'
    hotel_output_file = 'output3105/embedding/hotel_embeddings_openai.npy'
    room_output_file = 'output3105/embedding/room_embeddings_openai.npy'
    
    main(json_file, hotel_output_file, room_output_file)
