package nl.woutermarra.ahnglasses;

import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import org.json.*;

public final class MainActivity extends Activity {
    // for building the cards
    private List<CardBuilder> mCards;
    private CardScrollView mCardScrollView;
    private ExampleCardScrollAdapter mAdapter;

    // getting GPS location
    private LocationManager mLocationManager;
    private String mLocationProvider;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        // allow stupid stuff
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Ensure screen stays on during demo.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // get GPS lcoation
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);
        mLocationProvider = mLocationManager.getBestProvider(new Criteria(), true);
        Location location = mLocationManager.getLastKnownLocation(mLocationProvider);

        Double lat = 52.3267291;
        Double lon =  7.0276122;

        if (location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
        }

        // convert gps coordinates to RD coordinates
        /*
        // OLD method using server:
        URL projecturl = null;
        try {
            projecturl = new URL("http", "arcgis-server.geo.uu.nl", 6080, "arcgis/rest/services/Utilities/Geometry/GeometryServer/project?inSR=4326&outSR=28992&geometries=%7B%0D%0A%22geometryType%22%3A%22esriGeometryPoint%22%2C%0D%0A%22geometries%22%3A%5B%7B%22x%22%3A" +
                    Double.toString(lon) + "%2C%22y%22%3A" + Double.toString(lat) + "%7D%5D%0D%0A%7D&transformation=108237&transformForward=false&f=pjson");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Log.v("GlassMap", "Project coordintes url: "  + projecturl);

        // parse json response for projection
        String jsonstrproject = "";
        double x = 0.0;
        double y = 0.0;

        try {
            jsonstrproject =  new RetrieveData().execute(projecturl).get();
            JSONObject json = new JSONObject(jsonstrproject);
            JSONObject geometies = json.getJSONArray("geometries").getJSONObject(0);
            x = Double.parseDouble(geometies.getString("x"));
            y = Double.parseDouble(geometies.getString("y"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        // Direct calculation:
        double dlat = 0.36*(lat - 52.15517440);
        double dlon = 0.36*(lon - 5.38720621);

        double dx = 190094.945 *  Math.pow(dlat, 0) * Math.pow(dlon, 1) +  -11832.228 *  Math.pow(dlat, 1) * Math.pow(dlon, 1) +  -114.221 *  Math.pow(dlat, 2) * Math.pow(dlon, 1) +  -32.391 *  Math.pow(dlat, 0) * Math.pow(dlon, 3) +  -0.705 *  Math.pow(dlat, 1) * Math.pow(dlon, 0) +  -2.34 *  Math.pow(dlat, 3) * Math.pow(dlon, 1) +  -0.608 *  Math.pow(dlat, 1) * Math.pow(dlon, 3) +  -0.008 *  Math.pow(dlat, 0) * Math.pow(dlon, 2) +  0.148 *  Math.pow(dlat, 2) * Math.pow(dlon, 3);
        double dy = 309056.544 *  Math.pow(dlat, 1) * Math.pow(dlon, 0) +  3638.893 *  Math.pow(dlat, 0) * Math.pow(dlon, 2) +  73.077 *  Math.pow(dlat, 2) * Math.pow(dlon, 0) +  -157.984 *  Math.pow(dlat, 1) * Math.pow(dlon, 2) +  59.788 *  Math.pow(dlat, 3) * Math.pow(dlon, 0) +  0.433 *  Math.pow(dlat, 0) * Math.pow(dlon, 1) +  -6.439 *  Math.pow(dlat, 2) * Math.pow(dlon, 2) +  -0.032 *  Math.pow(dlat, 1) * Math.pow(dlon, 1) +  0.092 *  Math.pow(dlat, 0) * Math.pow(dlon, 4) +  -0.054 *  Math.pow(dlat, 1) * Math.pow(dlon, 4);

        double x = dx + 155000;
        double y = dy + 463000;

        Log.v("GlassMap", "Coordintes: " + lat + ", " + lon + " / " + x + ", " + y);

        // Get Height
        URL urlvalue = null;
        try {
            urlvalue = new URL("http", "ahn.arcgisonline.nl", 80, "/arcgis/rest/services/Hoogtebestand/AHN2_r/ImageServer/identify?" +
                    "geometry=%7B%22y%22+%3A+" + y + "%2C+%22x%22+%3A+" + x +
                    "%2C+%22spatialReference%22+%3A+%7B%22wkid%22+%3A+28992%7D%7D&geometryType=esriGeometryPoint&mosaicRule=&renderingRule=&pixelSize=&time=&returnGeometry=false&returnCatalogItems=false&f=pjson");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        Log.v("GlassMap", "Heigt url: " + urlvalue);

        // parse json response of height
        String jsonstr = "";
        String z = "?";

        try {
            jsonstr =  new RetrieveData().execute(urlvalue).get();
            JSONObject json = new JSONObject(jsonstr);
            z = json.getString("value");
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Build Cards for 4 different zoom levels
        mCards = new ArrayList<CardBuilder>();
        mCards.add(createAHNCard(x, y, z, 1.0));
        mCards.add(createAHNCard(x, y, z, 0.5));
        mCards.add(createAHNCard(x, y, z, 0.25));
        mCards.add(createAHNCard(x, y, z, 0.125));

        // Build views
        mCardScrollView = new CardScrollView(this);
        mAdapter = new ExampleCardScrollAdapter();
        mCardScrollView.setAdapter(mAdapter);
        mCardScrollView.activate();
        setContentView(mCardScrollView);

    }

    private CardBuilder createAHNCard(double x, double y, String z, double scale) {

        // calculate bounding box
        double xres = 640.0;
        double yres = 360.0;

        String bbox = Double.toString(x-(xres*scale)) + "%2C" + Double.toString(y-(yres*scale)) + "%2C"
                + Double.toString(x+(xres*scale)) + "%2C" + Double.toString(y+(yres*scale));

        Log.v("GlassMap", "Bbox: " + bbox);

        // Url for image
        String url = "http://ahn.arcgisonline.nl/arcgis/rest/services/Hoogtebestand/AHN2_r/ImageServer/exportImage?bbox=" + bbox +
                "&bboxSR=28992&size=640%2C360&imageSR=28992&" +
                "format=jpg&pixelType=U8&noData=&noDataInterpretation=esriNoDataMatchAny&interpolation=+RSP_BilinearInterpolation&" +
                "compression=&compressionQuality=&bandIds=&mosaicRule=&" +
                "renderingRule=%7B\"rasterFunction\"%3A\"AHN2+-+Shaded+Relief\"%7D&" +
                "f=image";


        // Height string
        String str = "z = " + z + " m";//, x" + Double.toString(x);

        // Get Image
        Drawable result = null;
        Log.v("GlassMap", "Trying do download: "  + url);
        try {
            result = new DownloadImageTask().execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // Make Card
        return new CardBuilder(this, CardBuilder.Layout.CAPTION)
                .setFootnote(str)
                .addImage(result);

        }

    private Drawable drawableFromUrl(String url) {
        // get image from url
        try {
            Bitmap bitmap;
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            InputStream input = connection.getInputStream();
            bitmap = BitmapFactory.decodeStream(input);
            return new BitmapDrawable(bitmap);
        } catch (IOException e) {
            return null;
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Drawable> {
        // Async download task
        protected Drawable doInBackground(String... url) {
            return drawableFromUrl(url[0]);
        }

        protected void onPostExecute(Drawable result) {
        }
    }

    private class RetrieveData extends AsyncTask<URL, String, String> {
        // Get data from url
        @Override
        protected String doInBackground(URL... resource) {
            String data = "";
            try {
                //URL url = new URL(resource[0]);
                HttpURLConnection connection = (HttpURLConnection) resource[0].openConnection();
                InputStream in = connection.getInputStream(); //new BufferedInputStream(connection.getInputStream());
                data = convertStreamToString(in);
                in.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return data;
        }

        private String convertStreamToString(InputStream in) {
            Scanner s = new Scanner(in);
            return s.useDelimiter("\\A").hasNext() ? s.next() : "";
        }
    }

    private class ExampleCardScrollAdapter extends CardScrollAdapter {

        @Override
        public int getPosition(Object item) {
            return mCards.indexOf(item);
        }

        @Override
        public int getCount() {
            return mCards.size();
        }

        @Override
        public Object getItem(int position) {
            return mCards.get(position);
        }

        @Override
        public int getViewTypeCount() {
            return CardBuilder.getViewTypeCount();
        }

        @Override
        public int getItemViewType(int position){
            return mCards.get(position).getItemViewType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return mCards.get(position).getView(convertView, parent);
        }
    }
}