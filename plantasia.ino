#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <LittleFS.h>
#include <TFT_eSPI.h>
#include "cover_plantasia_mort.h"  // Include the cover image
#include "monstera.h"              // Include the monstera plant image
#include "drop.h"                  // Include the water drop icon
#include "calendar.h"              // Include the calendar icon

TFT_eSPI tft = TFT_eSPI();

const char* ssid = "Plantasia";
const char* password = "plantasia123";

ESP8266WebServer server(80);

int waterLevel = 8;
int calendarDays = 12;

#define PLANT_PATH "/plant.bin"
#define PLANT_SIZE (240 * 240 * 2)  // 115200 bytes

File uploadFile;

void setup() {
  Serial.begin(115200);

  // Initialize LittleFS
  if (!LittleFS.begin()) {
    Serial.println("LittleFS mount failed, formatting...");
    LittleFS.format();
    LittleFS.begin();
  }
  Serial.println("LittleFS ready");

  tft.init();
  tft.setRotation(0);  // Portrait mode
  tft.fillScreen(TFT_BLACK);

  // Show cover screen
  showCoverScreen();

  // Start WiFi AP and show connection progress
  startAccessPoint();

  // Set up web server routes
  setupRoutes();
  server.begin();
  Serial.println("Web server started");

  // Display the plant
  showPlantScreen();
}

void loop() {
  server.handleClient();
}

void startAccessPoint() {
  WiFi.softAP(ssid, password);

  // Animate dots while AP starts
  for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++) {
      delay(500);

      tft.fillRect(165, 200, 50, 20, TFT_BLACK);

      tft.setTextSize(2);
      tft.setTextColor(TFT_YELLOW, TFT_BLACK);
      tft.setCursor(165, 200);
      for (int k = 0; k <= j; k++) {
        tft.print(".");
      }

      Serial.print("Starting AP");
      for (int k = 0; k <= j; k++) Serial.print(".");
      Serial.println();
    }
  }

  IPAddress ip = WiFi.softAPIP();
  Serial.print("AP IP: ");
  Serial.println(ip);

  // Show IP on screen
  tft.fillRect(0, 190, 240, 50, TFT_BLACK);
  tft.setTextSize(2);
  tft.setTextColor(TFT_GREEN, TFT_BLACK);
  tft.setCursor(20, 195);
  tft.print("AP Ready!");
  tft.setTextSize(1);
  tft.setTextColor(TFT_WHITE, TFT_BLACK);
  tft.setCursor(20, 220);
  tft.print("IP: ");
  tft.print(ip);

  delay(2000);
}

bool hasCustomPlant() {
  return LittleFS.exists(PLANT_PATH);
}

// Draw the full plant image from LittleFS
void drawPlantFromFS() {
  File f = LittleFS.open(PLANT_PATH, "r");
  if (!f) {
    Serial.println("Failed to open plant file");
    return;
  }

  uint8_t buf[480];  // one row = 240 pixels * 2 bytes
  for (int y = 0; y < 240; y++) {
    int bytesRead = f.read(buf, 480);
    if (bytesRead != 480) break;
    for (int x = 0; x < 240; x++) {
      uint16_t pixel = (buf[x * 2] << 8) | buf[x * 2 + 1];
      uint16_t r = (pixel & 0xF800) >> 11;
      uint16_t g = (pixel & 0x07E0);
      uint16_t b = (pixel & 0x001F);
      tft.drawPixel(x, y, (b << 11) | g | r);
    }
  }
  f.close();
}

// Draw a rectangular region of the plant from LittleFS
void drawPlantRegionFromFS(int startX, int startY, int endX, int endY) {
  File f = LittleFS.open(PLANT_PATH, "r");
  if (!f) return;
  uint8_t rowBuf[480];
  for (int y = startY; y < endY; y++) {
    f.seek(y * 480);
    int bytesRead = f.read(rowBuf, 480);
    if (bytesRead != 480) break;
    for (int x = startX; x < endX; x++) {
      uint16_t pixel = (rowBuf[x * 2] << 8) | rowBuf[x * 2 + 1];
      uint16_t r = (pixel & 0xF800) >> 11;
      uint16_t g = (pixel & 0x07E0);
      uint16_t b = (pixel & 0x001F);
      tft.drawPixel(x, y, (b << 11) | g | r);
    }
  }
  f.close();
}

void setupRoutes() {
  server.on("/", HTTP_GET, handleRoot);
  server.on("/update", HTTP_GET, handleUpdate);
  server.on("/status", HTTP_GET, handleStatus);

  // Plant upload: raw body POST
  server.on("/plant", HTTP_POST, handlePlantUploadComplete, handlePlantUploadData);

  // Plant reset
  server.on("/plant", HTTP_DELETE, handlePlantDelete);
}

