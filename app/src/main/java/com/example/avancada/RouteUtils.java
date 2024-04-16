package com.example.avancada;

import android.graphics.Color;
import android.util.Log;

import com.example.geoutils.Region;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.TravelMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class RouteUtils {

    private static final String TAG = "RouteUtils";
    private static final String API_KEY = "YOUR_API_KEY"; // Substitua pelo seu próprio API key

    public static void drawRoute(GoogleMap googleMap, Queue<Region> regionQueue) {
        // Verifica se há pelo menos dois pontos na fila
        if (regionQueue.size() < 2) {
            Log.e(TAG, "É necessário ter pelo menos dois pontos na fila para calcular a rota.");
            return;
        }

        // Obtém os dois últimos pontos da fila
        Region lastRegion1 = null;
        Region lastRegion2 = null;
        for (Region region : regionQueue) {
            lastRegion2 = lastRegion1;
            lastRegion1 = region;
        }

        // Verifica se os pontos foram encontrados
        if (lastRegion1 == null || lastRegion2 == null) {
            Log.e(TAG, "Não foi possível encontrar os dois últimos pontos da fila.");
            return;
        }

        // Cria o contexto da API Geo
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey(API_KEY)
                .build();

        // Faz a solicitação da rota usando a Directions API do Google Maps
        DirectionsApi.newRequest(geoApiContext)
                .origin(new com.google.maps.model.LatLng(lastRegion2.getLatitude(), lastRegion2.getLongitude()))
                .destination(new com.google.maps.model.LatLng(lastRegion1.getLatitude(), lastRegion1.getLongitude()))
                .mode(TravelMode.DRIVING)
                .setCallback(new PendingResult.Callback<DirectionsResult>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        if (result != null && result.routes != null && result.routes.length > 0) {
                            // Desenha a rota no mapa
                            drawPolylineOnMap(googleMap, result.routes[0].overviewPolyline.getEncodedPath());
                        } else {
                            Log.e(TAG, "Falha ao traçar a rota.");
                        }
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        Log.e(TAG, "Erro ao obter a rota: " + e.getMessage());
                    }
                });
    }

    private static void drawPolylineOnMap(GoogleMap googleMap, String encodedPath) {
        // Decodifica o caminho codificado e adiciona a polyline ao mapa
        List<LatLng> decodedPath = PolylineDecoder.decodePoly(encodedPath);
        PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(decodedPath)
                .width(10)
                .color(Color.RED);
        Polyline polyline = googleMap.addPolyline(polylineOptions);
    }

    static class PolylineDecoder {
        public static List<LatLng> decodePoly(String encoded) {
            List<LatLng> poly = new ArrayList<>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng(((double) lat / 1E5), ((double) lng / 1E5));
                poly.add(p);
            }
            return poly;
        }
    }
}
