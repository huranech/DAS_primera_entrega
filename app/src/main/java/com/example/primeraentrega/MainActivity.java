    package com.example.primeraentrega;

    import android.app.NotificationChannel;
    import android.app.NotificationManager;
    import android.app.PendingIntent;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.database.Cursor;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.drawable.Drawable;
    import android.os.Build;
    import android.os.Bundle;

    import androidx.annotation.NonNull;
    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;
    import androidx.appcompat.widget.Toolbar;
    import androidx.core.app.ActivityCompat;
    import androidx.core.app.NotificationCompat;
    import androidx.core.app.NotificationManagerCompat;
    import androidx.core.content.ContextCompat;
    import androidx.lifecycle.LifecycleOwner;
    import androidx.lifecycle.Observer;
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;
    import androidx.work.Data;
    import androidx.work.OneTimeWorkRequest;
    import androidx.work.WorkInfo;
    import androidx.work.WorkManager;

    import android.util.Base64;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.Menu;
    import android.view.MenuItem;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.Toast;

    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.InputStreamReader;
    import java.util.ArrayList;
    import java.util.Arrays;
    import java.util.List;
    import android.Manifest;

    import com.google.android.gms.tasks.OnCompleteListener;
    import com.google.android.gms.tasks.Task;
    import com.google.firebase.messaging.FirebaseMessaging;

    public class MainActivity extends AppCompatActivity {

        private String tokenDispositivo;
        private RecyclerView recyclerView;
        private RecipeAdapter adapter;

        // lista de recetas actualizada
        private List<Recipe> recipeList;
        private String URL_ELIMINAR = "http://34.65.250.38:8080/eliminar_imagen.php";
        private String URL_TOKEN = "http://34.65.250.38:8080/subir_token.php";

        // id del canal para las notificaciones
        private static final String CHANNEL_ID = "idCanal";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            FirebaseMessaging.getInstance().getToken()
                    .addOnCompleteListener(new OnCompleteListener<String>() {
                        @Override
                        public void onComplete(@NonNull Task<String> task) {
                            if (task.isSuccessful()) {
                                tokenDispositivo = task.getResult();
                                // subir el token
                                Data inputData = new Data.Builder()
                                        .putString("token", tokenDispositivo)
                                        .putString("key", "subir_token")
                                        .putString("url", URL_TOKEN)
                                        .putString("nombre", "")
                                        .putString("ingredientes", "")
                                        .putString("descripcion", "")
                                        .build();

                                OneTimeWorkRequest tokenRequest = new OneTimeWorkRequest.Builder(TokenManager.class)
                                        .setInputData(inputData)
                                        .build();

                                WorkManager.getInstance(MainActivity.this).enqueue(tokenRequest);

                                // observer del resultado de la solicitud
                                WorkManager.getInstance(MainActivity.this).getWorkInfoByIdLiveData(tokenRequest.getId()).observe(MainActivity.this, new Observer<WorkInfo>() {
                                    @Override
                                    public void onChanged(WorkInfo workInfo) {
                                        if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                                            boolean success = workInfo.getOutputData().getBoolean("success", false);
                                            if (success) {
                                                // éxito al insertar token
                                            } else {
                                                // fracaso al insertar token
                                            }
                                        }
                                    }
                                });
                            } else {
                                Exception exception = task.getException();
                                if (exception != null) {
                                    Log.e("TOKEN_ERROR", "Error al obtener el token: " + exception.getMessage());
                                }
                            }
                        }
                    });

            // logica de register/login
            int loginId = MaePreferenceManager.getMiPreferenceManager(this).getLoginId();
            if (loginId == -1) {
                Intent intent = new Intent(MainActivity.this, RegisterLoginActivity.class);
                intent.putExtra("token_dispositivo", tokenDispositivo);
                startActivity(intent);
            }

            // pedir permisos de notificaciones
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
            }

            // obtener preferencias
            MaePreferenceManager preferenceManager = MaePreferenceManager.getMiPreferenceManager(this);
            preferenceManager.applyLanguage(this);
            preferenceManager.applyTheme(this);

            // inflar vistas
            setContentView(R.layout.activity_main);

            // configurar actionbar
            Toolbar toolbar = findViewById(R.id.actionbar);
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayShowTitleEnabled(false);

            // inicializar la lista de recetas y el adaptador
            this.recipeList = getRecipeList();
            if(recipeList.size() == 0) {
                // si no hay recetas, se generan recetas por defecto a partir de unos ficheros predefinidos
                InputStream fichero = getResources().openRawResource(R.raw.recetas_por_defecto);
                BufferedReader buff = new BufferedReader(new InputStreamReader(fichero));
                try {
                    // leer línea por línea del fichero
                    String linea;
                    while ((linea = buff.readLine()) != null) {
                        // obtener la información de cada lína según el formato del fichero
                        String[] partes = linea.split(";");
                        String idioma = partes[0];
                        String nombre = partes[1];
                        String ingredientes = partes[2];
                        String descripción = partes[3];
                        int vegano = Integer.parseInt(partes[4]);

                        // crear las recetas en función del idioma
                        if (MaePreferenceManager.getMiPreferenceManager(this).getLanguage().equals(idioma)) {
                            BaseDeDatos.getBaseDeDatos(this).anadirReceta(nombre, ingredientes, descripción, vegano, 0);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            // actualizar cambios correctamente para que se visualicen las recetas por defecto
            adapter = new RecipeAdapter(this.recipeList);
            actualizarListaRecetas();

            // configurar RecyclerView
            recyclerView = findViewById(R.id.recyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            recyclerView.setAdapter(adapter);

            // establecer el listener de eliminación después de crear el adaptador
            adapter.setOnDeleteClickListener(new RecipeAdapter.OnDeleteClickListener() {
                @Override
                public void onDeleteClick(int position) {
                    onDeleteRecipe(position);
                }
            });

            // establecer listener para el botón de añadir receta
            Button addButton = findViewById(R.id.addNewRecipeButton);
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mostrarDialogoAgregarReceta();
                }
            });

            // establecer listener para los botones de las recetas
            adapter.setOnRecipeClickListener(new RecipeAdapter.OnRecipeClickListener() {
                @Override
                public void onRecipeClick(int position) {
                    Recipe selectedRecipe = recipeList.get(position);
                    // iniciar la actividad de edición de recetas y pasar la información de la receta seleccionada
                    Intent intent = new Intent(MainActivity.this, EditRecipeActivity.class);
                    intent.putExtra("_id", selectedRecipe.getId());
                    intent.putExtra("nombre", selectedRecipe.getNombre());
                    intent.putExtra("ingredientes", selectedRecipe.getIngredientes());
                    intent.putExtra("descripcion", selectedRecipe.getDescripcion());
                    intent.putExtra("vegano", selectedRecipe.getVegano());
                    intent.putExtra("imagen", selectedRecipe.getIdImagen());
                    startActivity(intent);
                }
            });
        }

        // diálogo para agregar una nueva receta
        private void mostrarDialogoAgregarReceta() {
            // builder para el diálogo
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            // inflar vista
            LayoutInflater inflater = getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.dialog_agregar_receta, null);
            builder.setView(dialogView);

            final EditText nombreEditText = dialogView.findViewById(R.id.nombreEditText);

            builder.setPositiveButton(R.string.boton_agregar, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String nombre = nombreEditText.getText().toString();

                    // verificar si el nombre no está vacío
                    if (!nombre.isEmpty()) {
                        // agregar la receta a la base de datos
                        BaseDeDatos.getBaseDeDatos(MainActivity.this).anadirReceta(nombre, "", "", 0, 0);

                        // lanzar notificación de que se ha creado una nueva receta
                        String titulo = getResources().getString(R.string.receta_creada);
                        String descripcion = getResources().getString(R.string.receta_creada_descripcion) + ": " + nombre;
                        lanzarNotificacion(titulo, descripcion);

                        // actualizar la lista de recetas en el RecyclerView
                        actualizarListaRecetas();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.rogar_ingresar_nombre, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            builder.setNegativeButton(R.string.boton_cancelar, null);

            builder.create().show();
        }


        // método para actualizar la lista de recetas
        private void actualizarListaRecetas() {
            // limpiar la lista actual de recetas
            recipeList.clear();

            // volver a obtener la lista de recetas de la base de datos
            recipeList.addAll(getRecipeList());

            // notificar al adaptador sobre el cambio en los datos
            adapter.notifyDataSetChanged();
        }


        // método para eliminar receta
        private void onDeleteRecipe(int position) {
            // obtener el ID de la receta y la imagen que se eliminará
            int id = recipeList.get(position).getId();
            int idImagen = recipeList.get(position).getIdImagen();

            // eliminar imagen del servidor
            if(idImagen != 0) {
                Data inputData = new Data.Builder()
                        .putInt("id_imagen", idImagen)
                        .putString("key", "eliminar_imagen")
                        .putString("url", URL_ELIMINAR)
                        .build();

                OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ImageManager.class)
                        .setInputData(inputData)
                        .build();

                WorkManager.getInstance(this).enqueue(registerRequest);

                // observer del resultado de la solicitud
                WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId()).observe((LifecycleOwner) this, new Observer<WorkInfo>() {
                    @Override
                    public void onChanged(WorkInfo workInfo) {
                        if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                            boolean success = workInfo.getOutputData().getBoolean("success", false);
                            if (success) {
                                // imagen eliminada correctamente
                            } else {
                                // fallo al eliminar la imagen la imagen
                            }
                        }
                    }
                });
            }

            // eliminar la receta de la base de datos
            BaseDeDatos.getBaseDeDatos(this).eliminarReceta(id);

            // eliminar la receta de la lista de recetas
            recipeList.remove(position);

            // notificar al adaptador sobre el cambio
            adapter.notifyDataSetChanged();
        }

        // método para extraer las recetas de la BBDD y guardarlas en la lista de recetas
        private List<Recipe> getRecipeList() {
            List<Recipe> recipeList = new ArrayList<>();
            // paso 1: obtener las recetas de la base de datos
            Cursor recetas = BaseDeDatos.getBaseDeDatos(this).obtenerRecetas();

            // paso 2: iterar sobre las recetas
            int id, veganoInt, idImagen;
            String nombre, ingredientes, descripcion;
            while (recetas.moveToNext()) {
                // paso 2.1: obtener los índices de los atributos de cada receta
                int idIndex = recetas.getColumnIndex("_id");
                int nombreIndex = recetas.getColumnIndex("nombre");
                int ingredientesIndex = recetas.getColumnIndex("ingredientes");
                int descripcionIndex = recetas.getColumnIndex("descripcion");
                int veganoIndex = recetas.getColumnIndex("vegano");
                int imagenIndex = recetas.getColumnIndex("imagen");
                if (idIndex != -1) {
                    // paso 2.2: obtener los atributos de cada receta
                    id = recetas.getInt(idIndex);
                    nombre = recetas.getString(nombreIndex);
                    ingredientes = recetas.getString(ingredientesIndex);
                    descripcion = recetas.getString(descripcionIndex);
                    veganoInt = recetas.getInt(veganoIndex);
                    idImagen = recetas.getInt(imagenIndex);
                    boolean vegano = veganoInt == 1;
                    // paso 3: crear receta y añadirla a la lista
                    Recipe recipe = new Recipe(id, nombre, ingredientes, descripcion, vegano, idImagen);
                    recipeList.add(recipe);
                } else {
                        // si no se encontrasen las columnas...
                }
            }

            // paso 4: cerrar base de datos
            recetas.close();

            return recipeList;
        }


        // lanzar notificaciones (cuando se crea una receta)
        private void lanzarNotificacion(String titulo, String mensaje) {
            // pedir permisos
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 11);
            }

            // crear manager y builder
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

            // crear canal
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel canal = new NotificationChannel(CHANNEL_ID, "NombreCanal", NotificationManager.IMPORTANCE_DEFAULT);
                manager.createNotificationChannel(canal);
            }

            // configurar builder
            builder.setSmallIcon(R.drawable.default_recipe_image_foreground)
                    .setContentTitle(titulo)
                    .setContentText(mensaje)
                    .setAutoCancel(true);

            // intent para volver a main
            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("id", 1);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingI = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_MUTABLE);

            // lanzar notificación
            manager.notify(1, builder.build());
        }




        // métodos para configurar el action bar
        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.toolbar_menu, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();

            // para ir a recetas
            if(id == R.id.m_recetas) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
                return true;
            }

            // para ir a ajustes
            else if(id == R.id.m_ajustes) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }

            return super.onOptionsItemSelected(item);
        }

        // lógica para reanudar la actividad
        @Override
        protected void onResume() {
            super.onResume();
            // actualizar la lista de recetas cada vez que la actividad se reanude
            actualizarListaRecetas();
        }
    }
