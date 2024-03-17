package com.example.primeraentrega;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.preference.PreferenceManager;


import androidx.annotation.Nullable;


import java.util.Locale;

public class MaePreferenceManager {

    // atributos
    private static MaePreferenceManager miPreferenceManager;
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_THEME = "theme";
    private SharedPreferences prefs;

    // patrón singleton (MAE)
    public static MaePreferenceManager getMiPreferenceManager(@Nullable Context context) {
        if (miPreferenceManager == null) {
            miPreferenceManager = new MaePreferenceManager(context);
        }
        return miPreferenceManager;
    }
    private MaePreferenceManager(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // métodos propios

    // establecer idioma
    public void setLanguage(String language) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LANGUAGE, language);
        editor.apply();
    }

    // obtener idioma actual
    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "");
    }

    // aplicar cambios del idioma
    public void applyLanguage(Context context) {
        String language = getLanguage();
        if (!language.isEmpty()) {
            Locale locale = new Locale(language);
            Locale.setDefault(locale);

            Configuration configuration = context.getResources().getConfiguration();
            configuration.setLocale(locale);

            context.getResources().updateConfiguration(configuration, context.getResources().getDisplayMetrics());
        }
    }

    // establecer tema
    public void setTheme(int themeId) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_THEME, themeId);
        editor.apply();
    }

    // obtener tema actual
    public int getTheme() {
        return prefs.getInt(KEY_THEME, -1);
    }

    // aplicar tema
    public void applyTheme(Context context) {
        int themeId = getTheme();
        if (themeId != -1) {
            context.setTheme(themeId);
        }
    }
}
