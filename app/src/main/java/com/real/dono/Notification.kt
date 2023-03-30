package com.real.dono

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

const val notificationID = 1
const val channelID = "channel1"
const val titleExtra = "titleExtra"
const val messageExtra = "messageExtra"

class Notification : BroadcastReceiver() {
    var counter = 0
    val user = FirebaseAuth.getInstance().currentUser
    val email = user?.email
    override fun onReceive(context: Context, intent: Intent) {
        val id = System.currentTimeMillis().toString() + (0..1000).random()
        counter++

        val notificationBuilder = NotificationCompat.Builder(context, channelID)


        val mainIntent = Intent(context,MainActivity::class.java)
        val pendingMainIntent = if(Build.VERSION.SDK_INT >=23){
            PendingIntent.getActivity(context,
                0 ,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }else{
            PendingIntent.getActivity(context,
                0 ,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelID,
                "1",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)

            notificationBuilder.setSmallIcon(R.drawable.resource_do)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingMainIntent)
                .setColor(ContextCompat.getColor(context, R.color.savebtnbg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        }else{
            notificationBuilder.setSmallIcon(R.drawable.resource_do)
                .setContentText("$email")
                .setContentTitle(intent.getStringExtra(messageExtra))
                .setContentIntent(pendingMainIntent)
                .setColor(ContextCompat.getColor(context, R.color.savebtnbg))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        }



        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationID)
        manager.notify(id.hashCode(), notificationBuilder.build())
    }
}

