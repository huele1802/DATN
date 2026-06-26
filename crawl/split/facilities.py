import json

with open('output/cleaned/hotels_cleaned.json', 'r', encoding='utf-8') as file:
    data = json.load(file)

unique_keys = set()

for hotel in data:
    reviews = hotel.get('room_services', {})
    contents = reviews.get('Các tiện nghi khác', [])
    if isinstance(contents, list):
        for content in contents:
            unique_keys.add(content)
    
unique_keys_list = list(unique_keys)

with open("output/facilitiesValue.json", "w", encoding="utf-8") as f:
    json.dump(unique_keys_list, f, ensure_ascii=False, indent=2)