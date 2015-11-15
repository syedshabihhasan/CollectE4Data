# TestE4
Simple android app to collect data from Empatica E4 bands

## A few things to keep in mind
- You need to have your own API Key to access the E4 band. This also means that you will need a working internet connection for the API key validation. 
- The API key needs to be entered in the E4DataService.java file in the APIKEY variable (see below) before you load the application to your android device
```java
public class E4DataService extends Service implements EmpaDataDelegate, EmpaStatusDelegate {

    public static boolean isRecording = false;
    private EmpaDeviceManager empaDeviceManager;
    private Context context;
    private static final String APIKEY = ""; //TODO: add the key
```
- You will a need Bluetooth Low Energy capable device for the bands to connect. No, there is no way around this.
