package com.example.geoutils;

import android.annotation.SuppressLint;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {
    private static final String TAG = "Cryptography";
    private static final String KEY = "your_secret_key"; // Chave AES (deve ter 16, 24 ou 32 bytes)

    public static String encryptToBase64(Region region) {
        Gson gson = new Gson();
        String regionJson = gson.toJson(region);
        return encrypt(regionJson);
    }

    public static Region decryptFromBase64(String encryptedData) {
        String decryptedJson = decrypt(encryptedData);
        if (decryptedJson == null) {
            return null;
        }

        Gson gson = new Gson();
        Region region = gson.fromJson(decryptedJson, Region.class);

        // Verifica se é uma instância de SubRegion ou RestrictedRegion para descriptografar recursivamente
        if (region instanceof SubRegion) {
            SubRegion subRegion = (SubRegion) region;
            subRegion.setMainRegion(decryptFromBase64(subRegion.getMainRegion().getEncryptedData())); // descriptografa mainRegion
        } else if (region instanceof RestrictedRegion) {
            RestrictedRegion restrictedRegion = (RestrictedRegion) region;
            restrictedRegion.setMainRegion(decryptFromBase64(restrictedRegion.getMainRegion().getEncryptedData())); // descriptografa mainRegion
        }

        return region;
    }

    private static String encrypt(String data) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "Erro durante a criptografia: " + e.getMessage());
            return null;
        }
    }

    private static String decrypt(String encryptedData) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), "AES");
            @SuppressLint("GetInstance") Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.DEFAULT));
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Log.e(TAG, "Erro durante a descriptografia: " + e.getMessage());
            return null;
        }
    }
}
