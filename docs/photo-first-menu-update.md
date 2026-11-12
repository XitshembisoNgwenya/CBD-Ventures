# Photo-first menus and nearby directory

Updated 12 November 2026. The Explore and Assistant screens use the same 24-place directory intended for the 1 km Richfield student zone.

## How the menu cards work

- The directory contains 39 menu entries across 8 food venues.
- 37 entries have a published price; 2 entries have no published price and display “Check menu.”
- Only priced menu entries are used to answer budget questions.
- Explore shows photo-led cards before the user opens the full place details. Assistant menu cards link to the place profile and walking directions.
- When an item does not have its own image, the card uses the venue image. Some venue images are linked from third-party listings and may change.
- Online delivery prices may differ from in-store prices. Confirm the current price with the venue before ordering.

## Menu entries and sources

### Honchos

Source: [Student-supplied storefront photo, address and map pin](https://www.ubereats.com/za/store/honchos-pretorius-street-halaal/QBb3Wj7FRUKacrpASxoBHQ). Online listing prices can differ from walk-in prices; check at the counter.

- **Chicken and Cheese Wrap** — R34.9
- **Beef Burger and Fries** — R36.9
- **Strips and Spicy Rice** — R41.9

### Quick Bake Confectioners & Coffee

Source: [Student-supplied storefront photo, address and map pin](https://www.mrd.com/delivery/restaurant/quick-bake-confectioners-and-coffee-pretoria-central/29058). Menu prices are from Mr D and can differ at the counter.

- **Plain Scone** — R28
- **Fruit Scone** — R31
- **Pie** — R34
- **Cheese Scone** — R35

### SA Shawarma

Source: [Uber Eats business listing and address](https://www.ubereats.com/za/store/sa-shawarma/TwKKdmbkR7SoNK28KwHCzA). Menu prices are from the current Uber Eats listing and can differ at the counter.

- **Veggie Shawarma with Chips** — R20
- **Regular Chicken Shawarma with Chips** — R25
- **Beef Russian Shawarma with Chips** — R30
- **Chicken Shawarma and Cheese with Chips** — R35
- **Beef Shawarma with Chips** — R40
- **Beef Shawarma with Cheese and Chips** — R45
- **Bread, Chips, Achar and Polony** — R16

### Hungry Lion JSL Towers

Source: [Uber Eats store listing; official Hungry Lion menu for item prices](https://www.hungrylion.co.za/menu/). Prices and item photos are from Hungry Lion's official menu; confirm the current price at this outlet.

- **Munch Burger** — R24.9
- **Big Bite 2** — R42.9
- **Big Boss Cheese Burger** — R44.9
- **6 Sticky Wings** — R49
- **3 Chicken Fingers and 1 Dippa** — R29.9
- **The Big One** — R32.9
- **Fomo For One** — R38.9
- **Snack Pack** — R39.9
- **Double Munch Burger** — R34.9
- **Munch Meal** — R49.9
- **6 Zamba Wings** — R45
- **6 Chicken Fingers & 2 Dippas** — R54.9
- **Regular Chips** — R23.9

### Roman's Pizza Pretorius

Source: [Uber Eats listing and address](https://www.ubereats.com/za/store/romans-pizza-pretorius-exp/ry_TIqTHT2qzXN_TpGlpDQ). Menu prices are from the current Uber Eats listing and can differ at the counter.

- **Pizza Pie Pepperoni** — R38
- **Pizza Pie BBQ Chicken** — R38
- **Pizza Pie Chicken Mayo** — R38
- **Roman’s Pizza Selection** — Check menu — no published price
- **BBQ Chicken & Mushroom Pizza** — Check menu — no published price

### Pedros Pretorius Street

Source: [Uber Eats listing and address](https://www.mrd.com/delivery/restaurant/pedros-pretorius-street-pretoria-cbd/29874). Menu prices are from Mr D and can differ at the counter.

- **Sprinkle Chicken Burger** — R42.99
- **Paella Bowl** — R44.99

### Steers Van Der Walt Street

Source: [Uber Eats listing and address](https://www.ubereats.com/za/store/steersvan-der-walt-street-pta-cbd/2Y0ZuaguWHG5i5KYCeX4xA). Menu prices are from the current Uber Eats listing and can differ at the counter.

- **Phanda Cheese Burger** — R64.9
- **Phanda King Steer Burger** — R74.9

### Debonairs Pizza Pretoria CBD

Source: [Uber Eats listing and address](https://www.ubereats.com/za/store/debonairs-pizza-pretoria-cbd/T3aBf8IJW06-nfypvyVQzw). Menu prices are from the current Uber Eats listing and can differ at the counter.

- **BBQ Chicken Pizza** — R38.9
- **Debonairs Chicken Pizza** — R40.9
- **BBQ Beef Pizza** — R38.9

## Data and maintenance

The source of truth for the shared directory, place image references, menu entries, source links and verification dates is src/main/resources/assistant/places.json. Update that file when a venue confirms a new menu or corrects a location. Do not replace an approximate pin with a reported correction until it has been checked.

The black-and-white NxG monogram used in the header is src/main/resources/static/images/nxg-logo.png.
