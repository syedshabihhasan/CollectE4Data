EMPATICA API (2.0)
==================

The EmpaLink API allows you to create Android applications that can communicate with Empatica E4 and Empatica E3 devices.

First of all, you need to make sure your phone runs Android 4.4 KitKat (API level 19) or higher.
Android 4.3 doesn't offer a stable enough connection, while previous versions don't support Bluetooth 4.0 (BLE) at all, and, therefore, are not compatible with the Empatica API.

This release is meant to be used with Android Studio 1.0 or later.

DEPENDENCIES
------------

* [Android Asynchronous Http Client](https://github.com/loopj/android-async-http/archive/1.4.6.zip) - 1.4.6 

__WARNING__: 

You need "Android Asynchronous Http Client" __v1.4.6__. Other versions of the library are not supported.


INSTALLATION INSTRUCTIONS
-------------------------

1. Open your project in Android Studio 

2. Create a new Module from the AAR file (`empalink-2.0.aar`) included in the zip: `File > New > New Module > Import .JAR or .AAR Package`

3. Open `Module Settings` for your main app module and, in the `Dependencies` tab, add a dependency to the EmpaLink module you've just imported 

4. Open your main build.gradle and, in the `dependencies { ... }` block, add the following line:

	* `compile 'com.loopj.android:android-async-http:1.4.6'`

5. Make sure your `build.gradle` has a `minSdkVersion 19` (or higher) line.

6. If you're using Android Studio 1.1 with Gradle version 1.1, it may be possible that the IDE will tell you it can't resolve the symbols contained in the `empalink` library, though the compiler will compile just fine.  
If this is the case, please open your project `build.gradle`, find the gradle dependency line (e.g., `classpath 'com.android.tools.build:gradle:1.1.0'`) and change it into `classpath 'com.android.tools.build:gradle:1.0.1'`.  
If this doesn't help you, StackOverflow is your friend: Android Studio 1.1 currently has a few issues with importing AAR's.


USAGE
-----

First of all, you need to instantiate an `EmpaDeviceManager`, passing your application context, and references to classes implementing `EmpaDataDelegate` and `EmpaStatusDelegate`.  
Then, you must register your **API Key** using the Device Manager's `authenticateWithAPIKey()` method.

Here's an example:

	public class MainActivity extends Activity implements EmpaDataDelegate, EmpaStatusDelegate {

    	private EmpaDeviceManager deviceManager;

    	protected void onCreate(Bundle savedInstanceState) { 
    	
    		[...]

			deviceManager = new EmpaDeviceManager(getApplicationContext(), this, this);  
			deviceManager.authenticateWithAPIKey("YOUR API KEY");
		}
		
		[...]

	}

When the Device Manager is ready for use, your `EmpaStatusDelegate` will receive the `EmpaStatus.READY` value via `didUpdateStatus()`.  
The Device Manager is now ready to scan for Empatica Devices, using: `deviceManager.startScanning()`.  
If any devices are in range, you will receive them through the `EmpaStatusDelegate` callback `didDiscoverDevice(BluetoothDevice device, String deviceLabel, int rssi, boolean allowed)`.  
If `allowed` is true, you can then connect to the device as follows: `deviceManager.connectDevice(device)`.  
If the connection request is successful, the device will start streaming data, which will be transferred to your `EmpaDataDelegate` by invoking its callback methods.

Please check the Javadoc documentation for details about all the available methods.