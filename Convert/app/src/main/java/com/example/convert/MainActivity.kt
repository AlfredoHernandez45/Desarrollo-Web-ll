package com.example.convert

import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
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
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.NotificationManager
import android.app.ProgressDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.metaio.sdk.MetaioDebug
import com.metaio.tools.io.AssetsManager

class MainActivity : Activity() {
    //CompanyName
    val companyName = "ITCH" //NO CAMBIA
    //var companyName = "ITCH"

    //Values that return from PHP request on StatusBusiness are asigned to String's variables.  'mediaLauncher' `mediaToExecute` `xmlTrackingFile`  `marker`
    var mediaLauncher = ""
    var mediaToExecute = ""
    var xmlTrackingFile = ""
    var marker = ""
    var mHandler: Handler? = null

    /**AssetsExtracter mTask;
     * is called when the app starts. Extracts the assets and starts the main ARActivity.
     */
    var mTask: GetDataAsync? = null
    var goGps: GoToGPS? = null
    var alert: AlertMessage? = null
    var modelGps = ""
    var progressDialog: ProgressDialog? = null
    var progreso = 0
    var id = 0
    var downloadedSize = 0
    var totalSize = 0

    //Ruta DOWNLOADS de SD donde lee los archivos DESCARGADOS ya sea objetos, xml, videos
    var externalStorage: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    var download_file_path = "http://advertisingchannel.com.mx/oara/"
    var getAssets: AssetsExtracter? = null
    private var manejador: LocationManager? = null
    private var proveedor: String? = null
    var latitude: Double? = null
    var longitude: Double? = null
    var latiString = ""
    var longString = ""

    //String to compare in Database
    var latiToBD = ""
    var longToBD = ""
    var latitudID = ""
    var longitudID = ""
    @Override
    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)


        //Se eliminan las notificaciones en bandeja
        cancelNotification(0)
        manejador = getSystemService(LOCATION_SERVICE) as LocationManager?
        val criteria = Criteria()
        proveedor = manejador.getBestProvider(criteria, true)
        val localizacion: Location = manejador.getLastKnownLocation(proveedor)
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

        // Toast.makeText(this, latiToBD +" "+ longToBD, 5000).show();


        //Se crea el Servicio para lanzar la clase que se encargar� de estar verificando cuando hay un punto gps nuevo
        startService(Intent(this@MainActivity, ServiceGps::class.java))

        /* Las siguientes lineas es el c�digo para detener el servicio. Hay que ver en que momento se detendr� el servicio. Puede ser en un Settings.
		      -- stopService(new Intent(MainActivity.this,
              --   ServicioMusica.class));   */MetaioDebug.enableLogging(true)

        // extract all the assets
        /**   ASSETS es la carpeta INTERNA que contiene los archivos (marcadores, objetos, xml, tracking files, etc) en dado caso que NO UTILICEMOS
         * GUARDADO de archivos en SD � en SERVIDOR    (Sergio 2014) */
        getAssets = AssetsExtracter()
        getAssets.execute(0)

        //Launch de Asyntask
        mTask = GetDataAsync()
        mTask.execute(0)
        MetaioDebug.log(Log.ERROR, "This is a log")
    }

    private inner class GetDataAsync : AsyncTask<Integer?, Integer?, Boolean?>() {
        @Override
        protected fun doInBackground(vararg params: Integer?): Boolean {
            data
            return true
        }
    }

    private inner class GoToGPS : AsyncTask<Integer?, Integer?, Boolean?>() {
        @Override
        protected fun doInBackground(vararg params: Integer?): Boolean {
            val intent = Intent(getApplicationContext(), GPS::class.java)
            startActivity(intent)
            return true
        }
    }

    private inner class AlertMessage : AsyncTask<Integer?, Integer?, Boolean?>() {
        @Override
        protected fun doInBackground(vararg params: Integer?): Boolean {
            val builder1: AlertDialog.Builder = Builder(getApplicationContext())
            builder1.setMessage("Write your message here.")
            builder1.setCancelable(true)
            builder1.setPositiveButton("Yes",
                object : OnClickListener() {
                    fun onClick(dialog: DialogInterface, id: Int) {
                        dialog.cancel()
                    }
                })
            builder1.setNegativeButton("No",
                object : OnClickListener() {
                    fun onClick(dialog: DialogInterface, id: Int) {
                        dialog.cancel()
                    }
                })
            val alert11: AlertDialog = builder1.create()
            alert11.show()
            return true
        }
    }

    private inner class AssetsExtracter : AsyncTask<Integer?, Integer?, Boolean?>() {
        @Override
        protected fun doInBackground(vararg params: Integer?): Boolean {
            try {
                AssetsManager.extractAllAssets(getApplicationContext(), true)
            } catch (e: IOException) {
                MetaioDebug.printStackTrace(Log.ERROR, e)
                return false
            }
            return true
        }
    }// TODO: handle exception//id=0;//DESPUES LO  LEE DESDE LA SD //Do somehting//Objeto Json

    // jdata = jArray.getJSONObject(0);
