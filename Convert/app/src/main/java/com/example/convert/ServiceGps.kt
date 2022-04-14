package com.example.convert

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.ArrayList
import java.util.List
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.client.HttpClient
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.json.JSONArray
import org.json.JSONObject
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Toast

class ServiceGps : Service(), LocationListener {
    var reproductor: MediaPlayer? = null
    private var manejador: LocationManager? = null
    private var proveedor: String? = null
    var criteria: Criteria? = null
    var localizacion: Location? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var latiString = ""
    var longString = ""

    //String to compare in Database
    var latiToBD = ""
    var longToBD = ""
    var mTask: GetDataAsync? = null
    var companyName = ""
    @Override
    fun onBind(intent: Intent?): IBinder? {
        // TODO Auto-generated method stub
        return null
    }

    @Override
    fun onCreate() {
        //Toast.makeText(this,"Servicio creado", Toast.LENGTH_SHORT).show();
        reproductor = MediaPlayer.create(this, R.raw.audio)
        manejador = getSystemService(LOCATION_SERVICE) as LocationManager?
        val criteria = Criteria()
        proveedor = manejador.getBestProvider(criteria, true)
        localizacion = manejador.getLastKnownLocation(proveedor)
        manejador.requestLocationUpdates(proveedor, 1000, 1, this)
    }

    @Override
    fun onStartCommand(intenc: Intent?, flags: Int, idArranque: Int): Int {
        //Toast.makeText(this,"Servicio arrancado "+ idArranque,Toast.LENGTH_SHORT).show();
        //reproductor.start();
        return START_STICKY
    }

    @Override
    fun onDestroy() {
        Toast.makeText(
            this, "Servicio detenido",
            Toast.LENGTH_SHORT
        ).show()
        reproductor.stop()
    }

    fun showNotification(companyName: String) {

        // define sound URI, the sound to be played when there's a notification
        val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // intent triggered, you can add other intent for other actions
        val intent = Intent(this@ServiceGps, MainActivity::class.java)
        val pIntent: PendingIntent = PendingIntent.getActivity(this@ServiceGps, 0, intent, 0)

        // this is it, we'll build the notification!
        // in the addAction method, if you don't want any icon, just set the first param to 0
        val mNotification: Notification = Builder(this)
            .setContentTitle("Punto Gps: $companyName")
            .setContentText("lat: " + latiToBD + "long: " + longToBD)
            .setSmallIcon(R.drawable.ic_orange)
            .setContentIntent(pIntent)
            .setSound(soundUri)
            .addAction(R.drawable.ic_blue, "View", pIntent)
            .addAction(0, "Remind", pIntent)
            .build()
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // If you want to hide the notification after it was selected, do the code below
        // myNotification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(0, mNotification)
    }

    fun cancelNotification(notificationId: Int) {
        if (Context.NOTIFICATION_SERVICE != null) {
            val ns: String = Context.NOTIFICATION_SERVICE
            val nMgr: NotificationManager =
                getApplicationContext().getSystemService(ns) as NotificationManager
            nMgr.cancel(notificationId)
        }
    }

    fun onLocationChanged(location: Location?) {
        // TODO Auto-generated method stub
        //reproductor.start();
        localizacion = manejador.getLastKnownLocation(proveedor)
        latitude = localizacion.getLatitude()
        longitude = localizacion.getLongitude()
        latiString = latitude.toString()
        longString = longitude.toString()
        latiToBD = if (latiString.charAt(0) === '-') {
            "" + latiString.charAt(0) + latiString.charAt(1) + latiString.charAt(2) + latiString.charAt(
                3
            ) + latiString.charAt(4) + latiString.charAt(5) +
                    latiString.charAt(6) + latiString.charAt(7)
        } else "" + latiString.charAt(0) + latiString.charAt(1) + latiString.charAt(2) + latiString.charAt(
            3
        ) + latiString.charAt(4) + latiString.charAt(5) +
                latiString.charAt(6)
        /**------Longitud to String database  */
        longToBD = if (longString.charAt(0) === '-') {
            "" + longString.charAt(0) + longString.charAt(1) + longString.charAt(2) + longString.charAt(
                3
            ) + longString.charAt(4) + longString.charAt(5) +
                    longString.charAt(6) + longString.charAt(7)
        } else "" + longString.charAt(0) + longString.charAt(1) + longString.charAt(2) + longString.charAt(
            3
        ) + longString.charAt(4) + longString.charAt(5) +
                longString.charAt(6)


        //Launch de Asyntask 
        mTask = GetDataAsync()
        mTask.execute(0)
    }