void handlePlantUploadData() {
  HTTPUpload& upload = server.upload();

  if (upload.status == UPLOAD_FILE_START) {
    uploadFile = LittleFS.open(PLANT_PATH, "w");
    if (!uploadFile) {
      Serial.println("Failed to create plant file");
    }
    Serial.println("Plant upload started");
  } else if (upload.status == UPLOAD_FILE_WRITE) {
    if (uploadFile) {
      uploadFile.write(upload.buf, upload.currentSize);
    }
  } else if (upload.status == UPLOAD_FILE_END) {
    if (uploadFile) {
      uploadFile.close();
      Serial.print("Plant upload complete, size: ");
      Serial.println(upload.totalSize);
    }
  }
}

void handlePlantUploadComplete() {
  // Verify the file size
  File f = LittleFS.open(PLANT_PATH, "r");
  if (!f) {
    server.send(500, "application/json", "{\"error\":\"Failed to save plant\"}");
    return;
  }
  size_t size = f.size();
  f.close();

  if (size != PLANT_SIZE) {
    LittleFS.remove(PLANT_PATH);
    server.send(400, "application/json",
      "{\"error\":\"Invalid size. Expected " + String(PLANT_SIZE) + " bytes, got " + String(size) + "\"}");
    return;
  }

  server.send(200, "application/json", "{\"ok\":true}");
  showPlantScreen();
}

void handlePlantDelete() {
  if (LittleFS.exists(PLANT_PATH)) {
    LittleFS.remove(PLANT_PATH);
  }
  server.send(200, "application/json", "{\"ok\":true,\"plant\":\"default\"}");
  showPlantScreen();
}

void handleRoot() {
  String html = "<!DOCTYPE html><html><head>"
    "<meta name='viewport' content='width=device-width,initial-scale=1'>"
    "<title>Plantasia</title>"
    "<style>"
    "body{font-family:sans-serif;background:#1a1a2e;color:#e0e0e0;text-align:center;padding:20px;margin:0}"
    "h1{color:#7ec8a0}"
    ".stat{font-size:1.5em;margin:15px 0}"
    ".label{color:#aaa}"
    "input[type=number]{width:60px;font-size:1.2em;padding:5px;background:#16213e;color:#fff;border:1px solid #7ec8a0;border-radius:4px;text-align:center}"
    "button{background:#7ec8a0;color:#1a1a2e;border:none;padding:12px 24px;font-size:1.1em;border-radius:8px;margin-top:15px;cursor:pointer}"
    "button:active{background:#5aa880}"
    ".danger{background:#e74c3c;color:#fff}"
    ".danger:active{background:#c0392b}"
    "#status{margin-top:10px;font-size:0.9em;color:#aaa}"
    "</style></head><body>"
    "<h1>Plantasia</h1>"
    "<div class='stat'><span class='label'>Water:</span> <span id='w'>" + String(waterLevel) + "</span></div>"
    "<div class='stat'><span class='label'>Days:</span> <span id='d'>" + String(calendarDays) + "</span></div>"
    "<hr style='border-color:#333;margin:20px 0'>"
    "<div>Water: <input type='number' id='wi' value='" + String(waterLevel) + "' min='0' max='99'></div><br>"
    "<div>Days: <input type='number' id='di' value='" + String(calendarDays) + "' min='0' max='99'></div><br>"
    "<button onclick=\"fetch('/update?water='+document.getElementById('wi').value+'&days='+document.getElementById('di').value)"
    ".then(r=>r.json()).then(d=>{document.getElementById('w').textContent=d.water;document.getElementById('d').textContent=d.days})\">Update</button>"
    "<hr style='border-color:#333;margin:20px 0'>"
    "<h2 style='color:#7ec8a0;font-size:1.2em'>Plant Image</h2>"
    "<p style='font-size:0.85em;color:#aaa'>Upload a 240x240 RGB565 raw binary file (115,200 bytes)</p>"
    "<input type='file' id='pf' accept='.bin,.raw' style='margin:10px 0'><br>"
    "<button onclick='uploadPlant()'>Upload Plant</button> "
    "<button class='danger' onclick='resetPlant()'>Reset to Default</button>"
    "<div id='status'></div>"
    "<script>"
    "function uploadPlant(){"
      "var f=document.getElementById('pf').files[0];"
      "if(!f){document.getElementById('status').textContent='Select a file first';return;}"
      "if(f.size!=115200){document.getElementById('status').textContent='File must be exactly 115,200 bytes (got '+f.size+')';return;}"
      "var fd=new FormData();fd.append('plant',f,'plant.bin');"
      "document.getElementById('status').textContent='Uploading...';"
      "fetch('/plant',{method:'POST',body:fd})"
      ".then(r=>r.json()).then(d=>{"
        "document.getElementById('status').textContent=d.ok?'Upload complete!':'Error: '+(d.error||'unknown');"
      "}).catch(e=>document.getElementById('status').textContent='Upload failed: '+e);"
    "}"
    "function resetPlant(){"
      "if(!confirm('Reset to default monstera?'))return;"
      "fetch('/plant',{method:'DELETE'})"
      ".then(r=>r.json()).then(d=>{"
        "document.getElementById('status').textContent='Reset to default plant';"
      "}).catch(e=>document.getElementById('status').textContent='Reset failed: '+e);"
    "}"
    "</script>"
    "</body></html>";
  server.send(200, "text/html", html);
}

