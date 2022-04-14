package com.example.convert

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.ZoomControls
import com.metaio.sdk.ARViewActivity
import com.metaio.sdk.MetaioDebug
import com.metaio.sdk.jni.AnnotatedGeometriesGroupCallback
import com.metaio.sdk.jni.EGEOMETRY_FOCUS_STATE
import com.metaio.sdk.jni.EPLAYBACK_STATUS
import com.metaio.sdk.jni.IAnnotatedGeometriesGroup
import com.metaio.sdk.jni.IGeometry
import com.metaio.sdk.jni.IMetaioSDKCallback
import com.metaio.sdk.jni.IRadar
import com.metaio.sdk.jni.LLACoordinate
import com.metaio.sdk.jni.MovieTextureStatus
import com.metaio.sdk.jni.Rotation
import com.metaio.sdk.jni.SensorValues
import com.metaio.sdk.jni.TrackingValuesVector
import com.metaio.sdk.jni.Vector3d
import com.metaio.tools.SystemInfo
import com.metaio.tools.io.AssetsManager

class ArMotor : ARViewActivity() {
    private var mAnnotatedGeometriesGroup: IAnnotatedGeometriesGroup? = null
    private var mAnnotatedGeometriesGroupCallback: MyAnnotatedGeometriesGroupCallback? = null

    //Ruta DOWNLOADS de SD donde lee los archivos DESCARGADOS ya sea objetos, xml, videos
    var externalStorage: File =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    // Geometries Object
    private var IGeoObject: IGeometry? = null
    private val mSailboat: IGeometry? = null
    var mTrackingConfigFile = ""

    // Geometries Video
    private var IGeoVideo: IGeometry? = null
    private val mCallbackHandler: MetaioSDKCallbackHandler? = null

    // Geometries GPS 	 
    private var IGeoGps: IGeometry? = null
    private var mRadar: IRadar? = null
    var modelGps: String? = ""
    var progressDialog: ProgressDialog? = null
    var progreso = 0
    var id = 0
    var mTask: GetDataAsync? = null

    //Zoom Controls
    var zoom: ZoomControls? = null
    var objectSize = 80.0f

    //MEDIA LAUNCHER 
    var MEDIA_LAUNCHER = ""

    //Nombre del Archivo de Tracking, Objetos y videos
    var mediaToExecute = ""

    //final String NOMBRE_OBJETO2 = "UH60.zip";
    var xmlTrackingFile = ""

    //final String NOMBRE_VIDEO ="moments.3g2";
    var latitudID = ""
    var longitudID = ""
    val File = ""
    var dataStatusBusiness: Bundle? = null
    var pb: ProgressBar? = null
    var dialog: Dialog? = null
    var downloadedSize = 0
    var totalSize = 0
    var cur_val: TextView? = null
    var download_file_path = "http://advertisingchannel.com.mx/oara/"

