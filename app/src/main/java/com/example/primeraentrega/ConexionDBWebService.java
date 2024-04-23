package com.example.primeraentrega;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ConexionDBWebService extends Worker {

    public ConexionDBWebService(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // recibir los datos de entrada
        String url = getInputData().getString("url");
        String username = getInputData().getString("username");
        String password = getInputData().getString("password");
        String key = getInputData().getString("key");
        int id_usuario = getInputData().getInt("userId", -1);

        HttpURLConnection urlConnection = null;
        try {
            // configurar la conexión HTTP
            URL urlObject = new URL(url);
            urlConnection = (HttpURLConnection) urlObject.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setDoOutput(true);

            // construir el cuerpo de la solicitud
            String postData = "newUsername=" + URLEncoder.encode(username, "UTF-8") +
                    "&newPassword=" + URLEncoder.encode(password, "UTF-8") +
                    "&" + URLEncoder.encode(key, "UTF-8") + "=true" +
                    "&id_usuario=" + id_usuario;

            // enviar la solicitud
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // procesar la respuesta del servidor
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

                // procesar la respuesta del servidor
                String responseData = response.toString();
                JSONObject jsonResponse = new JSONObject(responseData);
                int userId = jsonResponse.getInt("idUsuario");
                boolean success = jsonResponse.getBoolean("success");

                // guardar el ID de usuario en las preferencias
                MaePreferenceManager.getMiPreferenceManager(getApplicationContext()).setLoginId(userId);

                // indicar que la tarea se ha completado exitosamente
                Data outputData = new Data.Builder()
                        .putBoolean("success", success)
                        .putInt("userId", userId)
                        .build();
                return Result.success(outputData);
            } else {
                Log.e("ConexionDBWebService", "Error en la solicitud HTTP: " + statusCode);
                return Result.failure();
            }
        } catch (IOException | JSONException e) {
            // manejar cualquier excepción que pueda ocurrir durante la solicitud
            Log.e("ConexionDBWebService", "Excepción durante la solicitud HTTP", e);
            return Result.failure();
        } finally {
            // cerrar la conexión después de usarla
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
