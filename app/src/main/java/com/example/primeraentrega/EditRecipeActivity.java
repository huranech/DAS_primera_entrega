package com.example.primeraentrega;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class EditRecipeActivity extends AppCompatActivity {

    private int id;
    private EditText nombreEditText;
    private EditText ingredientesEditText;
    private EditText descripcionEditText;
    private CheckBox veganoCheckBox; // Cambiado de EditText a CheckBox

    private Button guardarButton;

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
        veganoCheckBox = findViewById(R.id.veganoCheckBox); // Cambiado de EditText a CheckBox
        guardarButton = findViewById(R.id.guardarButton);

        // obtener datos de la receta pasados desde MainActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            id = extras.getInt("_id");
            String nombreReceta = extras.getString("nombre");
            String ingredientesReceta = extras.getString("ingredientes");
            String descripcionReceta = extras.getString("descripcion");
            boolean veganoReceta = extras.getBoolean("vegano");

            // mostrar los datos de la receta en los EditText
            nombreEditText.setText(nombreReceta);
            ingredientesEditText.setText(ingredientesReceta);
            descripcionEditText.setText(descripcionReceta);
            veganoCheckBox.setChecked(veganoReceta);
        }

        // configurar el listener del botón de guardar
        guardarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarCambios();
            }
        });
    }

    private void guardarCambios() {
        // obtener los nuevos valores del EditText
        String nuevoNombre = nombreEditText.getText().toString();
        String nuevosIngredientes = ingredientesEditText.getText().toString();
        String nuevaDescripcion = descripcionEditText.getText().toString();
        boolean nuevoVegano = veganoCheckBox.isChecked();

        // guardar las modificaciones en la receta
        BaseDeDatos.getBaseDeDatos(this).modificarReceta(id, nuevoNombre, nuevosIngredientes, nuevaDescripcion, nuevoVegano ? 1 : 0);

        // finalizar actividad (EditRecipeActivity -> MainActivity)
        finish();
    }
}
