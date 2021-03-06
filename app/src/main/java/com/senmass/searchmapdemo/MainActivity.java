package com.senmass.searchmapdemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public  class MainActivity extends AppCompatActivity implements LocateRecyclerAdapter.OnLocationItemClick, AMapLocationListener, PoiSearch.OnPoiSearchListener, Inputtips.InputtipsListener {

    public AMapLocationClient mlocationClient = null;
    @BindView(R.id.locate_recycler)
    RecyclerView mLocateRecycler;
    @BindView(R.id.locate_cancel)
    TextView mLocateCancel;
    @BindView(R.id.locate_refresh)
    TextView mLocateRefresh;
    private AMapLocationClientOption locationOption = new AMapLocationClientOption();
    private List<LocationInfo> mList;
    private LocateRecyclerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initLocate();
        mList = new ArrayList<>();
        mAdapter = new LocateRecyclerAdapter(MainActivity.this, mList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mLocateRecycler.setLayoutManager(layoutManager);
        mLocateRecycler.setAdapter(mAdapter);
        mAdapter.setLocationItemClick(this);
    }

    private void initLocate() {
        //声明mLocationOption对象
        AMapLocationClientOption mLocationOption = null;
        mlocationClient = new AMapLocationClient(this);
        //初始化定位参数
        mLocationOption = new AMapLocationClientOption();
        //设置定位监听
        mlocationClient.setLocationListener(this);
        //设置定位模式为高精度模式，Battery_Saving为低功耗模式，Device_Sensors是仅设备模式
        mLocationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
        //设置定位间隔,单位毫秒,默认为2000ms
        mLocationOption.setInterval(2000);
        mLocationOption.setOnceLocationLatest(true);
        //设置定位参数
        mlocationClient.setLocationOption(mLocationOption);
        // 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
        // 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
        // 在定位结束后，在合适的生命周期调用onDestroy()方法
        // 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
        //启动定位
        mlocationClient.startLocation();
    }


    @Override
    public void onLocationChanged(AMapLocation amapLocation) {
        if (amapLocation != null) {
            if (amapLocation.getErrorCode() == 0) {
                //定位成功回调信息，设置相关消息
                amapLocation.getLocationType();//获取当前定位结果来源，如网络定位结果，详见定位类型表
                double latitude = amapLocation.getLatitude();//获取纬度
                double longitude = amapLocation.getLongitude();//获取经度
                amapLocation.getAccuracy();//获取精度信息
                Log.d("haha", amapLocation.getAddress());
                LocationInfo locationInfo = new LocationInfo();
                locationInfo.setAddress(amapLocation.getAddress());
                locationInfo.setLatitude(latitude);
                locationInfo.setLonTitude(longitude);

                //这一段代码放在监听 EditText 监听文字改变的地方就可以达到边输入 边出来下面的周围地址     基本不用改就改一个地方我在下面标注
                mList.clear();
                mList.add(locationInfo);
                mAdapter.notifyDataSetChanged();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = new Date(amapLocation.getTime());
                df.format(date);//定位时间
                PoiSearch.Query query = new PoiSearch.Query("", "生活服务", "");
                query.setPageSize(20);
                PoiSearch search = new PoiSearch(this, query);
                search.setBound(new PoiSearch.SearchBound(new LatLonPoint(latitude, longitude), 10000));
                search.setOnPoiSearchListener(this);
                search.searchPOIAsyn();
                //！！！！！！前面第一个参数改成 你EditText 监听改变出来的文字就是你想输入的比如超市   第二个就是这个东西在哪个城市可以为空 意思就是全国搜索超市 填了天津就是 在天津搜索超市
                InputtipsQuery inputquery = new InputtipsQuery("嘉里汇", "天津市");
                inputquery.setCityLimit(true);//限制在当前城市
                Inputtips inputTips = new Inputtips(MainActivity.this, inputquery);
                inputTips.setInputtipsListener(MainActivity.this);
                inputTips.requestInputtipsAsyn();
                //


            } else {
                //显示错误信息ErrCode是错误码，errInfo是错误信息，详见错误码表。
                Log.e("AmapError", "location Error, ErrCode:"
                        + amapLocation.getErrorCode() + ", errInfo:"
                        + amapLocation.getErrorInfo());
            }
        }
    }

    @Override
    public void onPoiSearched(PoiResult result, int i) {
        PoiSearch.Query query = result.getQuery();
        ArrayList<PoiItem> pois = result.getPois();
        for (PoiItem poi : pois) {
            String name = poi.getCityName();
            String snippet = poi.getSnippet();
            LocationInfo info = new LocationInfo();
            info.setAddress(snippet);
            LatLonPoint point = poi.getLatLonPoint();

            info.setLatitude(point.getLatitude());
            info.setLonTitude(point.getLongitude());
            mList.add(info);
            Log.d("haha", "poi" + snippet);

        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {

    }


    @Override
    public void OnLocationClick(RecyclerView parent, View view, int position, LocationInfo locationInfo) {

    }

    @OnClick({R.id.locate_cancel, R.id.locate_refresh})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.locate_cancel:
                finish();
                break;
            case R.id.locate_refresh:
                initLocate();
                Toast.makeText(this, "正在重定位", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onGetInputtips(List<Tip> list, int i) {
        //这个是你搜索超市出来的周边的地方
        for(int j=0;j<list.size();j++){
            String name = list.get(j).getName();
            String address = list.get(j).getAddress();
            /*LocationInfo locationInfo = new LocationInfo();
            locationInfo.setAddress(name);
            locationInfo.setLatitude(list.get(j).getPoint().getLatitude()); //获取经纬度
            locationInfo.setLonTitude(list.get(j).getPoint().getLongitude());*///获取经纬度

            Log.i("ddddddddddd",""+name+"      "+address+"     "+list.get(j).getDistrict()+"        "+list.get(j).getPoint());
        }
    }
}
