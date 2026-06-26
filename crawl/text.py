import json

def hotel_to_text(hotel):
    parts = []

    parts.append(f"Tên khách sạn: {hotel['name']}")
    parts.append(f"Địa chỉ: {hotel['address']}")
    parts.append(f"Mô tả: {hotel['description']}")

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

    return "\n".join(parts)

def room_to_text(room):
    return f"Phòng {room['name']} - Cho {room['number_of_guests']} khách - Giá: {room['price']} VNĐ - Giá gốc: {room['original_price']} VNĐ"

def read_json_file(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    return data

data = read_json_file("output/cleaned/hotels_cleaned.json")

length = 0
lengthroom = 0
num_rooms = 0

for hotel in data:
    text = hotel_to_text(hotel)
    length += len(text.split())
    if hotel.get("room_types"):
        for room in hotel["room_types"]:
            room_text = room_to_text(room)
            lengthroom += len(room_text.split())
            num_rooms += 1

print(length / len(data))
print(len(data))

print(lengthroom / num_rooms)
print(num_rooms)
