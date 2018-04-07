package com.sleaf.androidtest

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.*
import com.baidu.location.BDLocation
import com.baidu.location.BDLocationListener
import com.baidu.location.LocationClient
import com.baidu.location.LocationClientOption
import com.baidu.mapapi.SDKInitializer
import com.baidu.mapapi.map.*
import com.baidu.mapapi.model.LatLng
import kotlinx.android.synthetic.main.activity_second.*
import java.io.Serializable


class BaiduMapActivity : AppCompatActivity() {
    private var mMapView: MapView? = null
    private var mBaiduMap: BaiduMap? = null
    //定位相关
    private var mLocationClient: LocationClient? = null
    private var mLocationListener: MyLocationListener? = null
    private var isFirstIn = true
    private lateinit var context: Context
    private var mLatitude = 0.0
    private var mLongitude = 0.0
    private var mCurrentX = 0.0f
    private lateinit var mLocationMode: MyLocationConfiguration.LocationMode
    //自定义图标
    var mIconLocation: BitmapDescriptor? = null
    private lateinit var myOrientationListener: MyOrientationListener

    //覆盖物相关
    lateinit var mMaker: BitmapDescriptor
    lateinit var mMakerLayout: RelativeLayout

    private fun initView() {
        mMapView = findViewById(R.id.bmapView)
        mBaiduMap = mMapView?.map
        val msu = MapStatusUpdateFactory.zoomTo(15.0F)
        mBaiduMap!!.setMapStatus(msu)
    }

    private fun initLocation() {
        mLocationMode = MyLocationConfiguration.LocationMode.NORMAL
        mLocationClient = LocationClient(this)
        mLocationListener = MyLocationListener()
        mLocationClient!!.registerLocationListener(mLocationListener)
        val option = LocationClientOption()
        option.setCoorType("bd09ll")
        option.setIsNeedAddress(true)
        option.openGps = true
        option.setScanSpan(1000)
        mLocationClient!!.locOption = option
        mIconLocation = BitmapDescriptorFactory.fromResource(R.drawable.ic_arrow)
        myOrientationListener = MyOrientationListener(this.context)
        myOrientationListener.mOnOrientationListener = object : OnOrientationListener {
            override fun onOrientationChanged(x: Float) {
                mCurrentX = x
            }
        }
    }

