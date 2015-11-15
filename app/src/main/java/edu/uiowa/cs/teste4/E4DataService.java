package edu.uiowa.cs.teste4;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.empatica.empalink.ConnectionNotAllowedException;
import com.empatica.empalink.EmpaDeviceManager;
import com.empatica.empalink.config.EmpaSensorStatus;
import com.empatica.empalink.config.EmpaSensorType;
import com.empatica.empalink.config.EmpaStatus;
import com.empatica.empalink.delegate.EmpaDataDelegate;
import com.empatica.empalink.delegate.EmpaStatusDelegate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class E4DataService extends Service implements EmpaDataDelegate, EmpaStatusDelegate {

    public static boolean isRecording = false;
    private EmpaDeviceManager empaDeviceManager;
    private Context context;
    private static final String APIKEY = ""; //TODO: add the key
    private static boolean keepWriting = false;

    private Queue<String> accelerationQueue, gsrQueue, bvpQueue,
            temperatureQueue, ibiQueue, batteryQueue;


    private FileOutputStream accelerationFOS, ibiFOS, bvpFOS,
            gsrFOS, batteryFOS, temperatureFOS;
    private OutputStreamWriter accelerationOSW, ibiOSW, bvpOSW,
            gsrOSW, batteryOSW, temperatureOSW;

    private Thread extractFromQueue;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("E4Service", "Entered onStartCommand");
        context = this;
        initVals(getCurrentDateTime());

        empaDeviceManager = new EmpaDeviceManager(context, this, this);
        empaDeviceManager.authenticateWithAPIKey(APIKEY);
        Log.d("E4Service", "Authentication command executed");

        return START_NOT_STICKY;
    }

    private void initVals(String currentDateTime) {
        createStorageSpace();
        Log.d("E4Service", "Initializing Queues");
        accelerationQueue = new ConcurrentLinkedQueue<>();

        gsrQueue = new ConcurrentLinkedQueue<>();

        bvpQueue = new ConcurrentLinkedQueue<>();

        temperatureQueue = new ConcurrentLinkedQueue<>();

        ibiQueue = new ConcurrentLinkedQueue<>();

        batteryQueue = new ConcurrentLinkedQueue<>();

        Log.d("E4Service", "Queues initialized, opening files");
        try{
            accelerationFOS = new FileOutputStream(
                    "/sdcard/teste4/acceleration_"+ currentDateTime+".txt", false);
            accelerationOSW = new OutputStreamWriter(accelerationFOS);

            gsrFOS = new FileOutputStream(
                    "/sdcard/teste4/gsr_"+currentDateTime+".txt", false);
            gsrOSW = new OutputStreamWriter(gsrFOS);

            bvpFOS = new FileOutputStream(
                    "/sdcard/teste4/bvp_"+currentDateTime+".txt", false);
            bvpOSW = new OutputStreamWriter(bvpFOS);

            temperatureFOS = new FileOutputStream(
                    "/sdcard/teste4/temperature_"+currentDateTime+".txt", false);
            temperatureOSW = new OutputStreamWriter(temperatureFOS);

            ibiFOS = new FileOutputStream(
                    "/sdcard/teste4/ibi_"+currentDateTime+".txt", false);
            ibiOSW = new OutputStreamWriter(ibiFOS);

            batteryFOS = new FileOutputStream(
                    "/sdcard/teste4/battery_"+currentDateTime+".txt", false);
            batteryOSW = new OutputStreamWriter(batteryFOS);

            Log.d("E4Service", "files opened");

        }catch (Exception e){
            Log.e("E4Service", "Error opening files to write to");
            e.printStackTrace();
        }

        extractFromQueue = new Thread(new Runnable() {
            @Override
            public void run() {
                writeToFile();
            }
        });

    }

    @Override
    public void onDestroy(){
        Log.d("E4Service", "Disconnecting");
        empaDeviceManager.disconnect();
        empaDeviceManager.cleanUp();
        isRecording = false;
        keepWriting = false;
    }


    @Override
    public void didReceiveGSR(float gsr, double timestamp) {
        gsrQueue.add("" + timestamp + "," + gsr + "\n");
    }

    @Override
    public void didReceiveBVP(float bvp, double timestamp) {
        bvpQueue.add("" + timestamp + "," + bvp + "\n");
    }

    @Override
    public void didReceiveIBI(float ibi, double timestamp) {
        ibiQueue.add("" + timestamp + "," + ibi + "\n");
    }

    @Override
    public void didReceiveTemperature(float temperature, double timestamp) {
        temperatureQueue.add("" + timestamp + "," + temperature + "\n");
    }

    @Override
    public void didReceiveAcceleration(int x, int y, int z, double timestamp) {
        accelerationQueue.add("" + timestamp + "," + x + ", " + y + "," + z + "\n");
    }

    @Override
    public void didReceiveBatteryLevel(float bl, double timestamp) {
        batteryQueue.add("" + timestamp + "," + bl + "\n");
    }

    @Override
    public void didUpdateStatus(EmpaStatus empaStatus) {
        Log.d("E4Service", "Entered didUpdateStatus");
        if(EmpaStatus.READY == empaStatus){
            Log.d("E4Service", "Started Scanning");
            this.empaDeviceManager.startScanning();
        }else if(EmpaStatus.DISCOVERING == empaStatus){
            Log.d("E4Service", "Discovering");
        }else if(EmpaStatus.CONNECTING == empaStatus){
            Log.d("E4Service", "Connecting");
        }else if(EmpaStatus.CONNECTED == empaStatus){
            Log.d("E4Service", "Connected");
            Log.d("E4Service", "Starting thread for extracting data");
            keepWriting = true;
            extractFromQueue.start();
        }
    }

    @Override
    public void didUpdateSensorStatus(EmpaSensorStatus empaSensorStatus,
                                      EmpaSensorType empaSensorType) {}

    @Override
    public void didDiscoverDevice(BluetoothDevice bluetoothDevice, String deviceName,
                                  int rssi, boolean allowed) {
        Log.d("E4Service", "Some bluetooth device discovered, allowed: "+allowed);
        if(allowed){
            this.empaDeviceManager.stopScanning();
            try{
                Log.d("E4Service", "Connecting");
                this.empaDeviceManager.connectDevice(bluetoothDevice);
                isRecording = true;
            } catch (ConnectionNotAllowedException e){
                Log.e("E4Service", "Could not connect to the band");
                e.printStackTrace();
            }

        }
    }

    @Override
    public void didRequestEnableBluetooth() {}

    private void writeToFile(){
        Log.d("E4Service", "Entered thread to extract data");
        while(keepWriting){
            writeFromQueue(accelerationQueue.poll(), accelerationOSW, "ACC");
            writeFromQueue(bvpQueue.poll(), bvpOSW, "BVP");
            writeFromQueue(ibiQueue.poll(), ibiOSW, "IBI");
            writeFromQueue(gsrQueue.poll(), gsrOSW, "GSR");
            writeFromQueue(temperatureQueue.poll(), temperatureOSW, "TMP");
            writeFromQueue(batteryQueue.poll(), batteryOSW, "BAT");
        }

        Log.d("E4Service", "Ending thread, writing remaining values");
        String tempResp;
        while(null != (tempResp = accelerationQueue.poll())){
            writeFromQueue(tempResp, accelerationOSW, "ACC_END");
        }
        while(null != (tempResp = bvpQueue.poll())){
            writeFromQueue(tempResp, bvpOSW, "BVP_END");
        }
        while (null != (tempResp = ibiQueue.poll())){
            writeFromQueue(tempResp, ibiOSW, "IBI_END");
        }
        while (null != (tempResp = gsrQueue.poll())){
            writeFromQueue(tempResp, gsrOSW, "GSR_END");
        }
        while (null != (tempResp = temperatureQueue.poll())){
            writeFromQueue(tempResp, temperatureOSW, "TMP_END");
        }
        while (null != (tempResp = batteryQueue.poll())){
            writeFromQueue(tempResp, batteryOSW, "BAT_END");
        }
        Log.d("E4Service", "Done writing remaining values, closing files");

        closeFiles();
        Log.d("E4Service", "Done closing files");
    }

    private void writeFromQueue(String tempString, OutputStreamWriter streamToWriteTo, String tag){
        if(null != tempString){
            try{
                streamToWriteTo.write(tempString);
            } catch (Exception e){
                Log.e("E4Service", "Error writing to file in " + tag);
                e.printStackTrace();
            }
        }
    }

    private void closeFiles(){
        try{
            accelerationOSW.close();
            accelerationFOS.close();

            gsrOSW.close();
            gsrFOS.close();

            ibiOSW.close();
            ibiFOS.close();

            bvpOSW.close();
            bvpFOS.close();

            temperatureOSW.close();
            temperatureFOS.close();

            batteryOSW.close();
            batteryFOS.close();
        }catch(Exception e){
            Log.e("E4Service", "Error closing files");
            e.printStackTrace();
        }
    }

    private String getCurrentDateTime(){
        Calendar calendar = Calendar.getInstance();
        return ""+
                calendar.get(Calendar.YEAR)+"_"+
                (calendar.get(Calendar.MONTH)+1)+"_"+
                calendar.get(Calendar.DAY_OF_MONTH)+"_"+
                calendar.get(Calendar.HOUR_OF_DAY)+"_"+
                calendar.get(Calendar.MINUTE)+"_"+
                calendar.get(Calendar.SECOND)+"_"+
                calendar.get(Calendar.MILLISECOND);
    }

    private void createStorageSpace(){
        Log.d("E4Service", "Checking if /sdcard/teste4 exists");
        File f = new File("/sdcard/teste4/");
        if(!f.isDirectory()){
            Log.d("E4Service", "Does not exist, creating...");
            f.mkdirs();
            Log.d("E4Service", "done!");
        }else{
            Log.d("E4Service", "It exists...");
        }
    }
}
