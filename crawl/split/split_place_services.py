import json

with open('output/cleaned/places_cleaned.json', 'r', encoding='utf-8') as file:
    data = json.load(file)

service_key = 'Các tùy chon dịch vụ'
planning_values = []

for hotel in data:
    highlights = hotel.get('service', [])
    for item in highlights:
        # if isinstance(item, dict):
        #     planning_values.extend(item.keys())
        if service_key in item:
            planning_values.extend(item[service_key])

planning_values = list(set(planning_values))

with open("output/facilitiesValue.json", "w", encoding="utf-8") as f:
    json.dump(planning_values, f, ensure_ascii=False, indent=2)