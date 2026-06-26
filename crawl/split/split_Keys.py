import json

with open('output/cleaned/hotels_cleaned.json', 'r', encoding='utf-8') as file:
    data1 = json.load(file)

unique_keys = set()
unique_service_keys = set()
highlights_data = set()
districts = set()
amenities_keys = set()

for hotel in data1:
    for amenity in hotel.get('facilities', []):
        amenities_keys.add(amenity)

    reviews = hotel.get('reviews', {})
    for key in reviews.keys():
        unique_keys.add(key)
    
    room_services = hotel.get('room_services', {})
    for key in room_services.keys():
        unique_service_keys.add(key)
    
    highlights = hotel.get('highlights', {})
    for category, contents in highlights.items():
        highlights_data.add(category)

    districts.add(hotel['district'])
    
unique_keys_list = list(unique_keys)
unique_service_keys_list = list(unique_service_keys)
unique_highlight_keys_list = list(highlights_data)
district_list = list(districts)
unique_amenities_keys_list = list(amenities_keys)

with open("output/facilities_list.json", "w", encoding="utf-8") as f:
    json.dump(unique_keys_list, f, ensure_ascii=False, indent=2)

with open("output/room_Services_Key.json", "w", encoding="utf-8") as f:
    json.dump(unique_service_keys_list, f, ensure_ascii=False, indent=2)

with open("output/room_Highlights_Key.json", "w", encoding="utf-8") as f:
    json.dump(unique_highlight_keys_list, f, ensure_ascii=False, indent=2)

with open("output/district_list.json", "w", encoding="utf-8") as f:
    json.dump(district_list, f, ensure_ascii=False, indent=2)

with open("output/facilities_list.json", "w", encoding="utf-8") as f:
    json.dump(unique_amenities_keys_list, f, ensure_ascii=False, indent=2)