// aquí puedes añadir funciones// Toast.makeText(MainActivity.this, "Hello", 10000).show();//alert = new AlertMessage();
    //alert.execute(0);
    //HttpPost httppost = new HttpPost("http://192.168.10.162/oara/PHP/statusBusiness.php"); //YOUR PHP SCRIPT ADDRESS
    //HttpPost httppost = new HttpPost("http://castell.net84.net/statusBusiness.php");
    val data:
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
                    Log.e(
                        "Oara. MainActivity",
                        "No existe registro de esta latitud y longitud en la base de datos"
                    )

                    //alert = new AlertMessage();
                    //alert.execute(0);
                    this.runOnUiThread(object : Runnable() {
                        @SuppressWarnings("deprecation")
                        fun run() {
                            // Toast.makeText(MainActivity.this, "Hello", 10000).show();
                            val alertDialog: AlertDialog = Builder(this@MainActivity).create()
                            alertDialog.setTitle("Oara")
                            alertDialog.setMessage("No hay información con Lat:$latitudID Long:$longitudID")
                            alertDialog.setButton("Aceptar", object : OnClickListener() {
                                fun onClick(dialog: DialogInterface?, which: Int) {
                                    // aquí puedes añadir funciones
                                }
                            })
                            alertDialog.setIcon(R.drawable.icon_alert)
                            alertDialog.show()
                        }
                    })
                } else {
                    Log.e("MainActivity", "Registro Encontrado")
                    //Objeto Json
                    // jdata = jArray.getJSONObject(0);
                    mediaLauncher = jdata.getString("mediaLauncher")
                    mediaToExecute = jdata.getString("mediaToExecute")
                    xmlTrackingFile = jdata.getString("xmlTrackingFile")
                    marker = jdata.getString("marker")
                    latitudID = jdata.getString("latitudID")
                    longitudID = jdata.getString("longitudID")
                    val file2 =
                        File(externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute)
                    if (file2.exists()) {
                        //Do somehting
                        val intent = Intent(getApplicationContext(), ArMotor::class.java)
                        intent.putExtra("mediaLauncher", mediaLauncher)
                        intent.putExtra("mediaToExecute", mediaToExecute)
                        intent.putExtra("xmlTrackingFile", xmlTrackingFile)
                        intent.putExtra("marker", marker)
                        intent.putExtra("latitudID", latitudID)
                        intent.putExtra("longitudID", longitudID)
                        startActivity(intent)
                    } else {
                        //DESPUES LO  LEE DESDE LA SD
                        runOnUiThread(object : Runnable() {
                            @Override
                            fun run() {
                                //id=0;
                                showDialog(id)
                            }
                        })
                        downloadFile(mediaToExecute)
                    }
                    val intent = Intent(getApplicationContext(), ArMotor::class.java)
                    intent.putExtra("mediaLauncher", mediaLauncher)
                    intent.putExtra("mediaToExecute", mediaToExecute)
                    intent.putExtra("xmlTrackingFile", xmlTrackingFile)
                    intent.putExtra("marker", marker)
                    intent.putExtra("latitudID", latitudID)
                    intent.putExtra("longitudID", longitudID)
                    startActivity(intent)
                }
            } catch (e: Exception) {
                // TODO: handle exception
                Log.e("log_tag_MAIN_ACTIVITY", "Error Parsing Data " + e.toString())
            }
        }

    //Metodo para Descargar un archivo de Servidor
    fun downloadFile(objectNameSaved: String): Boolean {
        try {
            val url = URL(download_file_path + objectNameSaved)
            val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
            urlConnection.setRequestMethod("GET")
            urlConnection.setDoOutput(true)

            //<span id="IL_AD3" class="IL_AD">connect</span>
            urlConnection.connect()

            //set the path where we want to <span id="IL_AD8" class="IL_AD">save</span> the file
            val SDCardRoot: File =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            //create a new file, to save the downloaded file
            val file = File(SDCardRoot, objectNameSaved)
            val fileOutput = FileOutputStream(file)

            //Stream used for reading the data from the internet
            val inputStream: InputStream = urlConnection.getInputStream()

            //this is the total size of the file which we are downloading
            totalSize = urlConnection.getContentLength()


            //create a buffer...
            val buffer = ByteArray(1024)
            var bufferLength = 0
            while (inputStream.read(buffer).also { bufferLength = it } > 0) {
                fileOutput.write(buffer, 0, bufferLength)
                downloadedSize += bufferLength
                //publishing the progress
                progreso = (downloadedSize * 100 / totalSize)
                onProgressUpdate()
            }
            //close the output stream when complete //
            fileOutput.close()
        } catch (e: MalformedURLException) {
            showError("Error : MalformedURLException $e")
            e.printStackTrace()
        } catch (e: IOException) {
            showError("Error : IOException $e")
            e.printStackTrace()
        } catch (e: Exception) {
            showError("Error : Please check your internet connection $e")
        }
        return true
    }

    fun showError(err: String?) {}
    protected fun onCreateDialog(id: Int): Dialog? {
        progressDialog = ProgressDialog(this)
        if (id == 0) {
            progressDialog.setProgressStyle(
                ProgressDialog.STYLE_HORIZONTAL
            )
            progressDialog.setIcon(R.drawable.itche)
            progressDialog.setTitle("Actualizando Multimedia . . . ")
            progressDialog.setMessage("Espere un momento…")
            progressDialog.setIndeterminate(false)
            progressDialog.setMax(100)
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.setCancelable(true)
        } else if (id == 1) {
            progressDialog.setProgressStyle(
                ProgressDialog.STYLE_SPINNER
            )
            progressDialog.setIcon(R.drawable.itche)
            progressDialog.setTitle("Descargando . . . ")
        }
        return progressDialog
    }

    protected fun onProgressUpdate(vararg progress: Void?) {
        progressDialog.setProgress(progreso)
        if (progreso == 100) removeDialog(id)
        //se puede usar lo siguiente para ocultar el diálogo
        // como alternativa:
        // if(progreso==100)progressDialog . hide();
    }

    // Metodos para mostrar informacion
    private fun log(cadena: String) {
        Toast.makeText(
            this, """
     $cadena
     
     """.trimIndent(), Toast.LENGTH_LONG
        ).show()
    }

    private fun muestraLocaliz(localizacion: Location?) {
        if (localizacion == null) log("Localizaci�n desconocida\n") else log(
            localizacion.toString().toString() + "\n"
        )
    }

    fun cancelNotification(notificationId: Int) {
        if (Context.NOTIFICATION_SERVICE != null) {
            val ns: String = Context.NOTIFICATION_SERVICE
            val nMgr: NotificationManager =
                getApplicationContext().getSystemService(ns) as NotificationManager
            nMgr.cancel(notificationId)
        }
    }
}