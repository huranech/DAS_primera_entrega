    package com.example.primeraentrega;

    import android.app.NotificationChannel;
    import android.app.NotificationManager;
    import android.app.PendingIntent;
    import android.content.Context;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.database.Cursor;
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
    import androidx.recyclerview.widget.LinearLayoutManager;
    import androidx.recyclerview.widget.RecyclerView;

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

    public class MainActivity extends AppCompatActivity {

        private RecyclerView recyclerView;
        private RecipeAdapter adapter;

        // lista de recetas actualizada
        private List<Recipe> recipeList;

        // id del canal para las notificaciones
        private static final String CHANNEL_ID = "idCanal";

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

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
                            BaseDeDatos.getBaseDeDatos(this).anadirReceta(nombre, ingredientes, descripción, vegano);
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
                        BaseDeDatos.getBaseDeDatos(MainActivity.this).anadirReceta(nombre, "", "", 0);

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
            // obtener el ID de la receta que se eliminará
            int id = recipeList.get(position).getId();

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
            int id, veganoInt;
            String nombre, ingredientes, descripcion;
            while (recetas.moveToNext()) {
                // paso 2.1: obtener los índices de los atributos de cada receta
                int idIndex = recetas.getColumnIndex("_id");
                int nombreIndex = recetas.getColumnIndex("nombre");
                int ingredientesIndex = recetas.getColumnIndex("ingredientes");
                int descripcionIndex = recetas.getColumnIndex("descripcion");
                int veganoIndex = recetas.getColumnIndex("vegano");
                if (idIndex != -1) {
                    // paso 2.2: obtener los atributos de cada receta
                    id = recetas.getInt(idIndex);
                    nombre = recetas.getString(nombreIndex);
                    ingredientes = recetas.getString(ingredientesIndex);
                    descripcion = recetas.getString(descripcionIndex);
                    veganoInt= recetas.getInt(veganoIndex);
                    boolean vegano = veganoInt == 1;
                    // paso 3: crear receta y añadirla a la lista
                    Recipe recipe = new Recipe(id, nombre, ingredientes, descripcion, vegano);
                    recipeList.add(recipe);
                } else {
                        // si no se encontrasen las columnas...
                }
            }

            // paso 4: cerrar base de datos
            recetas.close();

            return recipeList;
        }


        private void lanzarNotificacion(String titulo, String mensaje) {
            // crear el canal de notificación si no existe
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // pedir permiso
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

        @Override
        protected void onResume() {
            super.onResume();
            // actualizar la lista de recetas cada vez que la actividad se reanude
            actualizarListaRecetas();
        }
    }