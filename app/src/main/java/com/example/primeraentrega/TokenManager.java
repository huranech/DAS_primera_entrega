package com.example.primeraentrega;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class TokenManager extends Worker {

    // constructora
    public TokenManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // paso 1: recibir parámetros
        String url = getInputData().getString("url");
        String key = getInputData().getString("key");
        String token = getInputData().getString("token");
        String nombre = getInputData().getString("nombre");
        String ingredientes = getInputData().getString("ingredientes");
        String descripcion = getInputData().getString("descripcion");

        // paso 2: configurar la conexión HTTP
        HttpURLConnection urlConnection = null;
        try {
            // Configurar la conexión HTTP
            URL urlObject = new URL(url);
            urlConnection = (HttpURLConnection) urlObject.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setDoOutput(true);

            // paso 3: construir el cuerpo de la solicitud
            String postData = "token_dispositivo=" + token +
                    "&" + URLEncoder.encode(key, "UTF-8") +
                    "&nombre=" + nombre +
                    "&ingredientes=" + ingredientes +
                    "&descripcion=" + descripcion;

            // paso 3: enviar la solicitud
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // paso 4 procesar la respuesta del servidor
            int statusCode = urlConnection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK) {
                BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.append(line);
                }
                inputStream.close();

                String responseData = response.toString();
                // obtener datos del JSON
                JSONObject jsonResponse = new JSONObject(responseData);
                boolean success = jsonResponse.getBoolean("success");
                String mensaje = jsonResponse.getString("message");


                // paso 5: indicar que la tarea se ha completado exitosamente y enviar el indicador de éxito
                Data outputData = new Data.Builder()
                        .putBoolean("success", success)
                        .build();
                return Result.success(outputData);
            } else {
                return Result.failure();
            }
        } catch (IOException | JSONException e) {
            // manejar cualquier excepción que pueda ocurrir durante la solicitud
            Log.e("TokenManager", "Excepción durante la solicitud HTTP", e);
            return Result.failure();
        } finally {
            // cerrar la conexión después de usarla
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
