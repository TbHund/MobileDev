package com.example.calculator;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {
    @GET("current.json")
    Call<WeatherData> getCurrentWeather(
            @Query("key") String apiKey,
            @Query("q") String city,
            @Query("lang") String lang
    );
}