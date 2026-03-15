package com.example.calculator;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static final String BASE_URL = "https://api.weatherapi.com/v1/";
    private static Retrofit retrofit = null;
    private static WeatherService service = null;

    public static WeatherService getService() {
        if (service == null) {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
            service = retrofit.create(WeatherService.class);
        }
        return service;
    }
}