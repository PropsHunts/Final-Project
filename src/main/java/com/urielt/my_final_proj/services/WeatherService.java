package com.urielt.my_final_proj.services;

import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchCurrentWeather(double latitude, double longitude) {
        // פנייה ל-API החיצוני עם קווי הרוחב והאורך של העיר
        String url = String.format(
            "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m",
            latitude, longitude
        );

        try {
            String jsonResponse = restTemplate.getForObject(url, String.class);
            if (jsonResponse == null) return "--°C";

            // חילוץ דינמי של הטמפרטורה מתוך ה-JSON שחזר מהאינטרנט
            JSONObject rootJson = new JSONObject(jsonResponse);
            JSONObject currentJson = rootJson.getJSONObject("current");
            double temp = currentJson.getDouble("temperature_2m");
            
            return temp + "°C"; // מחזיר למשל "22.5°C" מהאינטרנט
        } catch (Exception e) {
            System.err.println("שגיאה במשיכת נתוני מזג אוויר: " + e.getMessage());
            return "--°C";
        }
    }
}