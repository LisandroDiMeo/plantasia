#include <TFT_eSPI.h>
#include "cover_plantasia_mort.h"  // Include the cover image
#include "monstera.h"              // Include the monstera plant image
#include "drop.h"                  // Include the water drop icon

TFT_eSPI tft = TFT_eSPI();

void setup() {
  Serial.begin(115200);
  
  tft.init();
  tft.setRotation(0);  // Portrait mode
  tft.fillScreen(TFT_BLACK);
  
  // Show cover screen
  showCoverScreen();
  
  // Simulate connection process
  simulateConnection();
  
  // Display the plant
  showPlantScreen();
}

void loop() {
  // Idle for now
  delay(100);
}

void showCoverScreen() {
  tft.fillScreen(TFT_BLACK);
  
  // Draw with RGB/BGR swap
  for (int y = 0; y < COVER_HEIGHT; y++) {
    for (int x = 0; x < COVER_WIDTH; x++) {
      uint16_t pixel = pgm_read_word(&cover_image[y * COVER_WIDTH + x]);
      
      // Swap red and blue channels
      // RGB565: RRRRR GGGGGG BBBBB
      uint16_t r = (pixel & 0xF800) >> 11;  // Extract red (bits 15-11)
      uint16_t g = (pixel & 0x07E0);        // Keep green (bits 10-5)
      uint16_t b = (pixel & 0x001F);        // Extract blue (bits 4-0)
      
      // Reconstruct as BGR
      uint16_t swapped = (b << 11) | g | r;
      
      tft.drawPixel(x, y, swapped);
    }
  }
  
  // Status text
  tft.fillRect(0, 200, 240, 40, TFT_BLACK);
  tft.setTextSize(2);
  tft.setTextColor(TFT_YELLOW, TFT_BLACK);
  tft.setCursor(40, 210);
  tft.print("Connecting");
  
  Serial.println("Cover screen displayed");
}
void simulateConnection() {
  // Animate dots while "connecting"
  for (int i = 0; i < 3; i++) {  // 3 cycles
    for (int j = 0; j < 3; j++) {
      delay(500);
      
      // Clear dot area
      tft.fillRect(165, 200, 50, 20, TFT_BLACK);
      
      // Draw dots
      tft.setTextSize(2);
      tft.setTextColor(TFT_YELLOW, TFT_BLACK);
      tft.setCursor(165, 200);
      for (int k = 0; k <= j; k++) {
        tft.print(".");
      }
      
      Serial.print("Connecting");
      for (int k = 0; k <= j; k++) Serial.print(".");
      Serial.println();
    }
  }
  
  // Connection success
  tft.fillRect(0, 190, 240, 30, TFT_BLACK);
  tft.setTextColor(TFT_GREEN, TFT_BLACK);
  tft.setCursor(50, 200);
  tft.println("Connected!");
  
  Serial.println("Connection successful!");
  delay(1000);
}

void showPlantScreen() {
  tft.fillScreen(TFT_BLACK);


  for (int y = 0; y < COVER_HEIGHT; y++) {
    for (int x = 0; x < COVER_WIDTH; x++) {
      uint16_t pixel = pgm_read_word(&monstera[y * COVER_WIDTH + x]);
      uint16_t r = (pixel & 0xF800) >> 11;
      uint16_t g = (pixel & 0x07E0);
      uint16_t b = (pixel & 0x001F);
      tft.drawPixel(x, y, (b << 11) | g | r);
    }
  }

  drawWaterStat(8);

  Serial.println("Plant screen displayed");
}

void drawWaterStat(int level) {
  int iconX = 240 - DROP_WIDTH - 30;  // room for icon + number
  int iconY = 240 - DROP_HEIGHT - 8;

  // Draw drop icon, skipping black (transparent) pixels
  for (int y = 0; y < DROP_HEIGHT; y++) {
    for (int x = 0; x < DROP_WIDTH; x++) {
      uint16_t pixel = pgm_read_word(&drop[y * DROP_WIDTH + x]);
      if (pixel != 0x0000) {
        uint16_t r = (pixel & 0xF800) >> 11;
        uint16_t g = (pixel & 0x07E0);
        uint16_t b = (pixel & 0x001F);
        uint16_t swapped = (b << 11) | g | r;
        tft.drawPixel(iconX + x, iconY + y, swapped);
      }
    }
  }

  // Draw the level number to the right of the icon
  tft.setTextSize(2);
  tft.setTextColor(TFT_WHITE);
  tft.setCursor(iconX + DROP_WIDTH + 4, iconY + (DROP_HEIGHT - 16) / 2);
  tft.print(level);
}