    private fun initMarker() {
        mMaker = BitmapDescriptorFactory.fromResource(R.drawable.ic_my_location)
        mMakerLayout = findViewById(R.id.id_maker_layout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(applicationContext)
        setContentView(R.layout.activity_baidu_map)
        this.context = this
        initView()
        initLocation()
        initMarker()

        mBaiduMap!!.setOnMarkerClickListener {
            val extraInfo = it.extraInfo
            val info = extraInfo.getSerializable("info") as Info
            val iv = mMakerLayout.findViewById<ImageView>(R.id.id_info_img)
            val distance = mMakerLayout.findViewById<TextView>(R.id.id_info_distance)
            val name = mMakerLayout.findViewById<TextView>(R.id.id_info_name)
            val like = mMakerLayout.findViewById<TextView>(R.id.id_info_like)
            iv.setImageResource(info.imgID)
            distance.text = info.distance
            name.text = info.name
            like.text = info.like.toString()

            val tv = TextView(context)
            tv.setBackgroundResource(R.drawable.tooltip_frame_light)
            tv.setPadding(30, 20, 30, 50)
            tv.text = info.name

            val latLng = LatLng(info.latitude, info.longitude)
            val p = mBaiduMap!!.projection.toScreenLocation(latLng)
            p.y -= 47
            val ll = mBaiduMap!!.projection.fromScreenLocation(p)
            val infoWindow = InfoWindow(tv, ll, 0)
            mBaiduMap!!.showInfoWindow(infoWindow)
            mMakerLayout.visibility = View.VISIBLE
            true
        }

        mBaiduMap!!.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
            override fun onMapClick(p0: LatLng?) {
                mMakerLayout.visibility = View.GONE
                mBaiduMap!!.hideInfoWindow()
            }

            override fun onMapPoiClick(p0: MapPoi?): Boolean {
                return false
            }

        })
    }

    override fun onDestroy() {
        super.onDestroy()
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mMapView!!.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView!!.onResume()
    }

    override fun onStart() {
        super.onStart()
        //开始定位
        mBaiduMap?.isMyLocationEnabled = true
        if (!mLocationClient!!.isStarted) {
            mLocationClient!!.start()
        }
        myOrientationListener.start()
    }

    override fun onStop() {
        super.onStop()
        //停止定位
        mBaiduMap!!.isMyLocationEnabled = false
        mLocationClient!!.stop()
        myOrientationListener.stop()
    }

    override fun onPause() {
        super.onPause()
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView!!.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_baidu_map, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item!!.itemId) {
            R.id.id_map_common -> {
                mBaiduMap!!.mapType = BaiduMap.MAP_TYPE_NORMAL
            }
            R.id.id_map_site -> {
                mBaiduMap!!.mapType = BaiduMap.MAP_TYPE_SATELLITE
            }
            R.id.id_map_traffic -> {
                mBaiduMap!!.isTrafficEnabled = !mBaiduMap!!.isTrafficEnabled
                item.title = "实时交通（${if (mBaiduMap!!.isTrafficEnabled) {
                    "on"
                } else {
                    "off"
                }}）"

            }
            R.id.id_map_mylocation -> {
                centerToMyLocation()
            }
            R.id.id_map_mode_common -> {
                mLocationMode = MyLocationConfiguration.LocationMode.NORMAL
            }
            R.id.id_map_mode_follow -> {
                mLocationMode = MyLocationConfiguration.LocationMode.FOLLOWING
            }
            R.id.id_map_mode_compass -> {
                mLocationMode = MyLocationConfiguration.LocationMode.COMPASS
            }
            R.id.id_map_overlay -> {
                addOverlays(Info.infos)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addOverlays(infos: List<Info>) {
        mBaiduMap!!.clear()
        var latLng: LatLng? = null
        var maker: Marker? = null
        var options: OverlayOptions? = null
        for (info: Info in infos) {
            latLng = LatLng(info.latitude, info.longitude)
            options = MarkerOptions()
                    .position(latLng)
                    .icon(mMaker)
                    .zIndex(5)
            maker = mBaiduMap!!.addOverlay(options) as Marker?
            val tmp = Bundle()
            tmp.putSerializable("info", info)
            maker!!.extraInfo = tmp
        }

        val msu = MapStatusUpdateFactory.newLatLng(latLng)
        mBaiduMap!!.setMapStatus(msu)
    }

    inner class MyLocationListener : BDLocationListener {
        override fun onReceiveLocation(location: BDLocation) {
            mLatitude = location.latitude
            mLongitude = location.longitude
            val data = MyLocationData.Builder()
                    .accuracy(location.radius)
                    .direction(mCurrentX)
                    .latitude(mLatitude)
                    .longitude(mLongitude)
                    .build()
            mBaiduMap!!.setMyLocationData(data)
            //设置自定义图标
            val config = MyLocationConfiguration(mLocationMode, true, mIconLocation)
            mBaiduMap!!.setMyLocationConfiguration(config)
            if (isFirstIn) {
                Toast.makeText(context, location.addrStr, Toast.LENGTH_SHORT).show()
                centerToMyLocation()
                isFirstIn = false
            }
        }

    }

    private fun centerToMyLocation() {
        val latLng = LatLng(mLatitude, mLongitude)
        val msu = MapStatusUpdateFactory.newLatLng(latLng)
        mBaiduMap!!.animateMapStatus(msu)
    }
}

class MyOrientationListener(private val mContext: Context) : SensorEventListener {
    lateinit var mSensorManager: SensorManager
    private var mSensorMagnetic: Sensor? = null
    private var mSensorAccelerometer: Sensor? = null
    var lastX = 0.0F
    lateinit var mOnOrientationListener: OnOrientationListener
    private var accelerometerValues = FloatArray(3)
    private var magneticFieldValues = FloatArray(3)
    private var values = FloatArray(3)
    private var rotate = FloatArray(9)

    fun start() {
        mSensorManager = mContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensorMagnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (mSensorMagnetic != null) {
            mSensorManager.registerListener(this, mSensorMagnetic, SensorManager.SENSOR_DELAY_UI)
        }
        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stop() {
        mSensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerValues = event.values
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magneticFieldValues = event.values
            }
        }
        SensorManager.getRotationMatrix(rotate, null, accelerometerValues, magneticFieldValues)//通过磁力和加速度值计算旋转矩阵，赋值给rotate
        SensorManager.getOrientation(rotate, values)//最后通过矩阵数组计算x,y,z方向手机角度，目前我们需要第一个角度x轴的
        values[0] = Math.toDegrees(values[0].toDouble()).toFloat()
        val x = values[0]
        if (Math.abs(x - lastX) > 1) {
            Log.i("test", "定位角度是：" + values[0])//此结果为从北到南顺时针为0-180度，从南-北顺指针-180到0度，其实是弧度运算的，也就是从北顺指针是0-360度
            mOnOrientationListener.onOrientationChanged(x)
        }
        lastX = x
    }
}

interface OnOrientationListener {
    fun onOrientationChanged(x: Float)
}

class Info(var latitude: Double,
           var longitude: Double,
           var imgID: Int,
           var name: String,
           var distance: String,
           var like: Int = 0) : Serializable {

    companion object {
        val infos = listOf(
                Info(31.297430, 121.5614234, R.drawable.img_youya, "上海理工大学-音乐堂", "200米",256),
                Info(31.2985566, 121.55901, R.drawable.img_youya, "上海理工大学-学生活动中心", "250米"),
                Info(31.29247789289, 121.560273, R.drawable.img_youya, "卜蜂莲花(周家嘴路店)", "1250米",999)
        )
    }
}