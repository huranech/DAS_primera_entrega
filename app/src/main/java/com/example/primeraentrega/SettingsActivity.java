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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.example.primeraentrega.MaePreferenceManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private Button spanishButton;
    private Button basqueButton;
    private Button whiteButton;
    private Button lightBlueButton;
    private Button logoutButton;
    private Button deleteAccountButton;

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
        logoutButton = findViewById(R.id.logoutButton);
        deleteAccountButton = findViewById(R.id.deleteAccountButton);

        // establecer listeners para los botones
        spanishButton.setOnClickListener(spanishButtonListener);
        basqueButton.setOnClickListener(basqueButtonListener);
        whiteButton.setOnClickListener(whiteButtonListener);
        lightBlueButton.setOnClickListener(lightBlueButtonListener);
        logoutButton.setOnClickListener(logoutButtonListener);
        deleteAccountButton.setOnClickListener(deleteAccountButtonListener);
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


    // cerrar sesión
    private final View.OnClickListener logoutButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // reiniciar id de usuario
            MaePreferenceManager.getMiPreferenceManager(getApplicationContext()).setLoginId(-1);
            // redirigir al usuario a la pantalla de inicio de sesión
            startActivity(new Intent(SettingsActivity.this, RegisterLoginActivity.class));
            finish(); // finalizar esta actividad después de cerrar sesión
        }
    };

    // eliminar cuenta
    private final View.OnClickListener deleteAccountButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int userId = MaePreferenceManager.getMiPreferenceManager(getApplicationContext()).getLoginId();
            deleteUser(userId);
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

    public void deleteUser(int userId) {
        // eliminar usuario de la base de datos
        String delete_url = "http://34.65.250.38:8080/eliminar_usuario.php";

        // preparar datos para la solicitud
        Data inputData = new Data.Builder()
                .putString("username", "")
                .putString("password", "")
                .putInt("userId", userId)
                .putString("key", "delete")
                .putString("url", delete_url)
                .build();

        OneTimeWorkRequest deleteRequest = new OneTimeWorkRequest.Builder(ConexionDBWebService.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(deleteRequest);

        // observer del resultado de la solicitud
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(deleteRequest.getId()).observe(this, new Observer<WorkInfo>() {


            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    boolean success = workInfo.getOutputData().getBoolean("success", false);
                    if (success) {
                        // usuario eliminado con éxito
                        Toast.makeText(SettingsActivity.this, "Se ha eliminado la cuenta", Toast.LENGTH_SHORT).show();
                        // reiniciar id de usuario
                        MaePreferenceManager.getMiPreferenceManager(getApplicationContext()).setLoginId(-1);
                        // redirigir al usuario a la pantalla de inicio de sesión
                        startActivity(new Intent(SettingsActivity.this, RegisterLoginActivity.class));
                        finish(); // finalizar esta actividad después de eliminar la cuenta
                    } else {
                        // algo a fallado al intentar eliminar la cuenta
                        Toast.makeText(SettingsActivity.this, "Algo ha fallado al eliminar la cuenta", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
