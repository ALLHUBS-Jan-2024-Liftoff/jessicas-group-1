package com.launchtrip.launchtrip.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.launchtrip.launchtrip.models.Location;
import com.launchtrip.launchtrip.models.data.LocationRepository;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    @Autowired
    LocationRepository locationRepository;

    // Todo: Separate out into three separate functions (searchLocationsFromQuery, fetchPlaceId, and fetchLocations) to utilize each separately and reduce overlap

    private static final String BASE_URL = "http://api.geoapify.com";
    private static final String API_KEY = "81e201745295492d891b0e474458e63c";

    public List<Location> searchLocationsFromQuery(String searchQuery) throws IOException {
        // Call Geocoding API to convert search query into PlaceId

        if (searchQuery.contains(" ")) {
            searchQuery.replace(" ", "%20");
        }


        OkHttpClient geocodingClient = new OkHttpClient().newBuilder()
                .build();
        Request geocodingRequest = new Request.Builder()
                .url("https://api.geoapify.com/v1/geocode/search?text=" + searchQuery + "&format=json&apiKey=" + API_KEY)
                .method("GET", null)
                .build();
        Response geocodingResponse = geocodingClient.newCall(geocodingRequest).execute();

        // Pull PlaceId from geocodingResponse

        // Testing: for now if placeId doesn't work it will default to KC
        String placeId = "51411da04500a557c05942959a3dd08c4340f00101f901d026020000000000c00208";

            if (geocodingResponse.isSuccessful()) {
                ResponseBody geocodingBody = geocodingResponse.body();
                if (geocodingBody != null) {
                    // Look through response to get placeId of first result
                    ObjectMapper geocodingMapper = new ObjectMapper();
                    String geocodingString = geocodingBody.string();
                    // Pull placeId
                    JsonNode root = geocodingMapper.readTree(geocodingString);
                    JsonNode results = root.path("results");
                    if (results.isArray() && results.size() > 0) {
                        JsonNode firstResult = results.get(0);
                        placeId = firstResult.path("place_id").asText();
                    }
                }
            }

            // Start Building URL

            // Append v2/places
            String placesUrl = BASE_URL + "/v2/places";

            // Append Parameters

            // API Key
            placesUrl += "?apiKey=" + API_KEY;

            // Limit
            placesUrl += "&limit=10";

            // Place Types
            // ToDo: create a method for taking in a place type and converting it to a geoapify category
            placesUrl += "&categories=entertainment,natural,catering.restaurant,catering.cafe,catering.bar,catering.taproom";

            // Filter Place
            placesUrl += "&filter=place:" + placeId;

            // Make the HTTP Call
            OkHttpClient placesClient = new OkHttpClient().newBuilder()
                    .build();
            Request placesRequest = new Request.Builder()
                    .url(placesUrl)
                    .method("GET", null)
                    .build();
            Response placesResponse = placesClient.newCall(placesRequest).execute();

            // Convert placesResponse JSON to an array list of Locations

            List<Location> locations = new ArrayList<>();

            if (placesResponse.isSuccessful()) {
                ResponseBody responseBody = placesResponse.body();
                if (responseBody != null) {
                    // Parse the response to a list of Location objects
                    ObjectMapper objectMapper = new ObjectMapper();
                    String responseBodyString = responseBody.string();

                    // Parse JSON response
                    JsonNode root = objectMapper.readTree(responseBodyString);
                    JsonNode features = root.path("features");
                    for (JsonNode feature : features) {
                        JsonNode properties = feature.path("properties");
                        String name = properties.path("name").asText();
                        String city = properties.path("city").asText();
                        String usState = properties.path("state").asText();
                        String country = properties.path("country").asText();
                        String postcode = properties.path("postcode").asText();

                        // Extract categories as list
                        List<String> categories = new ArrayList<>();
                        for (JsonNode category : properties.path("categories")) {
                            categories.add(category.asText());
                        }

                        Location location = new Location(name, city, placeId, usState, country, postcode, categories);
                        locations.add(location);
                    }
                }
            }
            return locations;
        }
    }