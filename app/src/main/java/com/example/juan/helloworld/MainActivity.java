package com.example.juan.helloworld;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class MainActivity extends ActionBarActivity {

    private static final String TAG = "MainActivity";
    private boolean mExternalStorageAvailable = false;
    private boolean mExternalStorageWriteable = false;

    private String comentario = "";
    private String logFileName = "log.txt";

    private String encabezados = "Provider"+"\t"
                                    +"Accuracy [m]"+"\t"
                                    +"Latitude"+"\t"
                                    +"Longitude"+"\t"
                                    +"Speed m/s"+"\t"
                                    +"Date"+"\t"
                                    +"Time"+"\t"
                                    +"Comment";

    private TextView tvConsole;
    private EditText etComment;
    private Button bInicio;
    private RadioGroup radioGroup;

    private String texto_iniciar = "Start detection";
    private String texto_parar = "Stop detection";

    private String provider = null;
    private String labelProvider = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvConsole = (TextView)findViewById(R.id.tv_console);
        bInicio = (Button)findViewById(R.id.button);
        etComment = (EditText) findViewById(R.id.editText);
        radioGroup = (RadioGroup)findViewById(R.id.radioGroup);

        verificarExtMem();

        // Acquire a reference to the system Location Manager
        final LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        final LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                logLocation(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                writeConsole("Provider "+provider +" status changed. "+getTime());
                logComment("Provider "+provider +" status changed. "+getTime());
            }

            public void onProviderEnabled(String provider) {
                writeConsole("Provider "+provider +" enabled. "+getTime());
                logComment("Provider "+provider +" enabled. "+getTime());
            }

            public void onProviderDisabled(String provider) {
                writeConsole("Provider "+provider +" disabled. "+getTime());
                logComment("Provider "+provider +" disabled. "+getTime());
            }
        };

        bInicio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(bInicio.getText().equals(texto_iniciar)){
                    int id = radioGroup.getCheckedRadioButtonId();
                    if(id == R.id.rb_gps){
                        provider = LocationManager.GPS_PROVIDER;
                        labelProvider = "GPS";
                        turnGPSOn();
                    }else if(id == R.id.rb_wifi){
                        provider = LocationManager.NETWORK_PROVIDER;
                        labelProvider = "WIFI";
                        turnOnWifi();
                    }else{
                        provider = LocationManager.NETWORK_PROVIDER;
                        labelProvider = "CELL TOWER";
                    }
                    // Register the listener with the Location Manager to receive location updates
                    //At this point detection begins
                    logComment(" ");
                    logComment("Detection started "+labelProvider+" "+getTime());
                    logComment(encabezados);
                    locationManager.requestLocationUpdates(provider, 0, 0, locationListener);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bInicio.setText(texto_parar);
                        }
                    });
                }else{
                    logComment("Detection stopped: "+labelProvider +" "+getTime());
                    locationManager.removeUpdates(locationListener);
                    if(labelProvider.equals("GPS")){
                        turnGPSOff();
                    }else if(labelProvider.equals("WIFI")){
                        turnOffWifi();
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bInicio.setText(texto_iniciar);
                        }
                    });
                }
            }
        });

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void logLocation(Location location){

        Time time = new Time(Time.getCurrentTimezone());
        time.set(location.getTime());

        String logText = labelProvider+"\t"
                        +location.getAccuracy()+"\t"
                        +location.getLatitude()+"\t"
                        +location.getLongitude()+"\t"
                        +location.getSpeed()+"\t"
                        +time.format("%Y-%m-%d")+"\t"
                        +time.format("%H:%M:%S.000")+"\t"
                        +etComment.getText();

        generateNoteOnSD(logFileName,logText);
        writeConsole("New location registered "+time.format("%Y-%m-%d T %H:%M:%S.000").toString());
    }

    public void logComment(String comentario){
        String logText = comentario;
        writeConsole(logText);
        generateNoteOnSD(logFileName,comentario);
    }

    /*
	 * Escribe un archivo en la memoria externa del dispositivo
	 */
    public void generateNoteOnSD(String sFileName, String sBody){
        if(this.mExternalStorageWriteable){
            try
            {
                /*File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "LocationStudyApp/Log/");

                if (!root.exists()) {
                    root.mkdirs();
                    Log.i(TAG, "Creando directorio");
                }*/

                //Use BufferedWriter if many requests are made
                File txtFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), sFileName);
                if(txtFile.exists()){
                    //Log.i(TAG, "El archivo ya existe");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile,true));
                    writer.append(sBody);
                    writer.newLine();
                    writer.flush();
                    writer.close();
                }else{
                    Log.i(TAG, "El archivo no existe, creando");
                    BufferedWriter writer = new BufferedWriter(new FileWriter(txtFile));
                    writer.append(sBody);
                    writer.newLine();
                    writer.flush();
                    writer.close();
                }

                MediaScannerConnection.scanFile(this, new String[]{txtFile.getAbsolutePath()}, null, null);
            }
            catch(IOException e)
            {
                Log.e(TAG, e.getMessage());
            }
        }else{
            Log.i(TAG, "Memoria no puede ser escrita");
            Toast.makeText(this, "No se puede escribir en memoria externa, " +
                    "no se generara log", Toast.LENGTH_LONG).show();
        }


    }

    public void toLog(String message){
        StringBuilder sb = new StringBuilder();
        sb.append(message+"\t"+getTime());
        sb.append("\r\n");
        generateNoteOnSD("log.txt",sb.toString());
        //Falta verificación de tamaño

    }

    /*
	 * Verifica la disponibilidad de la memoria extderna
	 */
    public void verificarExtMem(){
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // We can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
            Toast.makeText(this, "Acceso completo a memoria externa", Toast.LENGTH_SHORT).show();
            toLog("Acceso completo a memoria externa");
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
            Toast.makeText(this, "Memoria externa solo puede ser leída", Toast.LENGTH_SHORT).show();
            toLog("Memoria externa solo puede ser leída");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            //  to know is we can neither read nor write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
            Toast.makeText(this, "No se puede acceder a la memoria externa", Toast.LENGTH_SHORT).show();
            toLog("No se puede acceder a la memoria externa");
        }
    }

    private String getTime(){
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        //Log.i(TAG, "Time: "+today.format("%H:%M:%S"));
        return today.format("%H:%M:%S");
    }

    private String getDateFileFormat(){
        Time today = new Time(Time.getCurrentTimezone());
        today.setToNow();
        //Log.i(TAG, "Date File Format: "+today.format("%d_%m_%Y"));
        return today.format("%d_%m_%Y");
    }

    public void writeConsole(final String text){
        runOnUiThread(new Runnable(){
            public void run(){
                tvConsole.setText(text);
            }
        });
    }

    /*public void turnGPSOn()
    {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        this.sendBroadcast(intent);

        String provider = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.contains("gps")){ //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.sendBroadcast(poke);


        }
    }*/

    public void turnGPSOn()
    {
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        this.sendBroadcast(intent);

        String provider = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(!provider.contains("gps"))
        {
            //if gps is disabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.sendBroadcast(poke);
        }
    }
    // automatic turn off the gps
    /*public void turnGPSOff()
    {
        String provider = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.sendBroadcast(poke);
        }
    }*/
    public void turnGPSOff()
    {
        String provider = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        if(provider.contains("gps")){ //if gps is enabled
            final Intent poke = new Intent();
            poke.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
            poke.addCategory(Intent.CATEGORY_ALTERNATIVE);
            poke.setData(Uri.parse("3"));
            this.sendBroadcast(poke);
        }
    }

    public void turnOnWifi(){
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
    }

    public void turnOffWifi(){
        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
    }
}
