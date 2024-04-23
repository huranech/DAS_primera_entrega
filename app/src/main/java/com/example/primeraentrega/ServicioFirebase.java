package com.example.primeraentrega;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ServicioFirebase extends FirebaseMessagingService {

    // constructora
    public ServicioFirebase() {

    }

    // qué hacer al recibir un mensaje
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (remoteMessage.getData().size() > 0) {
            String cuerpo = remoteMessage.getNotification().getBody();
            lanzarNotificacion(remoteMessage.getNotification().getTitle(), cuerpo);
        }
    }

    // lanzar una notificación
    private void lanzarNotificacion(String titulo, String cuerpo) {
        // crear manager y builder
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "id_canal");

        // crear canal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel("id_canal", "NombreCanal", NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(canal);
        }

        // configurar builder
        builder.setSmallIcon(R.drawable.default_recipe_image_foreground)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true);

        // intent para volver a main
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra("id", 1);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingI = PendingIntent.getActivity(this, 0, i, PendingIntent.FLAG_MUTABLE);

        // lanzar notificación
        manager.notify(1, builder.build());
    }

    // cada vez que se genere un nuevo token para el dispositivo
    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);

    }
}