    @Override
    fun onProviderDisabled(arg0: String?) {
        // TODO Auto-generated method stub
    }

    @Override
    fun onProviderEnabled(arg0: String?) {
        // TODO Auto-generated method stub
    }

    @Override
    fun onStatusChanged(arg0: String?, arg1: Int, arg2: Bundle?) {
        // TODO Auto-generated method stub
    }

    private inner class GetDataAsync : AsyncTask<Integer?, Integer?, Boolean?>() {
        @Override
        protected fun doInBackground(vararg params: Integer?): Boolean {
            dataNewPoint
            return true
        }
    }// TODO: handle exception// Toast.makeText(ServiceGps.this, "No existe datos con esa lat y long", 10000).show();


    //Toast.makeText(this, "No existe registro", Toast.LENGTH_LONG).show();
    //HttpPost httppost = new HttpPost("http://192.168.10.162/oara/PHP/statusBusiness.php"); //YOUR PHP SCRIPT ADDRESS 
    //HttpPost httppost = new HttpPost("http://castell.net84.net/statusBusiness.php");
    val dataNewPoint:
            //A�adir Parametros, en este caso el numero de Control
            //params.add(new BasicNameValuePair("companyName", companyName));
            //convert response to string

            //parse json data
            Unit
        get() {
            var result = ""
            var isr: InputStream? = null
            try {
                val httpclient: HttpClient = DefaultHttpClient()
                //HttpPost httppost = new HttpPost("http://192.168.10.162/oara/PHP/statusBusiness.php"); //YOUR PHP SCRIPT ADDRESS 
                //HttpPost httppost = new HttpPost("http://castell.net84.net/statusBusiness.php");
                val httppost = HttpPost("http://advertisingchannel.com.mx/oara/statusBusiness.php")
                //A�adir Parametros, en este caso el numero de Control
                val params: List<NameValuePair> = ArrayList<NameValuePair>()
                //params.add(new BasicNameValuePair("companyName", companyName));
                params.add(BasicNameValuePair("latitudID", latiToBD))
                params.add(BasicNameValuePair("longitudID", longToBD))
                httppost.setEntity(UrlEncodedFormEntity(params))
                val response: HttpResponse = httpclient.execute(httppost)
                val entity: HttpEntity = response.getEntity()
                isr = entity.getContent()
            } catch (e: Exception) {
                Log.e("log_tag", "Error in http connection " + e.toString())
            }
            //convert response to string
            try {
                val reader = BufferedReader(InputStreamReader(isr, "iso-8859-1"), 8)
                val sb = StringBuilder()
                var line: String? = null
                while (reader.readLine().also { line = it } != null) {
                    sb.append(
                        """
    ${line.toString()}
    
    """.trimIndent()
                    )
                }
                isr.close()
                result = sb.toString()
            } catch (e: Exception) {
                Log.e("log_tag", "Error  converting result " + e.toString())
            }

            //parse json data
            try {
                val jArray = JSONArray(result)
                val jdata: JSONObject = jArray.getJSONObject(0)
                val comparingEmpty: String = jdata.getString("mediaLauncher")
                if (comparingEmpty.equalsIgnoreCase("null")) {


                    // Toast.makeText(ServiceGps.this, "No existe datos con esa lat y long", 10000).show();


                    //Toast.makeText(this, "No existe registro", Toast.LENGTH_LONG).show();
                    Log.e(
                        "Oara. ServiceGPS",
                        "No existe registro de esta latitud y longitud en la base de datos"
                    )
                } else {
                    Log.e("ServiceGPS", "Registro Encontrado")
                    companyName = jdata.getString("companyName")
                    showNotification(companyName)
                }
            } catch (e: Exception) {
                // TODO: handle exception
                Log.e("log_tag_SERVICE_GPS", "Error Parsing Data " + e.toString())
            }
        }
}