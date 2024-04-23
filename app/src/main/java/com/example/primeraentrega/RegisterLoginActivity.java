package com.example.primeraentrega;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class RegisterLoginActivity extends AppCompatActivity {

    private EditText editTextUsernameRegister, editTextPasswordRegister;
    private EditText editTextUsernameLogin, editTextPasswordLogin;
    private Button buttonRegister, buttonLogin;
    private static final String REGISTER_URL = "http://34.65.250.38:8080/register.php";
    private static final String LOGIN_URL = "http://34.65.250.38:8080/login.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_login);

        //
        Intent esteIntent = getIntent();

        // inicializar vistas
        editTextUsernameRegister = findViewById(R.id.editTextUsernameRegister);
        editTextPasswordRegister = findViewById(R.id.editTextPasswordRegister);
        editTextUsernameLogin = findViewById(R.id.editTextUsernameLogin);
        editTextPasswordLogin = findViewById(R.id.editTextPasswordLogin);
        buttonRegister = findViewById(R.id.buttonRegister);
        buttonLogin = findViewById(R.id.buttonLogin);

        // configurar listeners para los botones
        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // obtener datos de los inputs
                String username = editTextUsernameRegister.getText().toString().trim();
                String password = editTextPasswordRegister.getText().toString().trim();

                // registrar usuario
                registerUser(username, password);
            }
        });

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // obtener datos de los inputs
                String username = editTextUsernameLogin.getText().toString().trim();
                String password = editTextPasswordLogin.getText().toString().trim();
                // tratar de iniciar sesión
                loginUser(username, password);
            }
        });
    }

    // volver a MainActivity una vez se consiga registrar/logear
    private void goToMainActivity() {
        Intent intent = new Intent(RegisterLoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    // registrar usuario
    private void registerUser(String username, String password) {
        // preparar datos para el registro
        Data inputData = new Data.Builder()
                .putString("username", username)
                .putString("password", password)
                .putString("key", "register")
                .putString("url", REGISTER_URL)
                .build();

        OneTimeWorkRequest registerRequest = new OneTimeWorkRequest.Builder(ConexionDBWebService.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(registerRequest);

        // observer del resultado de la solicitud
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(registerRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    boolean success = workInfo.getOutputData().getBoolean("success", false);
                    int idLogin = workInfo.getOutputData().getInt("userId", -1);
                    if (success) {
                        // registro exitoso
                        Toast.makeText(RegisterLoginActivity.this, "Usuario registrado", Toast.LENGTH_SHORT).show();
                        goToMainActivity();
                    } else {
                        // registro fallido
                        Toast.makeText(RegisterLoginActivity.this, "Registro fallido: usuario ya existente", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }


    // iniciar sesión
    private void loginUser(String username, String password) {
        // preparar datos para la solicitud
        Data inputData = new Data.Builder()
                .putString("username", username)
                .putString("password", password)
                .putString("key", "login")
                .putString("url", LOGIN_URL)
                .build();

        OneTimeWorkRequest loginRequest = new OneTimeWorkRequest.Builder(ConexionDBWebService.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(this).enqueue(loginRequest);

        // observer del resultado de la solicitud
        WorkManager.getInstance(this).getWorkInfoByIdLiveData(loginRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if (workInfo != null && workInfo.getState() == WorkInfo.State.SUCCEEDED) {
                    boolean success = workInfo.getOutputData().getBoolean("success", false);
                    int idLogin = workInfo.getOutputData().getInt("userId", -1);
                    if (success) {
                        // inicio de sesión exitoso
                        Toast.makeText(RegisterLoginActivity.this, "Inicio de sesión exitoso: " + username, Toast.LENGTH_SHORT).show();
                        //MaePreferenceManager.getMiPreferenceManager(RegisterLoginActivity.this).setLoginId(idLogin);
                        goToMainActivity();
                    } else {
                        // inicio de sesión fallido
                        Toast.makeText(RegisterLoginActivity.this, "Inicio de sesión fallido: credenciales incorrectas", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }
}
