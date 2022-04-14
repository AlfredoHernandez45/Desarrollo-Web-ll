package com.example.convert

import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import com.metaio.sdk.ARViewActivity
import com.metaio.sdk.MetaioDebug
import com.metaio.sdk.jni.AnnotatedGeometriesGroupCallback
import com.metaio.sdk.jni.EGEOMETRY_FOCUS_STATE
import com.metaio.sdk.jni.IAnnotatedGeometriesGroup
import com.metaio.sdk.jni.IGeometry
import com.metaio.sdk.jni.IMetaioSDKCallback
import com.metaio.sdk.jni.IRadar
import com.metaio.sdk.jni.LLACoordinate
import com.metaio.sdk.jni.Rotation
import com.metaio.sdk.jni.SensorValues
import com.metaio.sdk.jni.Vector3d
import com.metaio.tools.SystemInfo
import com.metaio.tools.io.AssetsManager

class GPS : ARViewActivity() {
    private var mAnnotatedGeometriesGroup: IAnnotatedGeometriesGroup? = null
    private var mAnnotatedGeometriesGroupCallback: MyAnnotatedGeometriesGroupCallback? = null

    // Geometries
    private var geoCitqroo: IGeometry? = null
    private var mRadar: IRadar? = null
    @Override
    fun onCreate(savedInstanceState: Bundle?) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState)

        // Set GPS tracking configuration
        val result: Boolean = metaioSDK.setTrackingConfiguration("GPS", false)
        MetaioDebug.log("Tracking data loaded: $result")
    }

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
            val geos: Array<IGeometry?> = arrayOf<IGeometry?>(geoCitqroo)
            val rot = Rotation((Math.PI / 2.0) as Float, 0.0f, -heading)
            for (geo in geos) {
                if (geo != null) {
                    geo.setRotation(rot)
                }
            }
        }
        super.onDrawFrame()
    }

    // TODO Auto-generated method stub
    @get:Override
    protected val gUILayout: Int
        protected get() =// TODO Auto-generated method stub
            R.layout.ar_view

    // TODO Auto-generated method stub
    @get:Override
    protected val metaioSDKCallbackHandler: IMetaioSDKCallback?
        protected get() =// TODO Auto-generated method stub
            null

    @Override
    protected fun loadContents() {
        // TODO Auto-generated method stub
        mAnnotatedGeometriesGroup = metaioSDK.createAnnotatedGeometriesGroup()
        mAnnotatedGeometriesGroupCallback = MyAnnotatedGeometriesGroupCallback()
        mAnnotatedGeometriesGroup.registerCallback(mAnnotatedGeometriesGroupCallback)

        // Clamp geometries' Z position to range [5000;200000] no matter how close or far they are away.
        // This influences minimum and maximum scaling of the geometries (easier for development).
        metaioSDK.setLLAObjectRenderingLimits(5, 200)

        // Set render frustum accordingly
        metaioSDK.setRendererClippingPlaneLimits(10, 220000)

        // let's create LLA objects for known cities    18.51904776254762, -88.3028295636177 Chetumal

        //Chetumal CITQROO radio 5 mts  18.51907319573751, -88.3028993010521
        //Casa 18.51970902425589, -88.32891136407852
        val citqroo = LLACoordinate(18.51970902425589, -88.32891136407852, 0, 0)


        // Load some POIs. Each of them has the same shape at its geoposition. We pass a string
        // (const char*) to IAnnotatedGeometriesGroup::addGeometry so that we can use it as POI title
        // in the callback, in order to create an annotation image with the title on it.
        geoCitqroo = createPOIGeometry(citqroo)
        mAnnotatedGeometriesGroup.addGeometry(geoCitqroo, "citqroo")
        val metaioManModel: String =
            AssetsManager.getAssetPath(getApplicationContext(), "Recursos/cube.obj")
        //String metaioManModel = AssetsManager.getAssetPath(getApplicationContext(), "Recursos/metaioman.md2");
        if (metaioManModel != null) {
            geoCitqroo = metaioSDK.createGeometry(metaioManModel)
            if (geoCitqroo != null) {
                geoCitqroo.setTranslationLLA(citqroo)
                geoCitqroo.setLLALimitsEnabled(true)
                geoCitqroo.setScale(250)
            } else {
                MetaioDebug.log(Log.ERROR, "Error loading geometry: $metaioManModel")
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
        mRadar.add(geoCitqroo)
    }

    //ABAJO 
    private fun createPOIGeometry(lla: LLACoordinate): IGeometry? {
        val path: String =
            AssetsManager.getAssetPath(getApplicationContext(), "Recursos/metaioman.md2")
        //String path = AssetsManager.getAssetPath(getApplicationContext(), "Recursos/cube.obj");
        return if (path != null) {
            val geo: IGeometry = metaioSDK.createGeometry(path)
            geo.setTranslationLLA(lla)
            geo.setLLALimitsEnabled(true)
            geo.setScale(60000)
            geo
        } else {
            MetaioDebug.log(Log.ERROR, "Missing files for POI geometry")
            null
        }
    }

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

    @Override
    protected fun onGeometryTouched(geometry: IGeometry) {
        MetaioDebug.log("Geometry selected: $geometry")
        mSurfaceView.queueEvent(object : Runnable() {
            @Override
            fun run() {
                mRadar.setObjectsDefaultTexture(
                    AssetsManager.getAssetPath(
                        getApplicationContext(),
                        "TutorialLocationBasedAR/Assets/yellow.png"
                    )
                )
                mRadar.setObjectTexture(
                    geometry,
                    AssetsManager.getAssetPath(
                        getApplicationContext(),
                        "TutorialLocationBasedAR/Assets/red.png"
                    )
                )
                mAnnotatedGeometriesGroup.setSelectedGeometry(geometry)
            }
        })
    }

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
}