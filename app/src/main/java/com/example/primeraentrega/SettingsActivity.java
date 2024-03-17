package com.example.primeraentrega;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.example.primeraentrega.MaePreferenceManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Button spanishButton;
    private Button basqueButton;
    private Button whiteButton;
    private Button lightBlueButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // obtener preferencias
        MaePreferenceManager preferenceManager = MaePreferenceManager.getMiPreferenceManager(this);
        preferenceManager.applyLanguage(this);
        preferenceManager.applyTheme(this);

        // inflar vistas
        setContentView(R.layout.activity_settings);

        // configurar actionbar
        Toolbar toolbar = findViewById(R.id.actionbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // inicializar botones
        spanishButton = findViewById(R.id.spanishButton);
        basqueButton = findViewById(R.id.basqueButton);
        whiteButton = findViewById(R.id.whiteButton);
        lightBlueButton = findViewById(R.id.lightBlueButton);

        // establecer listeners para los botones
        spanishButton.setOnClickListener(spanishButtonListener);
        basqueButton.setOnClickListener(basqueButtonListener);
        whiteButton.setOnClickListener(whiteButtonListener);
        lightBlueButton.setOnClickListener(lightBlueButtonListener);
    }

    // cambiar idioma a castellano
    private final View.OnClickListener spanishButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            changeLanguage("es");
        }
    };

    // cambiar idioma a euskera
    private final View.OnClickListener basqueButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            changeLanguage("eu");
        }
    };

    // cambiar tema a blanco
    private final View.OnClickListener whiteButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            applyTheme(R.style.AppTheme);
        }
    };

    // cambiar tema a azul claro
    private final View.OnClickListener lightBlueButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v)
        {
            applyTheme(R.style.AppTheme_Blue);
        }
    };

    // método auxiliar para realizar el cambio de idioma
    private void changeLanguage(String language) {
        MaePreferenceManager.getMiPreferenceManager(this).setLanguage(language);
        finish();
        startActivity(getIntent());
    }

    // método auxiliar para aplicar el tema
    private void applyTheme(int themeId) {
        MaePreferenceManager.getMiPreferenceManager(this).setTheme(themeId);
        finish();
        startActivity(getIntent());
    }

    // métodos para mostrar barra de acciones
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        // ir a recetas
        if(id == R.id.m_recetas) {
            startActivity(new Intent(SettingsActivity.this, MainActivity.class));
            return true;
        }

        // ir a ajustes
        else if(id == R.id.m_ajustes) {
            startActivity(new Intent(SettingsActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
