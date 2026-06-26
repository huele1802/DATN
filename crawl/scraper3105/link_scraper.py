from playwright.sync_api import sync_playwright
import pandas as pd
import time
import sys

sys.stdout.reconfigure(encoding='utf-8')

def crawl_booking2():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)  # Đặt True nếu muốn chạy ẩn danh
        page = browser.new_page()
        
        page_url = f'https://www.booking.com/searchresults.vi.html?ss=%C4%90%C3%A0+N%E1%BA%B5ng%2C+Vi%C3%AA%CC%A3t+Nam&efdco=1&label=gen173bo-1DCAEoggI46AdIKlgDaPQBiAEBmAEquAEXyAEM2AED6AEB-AEDiAIBmAICqAIDuAL29evBBsACAdICJGJhYzM3MzIyLTdlNTktNGNkZC05ODFiLTE3NzkyMzk1ZDRiNNgCBOACAQ&sid=e3e6407b406303ce34e87fb490817b2c&aid=304142&lang=vi&sb=1&src_elem=sb&src=index&dest_id=-3712125&dest_type=city&group_adults=2&no_rooms=1&group_children=0'
        page.goto(page_url, timeout=60000)  # Chờ tối đa 60 giây để tải trang

        hotels_list = []
        last_hotel_count = 0

        print("Đang crawl....")

        while True:
            hotels = page.locator('//div[@data-testid="property-card"]').all()
            new_hotels = hotels[last_hotel_count:]

            for hotel in new_hotels:
                try:
                    hotel_dict = {
                        "hotel-url": hotel.locator('//a[@data-testid="title-link"]').get_attribute("href"),
                    }
                    hotels_list.append(hotel_dict)
                except:
                    continue

            print(f"📢 Đã thu thập {len(new_hotels)} khách sạn mới!")
            last_hotel_count = len(hotels)

            page.evaluate("window.scrollBy(0, document.body.scrollHeight)")
            time.sleep(3)  # Đợi trang tải thêm nội dung

            load_more_button = page.locator('button.bbf83acb81')

            if load_more_button.count() > 0:
                print("Nhấn vào nút 'Tải thêm kết quả' để tiếp tục tải trang...")
                load_more_button.click()
                time.sleep(5)  # Chờ tải dữ liệu mới
            else:
                print("Không còn nút 'Tải thêm kết quả'. Kết thúc quá trình crawl!")
                break

        df = pd.DataFrame(hotels_list)
        df.to_csv('output3105/hotels_link.csv', index=False, encoding='utf-8')

        print(f"✅ Đã lưu {len(hotels_list)} khách sạn vào hotels_link.csv!")
        browser.close()

if __name__ == '__main__':
    # main()
    crawl_booking2()