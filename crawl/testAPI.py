# from openai import OpenAI

# client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

# # From the sentence: 'find cheap hotels', extract the location if any. If no location is mentioned, return null and tell whether it is generic or specific. Use JSON format like this: {\"location\": \"beach\", \"type\": \"generic\"}

# completion = client.chat.completions.create(
#   model="gpt-4o-mini",
#   store=True,
#   messages=[
#     {
#       "role": "user",
#       "content": 'Extract location from: "find cheap hotels". If none, return {"location": null, "type": null}. Else, set type as "generic" or "specific".'
#     }
#   ]
# )

# print(completion.choices[0].message.content);

import json
import os
from openai import OpenAI

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

def extract_location_info(sentence: str):
    prompt = f'Extract location from: "{sentence}". If none, return {{"location": null, "type": null}}. Else, set type as "generic" or "specific".'
    
    completion = client.chat.completions.create(
        model="gpt-4o-mini",
        store=True,
        messages=[
            {
                "role": "user",
                "content": prompt
            }
        ]
    )
    
    return json.loads(completion.choices[0].message.content)


def plan_trip( days, preferences, hotels, places):
    hotel_str = "\n".join([f"- {h['name']}: {h['description']}" for h in hotels])
    place_str = "\n".join([f"- {p['name']}: {p['description']}" for p in places])

    prompt = f"""
    Người dùng muốn đi du lịch tại Đà Nẵng trong {days} ngày.

    Yêu cầu cá nhân: {preferences}

    Các khách sạn có sẵn:
    {hotel_str}

    Các địa điểm tham quan tại Đà Nẵng:
    {place_str}

    Hãy gợi ý một lịch trình {days} ngày chi tiết, gồm:
    - Chọn khách sạn phù hợp
    - Lên kế hoạch mỗi ngày (sáng, chiều, tối)
    - Lý do vì sao lịch trình này phù hợp
    """
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        store=True,
        messages=[
            {"role": "system", "content": "Bạn là một chuyên gia lên lịch trình du lịch chuyên nghiệp."},
            {"role": "user", "content": prompt}
        ]
    )

    return response.choices[0].message.content.strip()

# Ví dụ sử dụng:
# result = extract_location_info("find cheap hotels")
# print(result['location'])

hotels = [
  {
    "name": "Khách sạn Biển Xanh",
    "description": "Gần bãi biển Mỹ Khê, có nhà hàng hải sản, giá hợp lý"
  },
  {
    "name": "Central Hotel",
    "description": "Nằm trung tâm Đà Nẵng, tiện đi cầu Rồng và chợ Cồn"
  }
]

places = [
  {
    "name": "Bãi biển Mỹ Khê",
    "description": "Tắm biển, chụp ảnh hoàng hôn"
  },
  {
    "name": "Bà Nà Hills",
    "description": "Cáp treo, vườn hoa, làng Pháp"
  },
  {
    "name": "Ngũ Hành Sơn",
    "description": "Leo núi và chùa Linh Ứng"
  }
]

result = plan_trip(2, "thích ngắm cảnh biển và ăn hải sản", hotels, places)
print(result)