    //mediaLauncher  mediaToExecute  xmlTrackingFile  marker
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState)
        IGeoObject = null
        IGeoGps = null
        dataStatusBusiness = getIntent().getExtras()
        MEDIA_LAUNCHER = dataStatusBusiness.getString("mediaLauncher")
        mediaToExecute = dataStatusBusiness.getString("mediaToExecute")
        xmlTrackingFile = dataStatusBusiness.getString("xmlTrackingFile")
        latitudID = dataStatusBusiness.getString("latitudID")
        longitudID = dataStatusBusiness.getString("longitudID")

        //Toast.makeText(this, "Launcher: "+ MEDIA_LAUNCHER+ "    "+"File: " + mediaToExecute , Toast.LENGTH_LONG).show();
    }

    @Override
    fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu)
        return true
    }

    fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.getItemId()) {
            R.id.stopService -> {
                stopService(Intent(this@ArMotor, ServiceGps::class.java))
                true
            }
            R.id.resumeService -> {
                startService(Intent(this@ArMotor, ServiceGps::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }// TODO Auto-generated method stub

    //------------------------------------------------------------------------------
    @get:Override
    protected val gUILayout: Int
        protected get() =// TODO Auto-generated method stub
            R.layout.ar_view

    //------------------------------------------------------------------------------
    @Override
    protected fun loadContents() {
        if (MEDIA_LAUNCHER.equalsIgnoreCase("Object")) {
            objectLauncher()
        } else if (MEDIA_LAUNCHER.equalsIgnoreCase("Video")) {
            videoLauncher()
        } else if (MEDIA_LAUNCHER.equalsIgnoreCase("GpsObject")) {
            gpsLauncher()
            //mTask = new GetDataAsync();
            //mTask.execute(0);
        }
        //objectLauncher();			           
        //gpsLauncher();
        //videoLauncher();
    }

    //------------------------------------------------------------------------------
    fun objectLauncher() {
        var modelPath = ""
        try {
            // Load desired tracking data for planar marker tracking

            //final String mTrackingConfigFile;
            //mTrackingConfigFile = AssetsManager.getAssetPath("Recursos/sergiocunmarker.xml");
            val fileTrackingXml =
                File(externalStorage.getAbsolutePath().toString() + "/" + xmlTrackingFile)
            mTrackingConfigFile = if (fileTrackingXml.exists()) {
                //Do somehting
                externalStorage.getAbsolutePath().toString() + "/" + xmlTrackingFile
            } else {
                //DESPUES LO  LEE DESDE LA SD 
                downloadFile(xmlTrackingFile)
                externalStorage.getAbsolutePath().toString() + "/" + xmlTrackingFile
            }

            //mTrackingConfigFile = externalStorage.getAbsolutePath()+"/itchmarker.xml";
            val result: Boolean = metaioSDK.setTrackingConfiguration(mTrackingConfigFile)
            MetaioDebug.log("Tracking data loaded: $result")

            // Load all the geometries.
            val file2 = File(externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute)
            modelPath = if (file2.exists()) {
                //Do somehting
                externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute
            } else {
                //DESPUES LO  LEE DESDE LA SD 
                downloadFile(mediaToExecute)
                externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute
            }
            if (modelPath != null) {
                IGeoObject = metaioSDK.createGeometry(modelPath)
                if (IGeoObject != null) {
                    // Set geometry properties
                    IGeoObject.setScale(Vector3d(80.0f, 80.0f, 80.0f))
                    IGeoObject.setVisible(true)
                    MetaioDebug.log("Loaded geometry $modelPath")
                } else MetaioDebug.log(Log.ERROR, "Error loading geometry: $modelPath")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //------------------------------------------------------------------------------
    fun videoLauncher() {
        var moviePath = ""
        try {
            // Load desired tracking data for planar marker tracking

            //final String mTrackingConfigFile;
            //mTrackingConfigFile = AssetsManager.getAssetPath("Recursos/sergiocunmarker.xml");
            val fileTrackingXml =
                File(externalStorage.getAbsolutePath().toString() + "/" + xmlTrackingFile)
            mTrackingConfigFile = if (fileTrackingXml.exists()) {
                //Do somehting
                externalStorage.getAbsolutePath().toString() + "/" + xmlTrackingFile
            } else {
                //DESPUES LO  LEE DESDE LA SD 
                downloadFile(xmlTrackingFile)
                externalStorage.getAbsolutePath().toString() + "/" + xmlTrackingFile
            }
            val result: Boolean = metaioSDK.setTrackingConfiguration(mTrackingConfigFile)
            MetaioDebug.log("Tracking data loaded: $result")
            val file2 = File(externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute)
            moviePath = if (file2.exists()) {
                //Do somehting

                //cargando video
                externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute
            } else {
                //DESPUES LO  LEE DESDE LA SD 
                downloadFile(mediaToExecute)
                //cargando video
                externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute
            }
            MetaioDebug.log(Log.ERROR, "movie loaded: $moviePath")
            if (moviePath != null) {
                IGeoVideo = metaioSDK.createGeometryFromMovie(moviePath, false)
                if (IGeoVideo != null) {
                    //MetaioDebug.log(Log.ERROR, "movie created");
                    IGeoVideo.setScale(6.0f)
                    //mMoviePlane.setRotation(new Rotation(0f, 0f, (float)-Math.PI/2));
                    MetaioDebug.log("Loaded geometry $moviePath")
                } else {
                    MetaioDebug.log(Log.ERROR, "Error loading geometry: $moviePath")
                }
            }

            //start displaying the model
            setActiveModel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //------------------------------------------------------------------------------
    fun gpsLauncher() {

        // Set GPS tracking configuration
        val result: Boolean = metaioSDK.setTrackingConfiguration("GPS", false)
        MetaioDebug.log("Tracking data loaded: $result")
        mAnnotatedGeometriesGroup = metaioSDK.createAnnotatedGeometriesGroup()
        mAnnotatedGeometriesGroupCallback = MyAnnotatedGeometriesGroupCallback()
        mAnnotatedGeometriesGroup.registerCallback(mAnnotatedGeometriesGroupCallback)

        // Clamp geometries' Z position to range [5000;200000] no matter how close or far they are away.
        // This influences minimum and maximum scaling of the geometries (easier for development).
        metaioSDK.setLLAObjectRenderingLimits(5, 200)

        // Set render frustum accordingly
        metaioSDK.setRendererClippingPlaneLimits(10, 220000)

        // let's create LLA objects for known cities    18.51904776254762, -88.3028295636177 Chetumal
        val latitud: Double = Double.parseDouble(latitudID)
        val longitud: Double = Double.parseDouble(longitudID)
        val coordenadasGps = LLACoordinate(latitud, longitud, 0, 0)

        // Load some POIs. Each of them has the same shape at its geoposition. We pass a string
        // (const char*) to IAnnotatedGeometriesGroup::addGeometry so that we can use it as POI title
        // in the callback, in order to create an annotation image with the title on it.
        IGeoGps = createPOIGeometry(coordenadasGps)
        mAnnotatedGeometriesGroup.addGeometry(IGeoGps, "Empresa")
        val cubeReference: String =
            AssetsManager.getAssetPath(getApplicationContext(), "Recursos/cube.obj")
        //String metaioManModel = AssetsManager.getAssetPath(getApplicationContext(), "Recursos/metaioman.md2");
        if (cubeReference != null) {
            IGeoGps = metaioSDK.createGeometry(cubeReference)
            if (IGeoGps != null) {
                IGeoGps.setTranslationLLA(coordenadasGps)
                IGeoGps.setLLALimitsEnabled(true)
                IGeoGps.setScale(1)
            } else {
                MetaioDebug.log(Log.ERROR, "Error loading geometry: $cubeReference")
            }
        }


        // create radar
        mRadar = metaioSDK.createRadar()
        //La siguiente linea es la imagen est�tica de la brujula en la esquina superior izquierda
        mRadar.setBackgroundTexture(
            AssetsManager.getAssetPath(
                getApplicationContext(),
                "Recursos/mac.png"
            )
        )
        //La siguiente linea es el indicador del Sensor del Giroscopio
        mRadar.setObjectsDefaultTexture(
            AssetsManager.getAssetPath(
                getApplicationContext(),
                "Recursos/circleyellow.png"
            )
        )
        mRadar.setRelativeToScreen(IGeometry.ANCHOR_TL)

        // add geometries to the radar
        mRadar.add(IGeoGps)
    }

    //------------------------------------------------------------------------------
    fun zoomIn(v: View?) {

        //Toast.makeText(this, "Zoom IN", Toast.LENGTH_LONG).show();
        objectSize += 20.0f
        IGeoObject.setScale(Vector3d(objectSize, objectSize, objectSize))
        IGeoObject.setVisible(true)
    }

    fun zoomOut(v: View?) {

        //Toast.makeText(this, "Zoom IN", Toast.LENGTH_LONG).show();
        objectSize -= 20.0f
        IGeoObject.setScale(Vector3d(objectSize, objectSize, objectSize))
        IGeoObject.setVisible(true)
    }

    fun onObjectButtonClick(v: View?) {
        IGeoObject.setVisible(true)
        mSailboat.setVisible(false)
        IGeoVideo.setVisible(false)
    }

    /**
     * activates the sailboat model and deactivates the Metaio man model
     * @param v
     */
    fun onBoatButtonClick(v: View?) {
        IGeoObject.setVisible(false)
        mSailboat.setVisible(true)
        IGeoVideo.setVisible(false)
    }

    fun onVideoButtonClick(v: View?) {
        IGeoObject.setVisible(false)
        mSailboat.setVisible(false)
        IGeoVideo.setVisible(true)
    }

    /**
     * changes the tracking configuration to 'ID marker tracking'
     * @param v
     */
    @SuppressWarnings("deprecation")
    fun onIdButtonClick(v: View?) {
        mTrackingConfigFile = AssetsManager.getAssetPath("Recursos/TrackingData_Marker.xml")
        MetaioDebug.log("Tracking Config path = $mTrackingConfigFile")
        val result: Boolean = metaioSDK.setTrackingConfiguration(mTrackingConfigFile)
        MetaioDebug.log("Id Marker tracking data loaded: $result")
        IGeoObject.setScale(Vector3d(1.0f, 1.0f, 1.0f))
        mSailboat.setScale(Vector3d(3.0f, 3.0f, 3.0f))
    }

    /**
     * changes the tracking configuration to 'picture marker tracking'
     * @param v
     */
    @SuppressWarnings("deprecation")
    fun onPictureButtonClick(v: View?) {
        mTrackingConfigFile = AssetsManager.getAssetPath("Recursos/TrackingData_PictureMarker.xml")
        MetaioDebug.log("Tracking Config path = $mTrackingConfigFile")
        val result: Boolean = metaioSDK.setTrackingConfiguration(mTrackingConfigFile)
        MetaioDebug.log("Picture Marker tracking data loaded: $result")
        IGeoObject.setScale(Vector3d(6.0f, 6.0f, 6.0f))
        mSailboat.setScale(Vector3d(14.0f, 14.0f, 14.0f))
    }

    /**
     * changes the tracking configuration to 'markerless tracking'
     * @param v
     */
    @SuppressWarnings("deprecation")
    fun onMarkerlessButtonClick(v: View?) {
        mTrackingConfigFile = AssetsManager.getAssetPath("Recursos/TrackingData_MarkerlessFast.xml")
        MetaioDebug.log("Tracking Config path = $mTrackingConfigFile")
        val result: Boolean = metaioSDK.setTrackingConfiguration(mTrackingConfigFile)
        MetaioDebug.log("Markerless tracking data loaded: $result")
        IGeoObject.setScale(Vector3d(4.0f, 4.0f, 4.0f))
        mSailboat.setScale(Vector3d(12.0f, 12.0f, 12.0f))
    }

    //------------------------------------------------------------------------------
    @Override
    protected fun onGeometryTouched(geometry: IGeometry) {
        // TODO Auto-generated method stub
        MetaioDebug.log("Geometry selected: $geometry")
        if (geometry.equals(IGeoVideo)) {
            val status: MovieTextureStatus = IGeoVideo.getMovieTextureStatus()
            if (status.getPlaybackStatus() === EPLAYBACK_STATUS.EPLAYBACK_STATUS_PLAYING) IGeoVideo.pauseMovieTexture() else IGeoVideo.startMovieTexture(
                true
            )
        }

        /*
		mSurfaceView.queueEvent(new Runnable()
		{

			@Override
			public void run()
			{
				mRadar.setObjectsDefaultTexture(AssetsManager.getAssetPath(getApplicationContext(), "TutorialLocationBasedAR/Assets/yellow.png"));
				mRadar.setObjectTexture(geometry, AssetsManager.getAssetPath(getApplicationContext(), "TutorialLocationBasedAR/Assets/red.png"));
				mAnnotatedGeometriesGroup.setSelectedGeometry(geometry);
			}
		});
		
		*/
    }

    //------------------------------------------------------------------------------
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
            progressDialog.setTitle("Descargando . . . ")
            progressDialog.setMessage("Espere un momento…")
            progressDialog.setIndeterminate(false)
            progressDialog.setMax(100)
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

    //------------------------------------------------------------------------------
    @Override
    protected fun onDestroy() {
        // Break circular reference of Java objects
        if (mAnnotatedGeometriesGroup != null) {
            mAnnotatedGeometriesGroup.registerCallback(null)
        }
        if (mAnnotatedGeometriesGroupCallback != null) {
            mAnnotatedGeometriesGroupCallback.delete()
            mAnnotatedGeometriesGroupCallback = null
        }
        super.onDestroy()
    }

    //------------------------------------------------------------------------------
    @Override
    fun onDrawFrame() {
        if (metaioSDK != null && mSensors != null) {
            val sensorValues: SensorValues = mSensors.getSensorValues()
            var heading = 0.0f
            if (sensorValues.hasAttitude()) {
                val m = FloatArray(9)
                sensorValues.getAttitude().getRotationMatrix(m)
                var v = Vector3d(m[6], m[7], m[8])
                v = v.normalize()
                heading = (-Math.atan2(v.getY(), v.getX()) - Math.PI / 2.0)
            }
            val geos: Array<IGeometry?> = arrayOf<IGeometry?>(IGeoGps)
            val rot = Rotation((Math.PI / 2.0) as Float, 0.0f, -heading)
            for (geo in geos) {
                if (geo != null) {
                    geo.setRotation(rot)
                }
            }
        }
        super.onDrawFrame()
    }

    //------------------------------------------------------------------------------
    //ABAJO 
    private fun createPOIGeometry(lla: LLACoordinate): IGeometry? {
        val file2 = File(externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute)
        modelGps = if (file2.exists()) {
            //Do somehting
            externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute
        } else {

            /*
			//DESPUES LO  LEE DESDE LA SD 
			runOnUiThread(new Runnable() 
    		{
    			@Override
    			public void run() 
    			{
    				//id=0;
    		    	showDialog(id);
    			}
    		});
		downloadFile(mediaToExecute);
		*/
            /*
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/

            //gpsLauncher();
            //loadContents();
            externalStorage.getAbsolutePath().toString() + "/" + mediaToExecute
        }


        //String path = AssetsManager.getAssetPath(getApplicationContext(), "Recursos/metaioman.md2");
        //String path = AssetsManager.getAssetPath(getApplicationContext(), "Recursos/cube.obj");
        return if (modelGps != null) {
            val geo: IGeometry = metaioSDK.createGeometry(modelGps)
            geo.setTranslationLLA(lla)
            geo.setLLALimitsEnabled(true)
            geo.setScale(500)
            geo
        } else {
            MetaioDebug.log(Log.ERROR, "Missing files for POI geometry")
            null
        }
    }

    //------------------------------------------------------------------------------
    internal inner class MyAnnotatedGeometriesGroupCallback : AnnotatedGeometriesGroupCallback() {
        @Override
        fun loadUpdatedAnnotation(
            geometry: IGeometry?, userData: Object?,
            existingAnnotation: IGeometry?
        ): IGeometry? {
            if (userData == null) {
                return null
            }
            if (existingAnnotation != null) {
                // We don't update the annotation if e.g. distance has changed
                return existingAnnotation
            }
            val title = userData as String // as passed to addGeometry
            val texturePath = getAnnotationImageForTitle(title)
            return metaioSDK.createGeometryFromImage(texturePath, true, false)
        }

        @Override
        fun onFocusStateChanged(
            geometry: IGeometry?, userData: Object,
            oldState: EGEOMETRY_FOCUS_STATE, newState: EGEOMETRY_FOCUS_STATE
        ) {
            MetaioDebug.log("onFocusStateChanged for " + userData as String + ", " + oldState + "->" + newState)
        }
    }

    //------------------------------------------------------------------------------
    private fun getAnnotationImageForTitle(title: String): String? {
        var billboard: Bitmap? = null
        try {
            val texturepath: String = getCacheDir().toString() + "/" + title + ".png"
            val mPaint = Paint()

            // Load background image and make a mutable copy
            val dpi: Float = SystemInfo.getDisplayDensity(getApplicationContext())
            val scale = if (dpi > 240) 2 else 1
            val filepath: String = AssetsManager.getAssetPath(
                getApplicationContext(),
                "TutorialLocationBasedAR/Assets/POI_bg" + (if (scale == 2) "@2x" else "") + ".png"
            )
            val mBackgroundImage: Bitmap = BitmapFactory.decodeFile(filepath)
            billboard = mBackgroundImage.copy(Bitmap.Config.ARGB_8888, true)
            val c = Canvas(billboard)
            mPaint.setColor(Color.WHITE)
            mPaint.setTextSize(24)
            mPaint.setTypeface(Typeface.DEFAULT)
            mPaint.setTextAlign(Paint.Align.CENTER)
            var y = (40 * scale).toFloat()
            val x = (30 * scale).toFloat()

            // Draw POI name
            if (title.length() > 0) {
                var n: String = title.trim()
                val maxWidth = 160 * scale
                var i: Int = mPaint.breakText(n, true, maxWidth, null)
                val xPos: Int = c.getWidth() / 2
                val yPos = (c.getHeight() / 2 - (mPaint.descent() + mPaint.ascent()) / 2) as Int
                c.drawText(n.substring(0, i), xPos, yPos, mPaint)

                // Draw second line if valid
                if (i < n.length()) {
                    n = n.substring(i)
                    y += (20 * scale).toFloat()
                    i = mPaint.breakText(n, true, maxWidth, null)
                    if (i < n.length()) {
                        i = mPaint.breakText(n, true, maxWidth - 20 * scale, null)
                        c.drawText(n.substring(0, i).toString() + "...", x, y, mPaint)
                    } else {
                        c.drawText(n.substring(0, i), x, y, mPaint)
                    }
                }
            }

            // Write texture file
            try {
                val out = FileOutputStream(texturepath)
                billboard.compress(Bitmap.CompressFormat.PNG, 90, out)
                MetaioDebug.log("Texture file is saved to $texturepath")
                return texturepath
            } catch (e: Exception) {
                MetaioDebug.log("Failed to save texture file")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            MetaioDebug.log("Error creating annotation texture: " + e.getMessage())
            MetaioDebug.printStackTrace(Log.DEBUG, e)
            return null
        } finally {
            if (billboard != null) {
                billboard.recycle()
                billboard = null
            }
        }
        return null
    }

    //------------------------------------------------------------------------------
    private fun setActiveModel() {
        //Si hubierse otro video, se pausa el modelvideo y se inicia el otro
        //modelVideo.stopMovieTexture();
        IGeoVideo.setVisible(true)
        // IGeoVideo.stopMovieTexture();
        IGeoVideo.startMovieTexture(true)

        // Start or pause movie according to tracking state
        mCallbackHandler!!.onTrackingEvent(metaioSDK.getTrackingValues())
    }// TODO Auto-generated method stub

    //------------------------------------------------------------------------------
    @get:Override
    protected val metaioSDKCallbackHandler: IMetaioSDKCallback?
        protected get() =// TODO Auto-generated method stub
            null

    //------------------------------------------------------------------------------
    private inner class MetaioSDKCallbackHandler : IMetaioSDKCallback() {
        @Override
        fun onSDKReady() {
            // show GUI after SDK is ready
            runOnUiThread(object : Runnable() {
                @Override
                fun run() {
                    mGUIView.setVisibility(View.VISIBLE)
                }
            })
        }

        //------------------------------------------------------------------------------
        @Override
        fun onTrackingEvent(trackingValues: TrackingValuesVector) {
            super.onTrackingEvent(trackingValues)

            // We only have one COS, so there can only ever be one TrackingValues structure passed.
            // Play movie if the movie button was selected and we're currently tracking.
            if (trackingValues.isEmpty() || !trackingValues.get(0).isTrackingState()) {
                if (IGeoVideo != null) IGeoVideo.startMovieTexture(true)
            }
        }
    }

    inner class GetDataAsync : AsyncTask<Integer?, Integer?, Boolean?>() {
        @Override
        protected fun doInBackground(vararg params: Integer?): Boolean {
            gpsLauncher()
            return true
        }
    } /*
	  class DownloadAsyncTask extends AsyncTask<Void,Void,Void>{
			protected Void doInBackground(Void...argO){
				try {
		            URL url = new URL(download_file_path+mediaToExecute);
		            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
		 
		            urlConnection.setRequestMethod("GET");
		            urlConnection.setDoOutput(true);
		 
		            //<span id="IL_AD3" class="IL_AD">connect</span>
		            urlConnection.connect();
		 
		            //set the path where we want to <span id="IL_AD8" class="IL_AD">save</span> the file            
		            File SDCardRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		            //create a new file, to save the downloaded file 
		            File file = new File(SDCardRoot,mediaToExecute);
		  
		            FileOutputStream fileOutput = new FileOutputStream(file);
		 
		            //Stream used for reading the data from the internet
		            InputStream inputStream = urlConnection.getInputStream();
		 
		            //this is the total size of the file which we are downloading
		            totalSize = urlConnection.getContentLength();
		            		            
		            //create a buffer...
		            byte[] buffer = new byte[1024];
		            int bufferLength = 0;
		 
		            while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
		                fileOutput.write(buffer, 0, bufferLength);
		                downloadedSize += bufferLength;
		                 
		                //publishing the progress
		                progreso=(int)(downloadedSize*100/totalSize);
		    			publishProgress();     			
		    			
		            }
		            
		            //close the output stream when complete //
		            fileOutput.close();
		                   
		            //removeDialog(1);
		        } catch (final MalformedURLException e) {
		            showError("Error : MalformedURLException " + e);        
		            e.printStackTrace();
		        } catch (final IOException e) {
		            showError("Error : IOException " + e);          
		            e.printStackTrace();
		        }
		        catch (final Exception e) {
		            showError("Error : Please check your internet connection " + e);
		        }     
				
				
		        
				return null;			
		  }
			protected void onProgressUpdate(Void...progress) {
				progressDialog.setProgress(progreso) ;
				if(progreso==100)removeDialog(id);
				//se puede usar lo siguiente para ocultar el diálogo
				// como alternativa:
				// if(progreso==100)progressDialog . hide();
				}
			
			protected void onPostExecute(String unused) {
	            //dismiss the dialog after the file was downloaded
	            dismissDialog(id);
	            
	        }
			
			
		}
		
	*/
}