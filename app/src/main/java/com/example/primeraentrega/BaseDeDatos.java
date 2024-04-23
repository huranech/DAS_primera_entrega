package com.example.primeraentrega;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class BaseDeDatos extends SQLiteOpenHelper {

    // atributos
    private static final String DATABASE_NAME = "baseDeDatos";
    private static final int DATABASE_VERSION = 3;

    // patrón singleton (MAE)
    private static BaseDeDatos miBaseDeDatos;
    public static BaseDeDatos getBaseDeDatos(@Nullable Context context) {
        if (miBaseDeDatos == null) {
            miBaseDeDatos = new BaseDeDatos(context);
        }
        return miBaseDeDatos;
    }

    private BaseDeDatos(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // sobreescribir métodos hereditarios
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS recetas (" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "nombre TEXT NOT NULL, " +
                "ingredientes TEXT, " +
                "descripcion TEXT," +
                "vegano INT," +
                "imagen INT" +
                ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        // eliminar tablas desactualizadas
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS recetas");

        // crear tablas de nuevo
        onCreate(sqLiteDatabase);
    }

    // métodos propios

    // añadir una receta
    public void anadirReceta(String nombre, String ingredientes, String descripcion, int vegano, int idImagen) {
        if (!nombre.isEmpty()) {  // Check if name is not empty
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put("nombre", nombre);
            if (!ingredientes.isEmpty()) {
                values.put("ingredientes", ingredientes);
            }
            if (!descripcion.isEmpty()) {
                values.put("descripcion", descripcion);
            }
            values.put("vegano", vegano);
            values.put("imagen", idImagen);
            db.insert("recetas", null, values);
            db.close();
        } else {
            // no se ha añadido ningún nombre
        }
    }

    // eliminar una receta
    public void eliminarReceta(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("recetas", "_id = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // modificar una receta
    public void modificarReceta(int id, String nuevoNombre, String nuevosIngredientes, String nuevaDescripcion, int nuevoVegano, int nuevaImagen) {
        SQLiteDatabase db = this.getWritableDatabase();

        // Crear un objeto ContentValues para contener los nuevos valores
        ContentValues values = new ContentValues();
        values.put("nombre", nuevoNombre);
        if (!nuevosIngredientes.isEmpty()) {
            values.put("ingredientes", nuevosIngredientes);
        }
        if (!nuevaDescripcion.isEmpty()) {
            values.put("descripcion", nuevaDescripcion);
        }
        values.put("vegano", nuevoVegano);
        values.put("imagen", nuevaImagen);

        // Realizar la actualización en la base de datos
        db.update("recetas", values, "_id = ?", new String[]{String.valueOf(id)});

        // Cerrar la conexión a la base de datos
        db.close();
    }

    // obtener todas las recetas
    public Cursor obtenerRecetas() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.rawQuery("SELECT * FROM recetas", null);
    }
}
