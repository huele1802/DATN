# pip install playwright pandas openpyxsl
# playwright install chromium

# crawl link

from playwright.sync_api import sync_playwright
import pandas as pd
import time
import sys

sys.stdout.reconfigure(encoding='utf-8')


def main():
    with sync_playwright() as p:
        # Cấu hình trình duyệt
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        # Thay đổi ngày check-in và check-out
        checkin_date = '2025-03-19'
        checkout_date = '2025-04-20'
        
        # URL Booking với các thông số tìm kiếm
        #page_url = f'https://www.booking.com/searchresults.vi.html?ss=%C4%90%C3%A0+N%E1%BA%B5ng%2C+Th%C3%A0nh+ph%E1%BB%91+%C4%90%C3%A0+N%E1%BA%B5ng%2C+Vi%E1%BB%87t+Nam&ssne=Kon+Tum&ssne_untouched=Kon+Tum&highlighted_hotels=13441614&label=gen173nr-1FCAQoggJCEnNlYXJjaF_EkcOgIG7hurVuZ0gqWARo9AGIAQGYASq4ARfIAQzYAQHoAQH4AQOIAgGoAgO4Ao2g374GwAIB0gIkYzQ5MzZiY2MtZmJiOC00ZDQ1LWFlYjYtZWMyNzFjMmYxNDcw2AIF4AIB&sid=8916a788d90c0e0919b3219e3628fe30&aid=304142&lang=vi&sb=1&src_elem=sb&src=searchresults&dest_id=-3712125&dest_type=city&ac_position=0&ac_click_type=b&ac_langcode=vi&ac_suggestion_list_length=5&search_selected=true&search_pageview_id=14f83f61175505d0&ac_meta=GhAxNGY4M2Y2MTE3NTUwNWQwIAAoATICdmk6AURAAEoAUAA%3D&checkin=2025-03-18&checkout=2025-03-20&group_adults=2&no_rooms=1&group_children=0'
        page_url = f'https://www.booking.com/searchresults.vi.html?checkin={checkin_date}&checkout={checkout_date}&selected_currency=VND&ss=Da%20Nang&ssne=Da%20Nang&ssne_untouched=Da%20Nang&lang=vi&sb=1&src_elem=sb&src=searchresults&dest_type=city'

        page.goto(page_url, timeout=60000) # trang tải xong trước 60 giây, code tiếp tục chạy

        hotels_list = []
        last_height = 0

        while True:
            # Lấy danh sách khách sạn hiện tại
            hotels = page.locator('//div[@data-testid="property-card"]').all()

            for hotel in hotels:
                try:
                    hotel_dict = {
                        "hotel": hotel.locator('//div[@data-testid="title"]').inner_text(),
                        "price": hotel.locator('//span[@data-testid="price-and-discounted-price"]').inner_text(),
                        "review-score": hotel.locator('//div[@data-testid="review-score"]/div[2]/div[2]').inner_text().split()[0],
                        "rating": hotel.locator('//div[@data-testid="review-score"]/div[1]').inner_text(),
                        "image": hotel.locator('//a[@data-testid="property-card-desktop-single-image"]/img').get_attribute("src")

                    }
                    hotels_list.append(hotel_dict)
                except:
                    continue

            page.evaluate("window.scrollBy(0, document.body.scrollHeight)")
            time.sleep(3)  # Chờ trang tải thêm nội dung

            new_height = page.evaluate("document.body.scrollHeight")
            if new_height == last_height:
                break
            last_height = new_height

        # Lưu dữ liệu vào CSV
        df = pd.DataFrame(hotels_list)
        df.to_csv('hotels_list.csv', index=False, encoding='utf-8')

        print(f" Đã lưu {len(hotels_list)} khách sạn vào hotels_list.csv!")
        browser.close()


def crawl_booking2():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)  # Đặt True nếu muốn chạy ẩn danh
        page = browser.new_page()

        checkin_date = '2025-04-29'
        checkout_date = '2025-04-30'

        page_url = f'https://www.booking.com/searchresults.vi.html?ss=%C4%90%C3%A0+N%E1%BA%B5ng%2C+Vi%C3%AA%CC%A3t+Nam&efdco=1&label=gen173bo-1DCAEoggI46AdIKlgDaPQBiAEBmAEquAEXyAEM2AED6AEB-AEDiAIBmAICqAIDuAL29evBBsACAdICJGJhYzM3MzIyLTdlNTktNGNkZC05ODFiLTE3NzkyMzk1ZDRiNNgCBOACAQ&sid=e3e6407b406303ce34e87fb490817b2c&aid=304142&lang=vi&sb=1&src_elem=sb&src=index&dest_id=-3712125&dest_type=city&group_adults=2&no_rooms=1&group_children=0'
        # page_url = f'https://www.booking.com/searchresults.vi.html?label=gen173nr-1BCAEoggI46AdIM1gEaPQBiAEBmAEquAEXyAEM2AEB6AEBiAIBqAIDuAKW7P2-BsACAdICJDkxMmVhYzIwLTI3MzQtNDg3ZS1iOWZiLWRlZjc5NmU4MzAyN9gCBeACAQ&sid=29c32ccf034192589443cdcaa6bb25e2&aid=304142&ss=%C4%90%C3%A0+N%E1%BA%B5ng%2C+Th%C3%A0nh+ph%E1%BB%91+%C4%90%C3%A0+N%E1%BA%B5ng%2C+Vi%E1%BB%87t+Nam&ssne=%C4%90%C3%A0+N%E1%BA%B5ng&ssne_untouched=%C4%90%C3%A0+N%E1%BA%B5ng&efdco=1&lang=vi&src=index&dest_id=-3712125&dest_type=city&ac_position=0&ac_click_type=b&ac_langcode=vi&ac_suggestion_list_length=5&search_selected=true&search_pageview_id=5396138bd52b0110&checkin={checkin_date}&checkout={checkout_date}&group_adults=2&no_rooms=1&group_children=0&nflt=ht_id%3D204'
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