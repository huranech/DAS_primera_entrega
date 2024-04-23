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

public class ImageManager extends Worker {

    // constructora
    public ImageManager(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // paso 1: recibir parámetros
        String url = getInputData().getString("url");
        String key = getInputData().getString("key");
        int id_antiguo = getInputData().getInt("id_antiguo", 0);
        int id_imagen = getInputData().getInt("id_imagen", -1);
        Bitmap imagenBitmap = null;
        if (id_imagen == -1) {
            imagenBitmap = BitmapFactory.decodeByteArray(getInputData().getByteArray("imagen"), 0, getInputData().getByteArray("imagen").length);
        }

        // filtrar según tipo de solicitud
        String encodedImage = " ";
        // convertir la imagen a base64
        if (! (imagenBitmap == null)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            imagenBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageData = baos.toByteArray();
            encodedImage = Base64.encodeToString(imageData, Base64.DEFAULT);
        }


        // paso 2: configurar la conexión HTTP
        HttpURLConnection urlConnection = null;
        try {
            URL urlObject = new URL(url);
            urlConnection = (HttpURLConnection) urlObject.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            urlConnection.setDoOutput(true);

            // paso 3: construir el cuerpo de la solicitud
            String postData = "id_imagen=" + id_imagen +
                    "&imagen=" + URLEncoder.encode(encodedImage, "UTF-8") +
                    "&" + URLEncoder.encode(key, "UTF-8") +
                    "&id_antiguo=" + id_antiguo;

            // paso 4: enviar la solicitud
            try (OutputStream os = urlConnection.getOutputStream()) {
                byte[] input = postData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // paso 5: procesar la respuesta del servidor
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
                int idImagen = jsonResponse.getInt("idImagen");
                String mensaje = jsonResponse.getString("message");
                String imagenBase64 = jsonResponse.getString("imagen");

                // filtros según tipo de solicitud
                // procesar la imagen para que pueda ser enviada
                if(!(imagenBase64.equals(""))) {
                    byte[] decodedBytes = Base64.decode(imagenBase64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 200, 200, true);

                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream);
                    byte[] resizedBytes = outputStream.toByteArray();
                    imagenBase64 = Base64.encodeToString(resizedBytes, Base64.DEFAULT);
                }

                // indicar que la tarea se ha completado exitosamente
                Data outputData = new Data.Builder()
                        .putBoolean("success", success)
                        .putInt("idImagen", idImagen)
                        .putString("imagen", imagenBase64)
                        .build();
                return Result.success(outputData);
            } else {
                return Result.failure();
            }
        } catch (IOException | JSONException e) {
            // manejar cualquier excepción que pueda ocurrir durante la solicitud
            Log.e("ImageManager", "Excepción durante la solicitud HTTP", e);
            return Result.failure();
        } finally {
            // cerrar la conexión después de usarla
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }
}
