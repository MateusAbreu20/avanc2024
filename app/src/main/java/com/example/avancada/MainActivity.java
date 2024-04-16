package com.example.avancada;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.geoutils.Region;
import com.example.geoutils.RestrictedRegion;
import com.example.geoutils.SubRegion;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("deletedRegions");

    private GoogleMap gMap;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private Queue<Region> regionQueue = new ConcurrentLinkedQueue<>();
    private Marker currentMarker;
    private static final LatLng BELO_HORIZONTE_LOCATION = new LatLng(-19.9167, -43.9345); // Coordenadas de Belo Horizonte
    private boolean regionsInserted = false;
    private boolean lastRegionWasSubRegion = false; // Flag para controlar o tipo de região a ser inserida

    private Button buttonShowRegions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.id_map);
        supportMapFragment.getMapAsync(this);

        buttonShowRegions = findViewById(R.id.buttonShowRegions);
        buttonShowRegions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegionsDialog();
            }
        });

        Button addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Adiciona uma região apenas se um marcador estiver presente
                if (currentMarker != null) {
                    LatLng latLng = currentMarker.getPosition();
                    // Verifica se já existem regiões no local
                    if (!regionsInserted) {
                        addRegion(latLng);
                        regionsInserted = true;
                    } else {
                        if (lastRegionWasSubRegion) {
                            addRestrictedRegion(latLng);
                        } else {
                            addSubRegion(latLng);
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Nenhuma região selecionada", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button deleteButton = findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Verifica se há regiões na fila
                if (!regionQueue.isEmpty()) {
                    // Remove a região mais recente da fila
                    Region deletedRegion = regionQueue.poll();
                    // Remove o marcador correspondente do mapa
                    if (deletedRegion != null && deletedRegion.getMarker() != null) {
                        deletedRegion.getMarker().remove();
                    }
                    // Salva a região deletada no banco de dados Firebase
                    saveDeletedRegion(deletedRegion);
                } else {
                    Toast.makeText(MainActivity.this, "Nenhuma região para deletar", Toast.LENGTH_SHORT).show();
                }
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
                } else {
                    getLastKnownLocation();
                }
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastKnownLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(MainActivity.this, location -> {
            if (location != null) {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                gMap.clear();

                currentMarker = gMap.addMarker(new MarkerOptions().position(latLng).title("Current Location"));
                gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12));
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        // Move a câmera para Belo Horizonte ao abrir o mapa
        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BELO_HORIZONTE_LOCATION, 12));

        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Remove o marcador atual e adiciona um novo marcador na posição clicada
                if (currentMarker != null) {
                    currentMarker.remove();
                }
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Clicked Location");
                currentMarker = gMap.addMarker(markerOptions);
            }
        });
    }

    private void addRegion(LatLng latLng) {
        // Adiciona a região à fila
        int regionNumber = regionQueue.size() + 1;
        String regionName = "Região " + regionNumber;
        int userCode = 123; // Substitua pelo código de usuário real
        long timestamp = System.nanoTime();
        Region newRegion = new Region(regionName, latLng.latitude, latLng.longitude, userCode, timestamp);
        regionQueue.add(newRegion);

        // Adiciona o marcador ao mapa
        Marker marker = gMap.addMarker(new MarkerOptions().position(latLng).title(newRegion.getName()));
        // Define o marcador na instância da região
        newRegion.setMarker(marker);
    }

    private void addSubRegion(LatLng latLng) {
        // Verifica se a nova sub-região está dentro de 5 metros de qualquer região existente
        for (Region existingRegion : regionQueue) {
            LatLng existingRegionLatLng = new LatLng(existingRegion.getLatitude(), existingRegion.getLongitude());
            double distance = calculateDistance(latLng, existingRegionLatLng);
            if (distance <= 5000) {
                // Adiciona a sub-região apenas se estiver dentro do raio de 5 metros da região existente
                int regionNumber = regionQueue.size() + 1;
                String regionName = "Sub-Região " + regionNumber;
                int userCode = 123; // Substitua pelo código de usuário real
                long timestamp = System.nanoTime();
                SubRegion newSubRegion = new SubRegion(regionName, latLng.latitude, latLng.longitude, userCode, timestamp, existingRegion);
                regionQueue.add(newSubRegion);

                // Adiciona o marcador ao mapa
                Marker marker = gMap.addMarker(new MarkerOptions().position(latLng).title(newSubRegion.getName()));
                // Define o marcador na instância da sub-região
                newSubRegion.setMarker(marker);

                return;
            }
        }
        // Se não estiver dentro do raio de 5 metros de nenhuma região existente, exibe uma mensagem de erro
        Toast.makeText(MainActivity.this, "Nova sub-região está muito distante de uma região existente", Toast.LENGTH_SHORT).show();
    }

    private void addRestrictedRegion(LatLng latLng) {
        // Verifica se a nova região restrita está dentro de 5 metros de qualquer região existente
        for (Region existingRegion : regionQueue) {
            LatLng existingRegionLatLng = new LatLng(existingRegion.getLatitude(), existingRegion.getLongitude());
            double distance = calculateDistance(latLng, existingRegionLatLng);
            if (distance <= 500) {
                // Adiciona a região restrita apenas se estiver dentro do raio de 5 metros da região existente
                int regionNumber = regionQueue.size() + 1;
                String regionName = "Região Restrita " + regionNumber;
                int userCode = 123; // Substitua pelo código de usuário real
                long timestamp = System.nanoTime();
                RestrictedRegion newRestrictedRegion = new RestrictedRegion(regionName, latLng.latitude, latLng.longitude, userCode, timestamp, true);
                newRestrictedRegion.setMainRegion(existingRegion);
                regionQueue.add(newRestrictedRegion);

                // Adiciona o marcador ao mapa
                Marker marker = gMap.addMarker(new MarkerOptions().position(latLng).title(newRestrictedRegion.getName()));
                // Define o marcador na instância da região restrita
                newRestrictedRegion.setMarker(marker);

                // Salva a região deletada no banco de dados Firebase
                saveDeletedRegion(newRestrictedRegion);

                return;
            }
        }
        // Se não estiver dentro do raio de 5 metros de nenhuma região existente, exibe uma mensagem de erro
        Toast.makeText(MainActivity.this, "Nova região restrita está muito distante de uma região existente", Toast.LENGTH_SHORT).show();
    }

    private void saveDeletedRegion(Region deletedRegion) {
        myRef.push().setValue(deletedRegion).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d("Firebase", "Dados salvos com sucesso no banco de dados Firebase");
            } else {
                Log.e("Firebase", "Erro ao salvar dados no banco de dados Firebase", task.getException());
            }
        });
    }

    private double calculateDistance(LatLng location1, LatLng location2) {
        double earthRadius = 6371000; // Raio da Terra em metros
        double lat1 = Math.toRadians(location1.latitude);
        double lon1 = Math.toRadians(location1.longitude);
        double lat2 = Math.toRadians(location2.latitude);
        double lon2 = Math.toRadians(location2.longitude);
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    private void showRegionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Regiões Salvas");

        if (regionQueue.isEmpty()) {
            builder.setMessage("Nenhuma região salva.");
        } else {
            StringBuilder message = new StringBuilder();
            for (Region region : regionQueue) {
                message.append("Nome: ").append(region.getName()).append("\n");
                message.append("Latitude: ").append(region.getLatitude()).append("\n");
                message.append("Longitude: ").append(region.getLongitude()).append("\n");
                message.append("Usuário: ").append(region.getUser()).append("\n");
                message.append("Timestamp: ").append(region.getTimestamp()).append("\n\n");
            }
            builder.setMessage(message.toString());
        }

        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
}