void handleUpdate() {
  if (server.hasArg("water")) {
    waterLevel = server.arg("water").toInt();
  }
  if (server.hasArg("days")) {
    calendarDays = server.arg("days").toInt();
  }

  refreshStats();

  String json = "{\"water\":" + String(waterLevel) + ",\"days\":" + String(calendarDays) + "}";
  server.send(200, "application/json", json);

  Serial.print("Updated - water: ");
  Serial.print(waterLevel);
  Serial.print(", days: ");
  Serial.println(calendarDays);
}

void handleStatus() {
  String json = "{\"water\":" + String(waterLevel) + ",\"days\":" + String(calendarDays) + "}";
  server.send(200, "application/json", json);
}

void refreshStats() {
  // Redraw the plant background in the stats area
  int clearY = 240 - DROP_HEIGHT - CALENDAR_HEIGHT - 12;
  int clearX = 190;

  if (hasCustomPlant()) {
    drawPlantRegionFromFS(clearX, clearY, 240, 240);
  } else {
    for (int y = clearY; y < 240; y++) {
      for (int x = clearX; x < 240; x++) {
        uint16_t pixel = pgm_read_word(&monstera[y * COVER_WIDTH + x]);
        uint16_t r = (pixel & 0xF800) >> 11;
        uint16_t g = (pixel & 0x07E0);
        uint16_t b = (pixel & 0x001F);
        tft.drawPixel(x, y, (b << 11) | g | r);
      }
    }
  }

  drawWaterStat(waterLevel);
  drawCalendarStat(calendarDays);
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

  Serial.println("Cover screen displayed");
}

void showPlantScreen() {
  tft.fillScreen(TFT_BLACK);

  if (hasCustomPlant()) {
    drawPlantFromFS();
    Serial.println("Custom plant displayed from LittleFS");
  } else {
    for (int y = 0; y < COVER_HEIGHT; y++) {
      for (int x = 0; x < COVER_WIDTH; x++) {
        uint16_t pixel = pgm_read_word(&monstera[y * COVER_WIDTH + x]);
        uint16_t r = (pixel & 0xF800) >> 11;
        uint16_t g = (pixel & 0x07E0);
        uint16_t b = (pixel & 0x001F);
        tft.drawPixel(x, y, (b << 11) | g | r);
      }
    }
    Serial.println("Default monstera displayed");
  }

  drawWaterStat(waterLevel);
  drawCalendarStat(calendarDays);
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

void drawCalendarStat(int days) {
  int iconX = 240 - CALENDAR_WIDTH - 30;
  int iconY = 240 - DROP_HEIGHT - CALENDAR_HEIGHT - 8;
  // Draw calendar icon, skipping black (transparent) pixels
  for (int y = 0; y < CALENDAR_HEIGHT; y++) {
    for (int x = 0; x < CALENDAR_WIDTH; x++) {
      uint16_t pixel = pgm_read_word(&calendar[y * CALENDAR_WIDTH + x]);
      if (pixel != 0x0000) {
        uint16_t r = (pixel & 0xF800) >> 11;
        uint16_t g = (pixel & 0x07E0);
        uint16_t b = (pixel & 0x001F);
        uint16_t swapped = (b << 11) | g | r;
        tft.drawPixel(iconX + x, iconY + y, swapped);
      }
    }
  }

  // Draw the days to the right of the icon
  tft.setTextSize(2);
  tft.setTextColor(TFT_WHITE);
  tft.setCursor(iconX + CALENDAR_WIDTH + 4, iconY + (CALENDAR_HEIGHT - 16) / 2);
  tft.print(days);
}
