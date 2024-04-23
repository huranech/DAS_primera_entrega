package com.example.primeraentrega;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.io.ByteArrayOutputStream;
import android.Manifest;

public class EditRecipeActivity extends AppCompatActivity {

    private int id;
    private boolean veganoReceta;
    private EditText nombreEditText;
    private EditText ingredientesEditText;
    private EditText descripcionEditText;
    private CheckBox veganoCheckBox;
    private ImageView imagenReceta;
    private Button botonCambiarImagen;

    private Button guardarButton;
    private int imagenNueva = 0;
    private String URL_SUBIDA = "http://34.65.250.38:8080/subir_imagen.php";
    private String URL_BAJADA = "http://34.65.250.38:8080/bajar_imagen.php";
    private String URL_NOTIFICAR = "http://34.65.250.38:8080/notificar_dispositivos.php";

    // sacar una foto
    private ActivityResultLauncher<Intent> takePictureLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    // procesar imagen
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    subirImagen(imageBitmap, 0);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // obtener preferencias
        MaePreferenceManager preferenceManager = MaePreferenceManager.getMiPreferenceManager(this);
        preferenceManager.applyLanguage(this);
        preferenceManager.applyTheme(this);

        // inflar vistas
        setContentView(R.layout.activity_edit_recipe);

        // Inicializar vistas
        nombreEditText = findViewById(R.id.nombreEditText);
        ingredientesEditText = findViewById(R.id.ingredientesEditText);
        descripcionEditText = findViewById(R.id.descripcionEditText);
        veganoCheckBox = findViewById(R.id.veganoCheckBox);
        guardarButton = findViewById(R.id.guardarButton);
        imagenReceta = findViewById(R.id.recipeImageView);
        botonCambiarImagen = findViewById(R.id.changeImageButton);

        // obtener datos de la receta pasados desde MainActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getInt("_id");
            String nombreReceta = extras.getString("nombre");
            String ingredientesReceta = extras.getString("ingredientes");
            String descripcionReceta = extras.getString("descripcion");
            veganoReceta = extras.getBoolean("vegano");
            int imagen = extras.getInt("imagen");

            // mostrar los datos de la receta en los EditText
            nombreEditText.setText(nombreReceta);
            ingredientesEditText.setText(ingredientesReceta);
            descripcionEditText.setText(descripcionReceta);
            veganoCheckBox.setChecked(veganoReceta);
            if (imagen == 0) {
                Drawable drawable = getResources().getDrawable(R.drawable.default_recipe_image_foreground);
                imagenReceta.setImageDrawable(drawable);
            }
            else {
                bajarImagen(imagen);
            }
        }

        botonCambiarImagen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { cambiarImagen(); }
        });

        // configurar el listener del botón de guardar
        guardarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarCambios();
            }
        });
    }

    private void cambiarImagen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // si no se tienen permisos, solicitarlos
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 123);
        }
        Intent elIntentFoto= new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureLauncher.launch(elIntentFoto);
    }

    private void guardarCambios() {
        // obtener los nuevos valores del EditText
        String nuevoNombre = nombreEditText.getText().toString();
        String nuevosIngredientes = ingredientesEditText.getText().toString();
        String nuevaDescripcion = descripcionEditText.getText().toString();
        boolean nuevoVegano = veganoCheckBox.isChecked();

        // guardar las modificaciones en la receta
        BaseDeDatos.getBaseDeDatos(this).modificarReceta(id, nuevoNombre, nuevosIngredientes, nuevaDescripcion, nuevoVegano ? 1 : 0, imagenNueva);

        // notificar a todos los dispositivos sobre una nueva receta vegana (que antes no lo era)
        if (nuevoVegano && !veganoReceta) {
            Data inputData = new Data.Builder()
                    .putString("key", "notificar_dispositivos")
                    .putString("url", URL_NOTIFICAR)
                    .putString("nombre", nuevoNombre)
                    .putString("ingredientes", nuevosIngredientes)
                    .putString("descripcion", nuevaDescripcion)
                    .build();

            OneTimeWorkRequest notificarRequest = new OneTimeWorkRequest.Builder(TokenManager.class)
                    .setInputData(inputData)
                    .build();

            WorkManager.getInstance(this).enqueue(notificarRequest);

            // observar el resultado de la solicitud de registro
            WorkManager.getInstance(this).getWorkInfoByIdLiveData(notificarRequest.getId()).observe(this, new Observer<WorkInfo>() {
                @Override
                public void onChanged(WorkInfo workInfo) {
                    if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                        boolean success = workInfo.getOutputData().getBoolean("success", false);
                        if (success) {
                            // Notificación exitosa
                        } else {
                            // Notificación fallida
                        }
                    }
                }
            });
        }

        // finalizar actividad (EditRecipeActivity -> MainActivity)
        finish();
    }

    private void subirImagen(Bitmap imagen, int idAntiguo) {
        // procesar la imagen
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imagen.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imagenByteArray = stream.toByteArray();

        // preparar datos
        Data inputData = new Data.Builder()
                .putByteArray("imagen", imagenByteArray)
                .putString("key", "subir_imagen")
                .putInt("id_antiguo", idAntiguo)
                .putString("url", URL_SUBIDA)
                .build();

        OneTimeWorkRequest subirRequest = new OneTimeWorkRequest.Builder(ImageManager.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(subirRequest);

        // observer del resultado de la solicitud
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(subirRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    boolean success = workInfo.getOutputData().getBoolean("success", false);
                    int idImagen = workInfo.getOutputData().getInt("idImagen", -1);
                    String error = workInfo.getOutputData().getString("message");
                    if (success) {
                        // imagen subida correctamente
                        imagenReceta.setImageBitmap(imagen);
                        imagenNueva = idImagen;
                        Toast.makeText(EditRecipeActivity.this, "La imagen se ha guardado", Toast.LENGTH_SHORT).show();
                    } else {
                        // fallo al subir la imagen
                        Toast.makeText(EditRecipeActivity.this, "No se ha podido cambiar la imagen", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    // descargar una imagen del servidor
    private void bajarImagen(int idImagen) {
        // preparar datos para la petición
        Data inputData = new Data.Builder()
                .putInt("id_imagen", idImagen)
                .putString("key", "bajar_imagen")
                .putString("url", URL_BAJADA)
                .build();

        OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ImageManager.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(registerRequest);

        // observer del resultado de la solicitud
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    boolean success = workInfo.getOutputData().getBoolean("success", false);
                    String imagenBase64 = workInfo.getOutputData().getString("imagen");
                    if (success && !imagenBase64.isEmpty()) {
                        // imagen bajada correctamente
                        byte[] decodedBytes = Base64.decode(imagenBase64, Base64.DEFAULT);
                        Bitmap imagen = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        imagenReceta.setImageBitmap(imagen);
                    } else {
                        // fallo al bajar la imagen la imagen
                        Drawable drawable = getResources().getDrawable(R.drawable.default_recipe_image_foreground);
                        imagenReceta.setImageDrawable(drawable);
                    }
                }
            }
        });
    }
}
