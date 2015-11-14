package edu.uiowa.cs.teste4;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {
    static final int ENABLE_BT = 1;
    Button enableDisable;
    Intent startE4Service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "Enabling intent for bluetooth");
        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetoothIntent, ENABLE_BT);
//        connectToE4 = new ConnectToE4(this);
        startE4Service  = new Intent(this, E4DataService.class);
        enableDisable = (Button) findViewById(R.id.button);
        setButtonLogic();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        //TODO: if prompted and the user cancels request, handle this
        Log.d("e4Set", "Got a result, request code:"+requestCode);
        super.onActivityResult(requestCode, resultCode, data);
    }

    protected void setButtonLogic(){
        Log.d("MainActivity", "Setting Button Logic, isRecording = "+E4DataService.isRecording);
        if(!E4DataService.isRecording){
            enableDisable.setText("Start");
            enableDisable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    connectToE4.enableEmpaticaDeviceManager();
                    startService(startE4Service);
                    E4DataService.isRecording = true;
                    setButtonLogic();
                }
            });
        }else{
            enableDisable.setText("Stop");
            enableDisable.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    connectToE4.disconnectFromDevice();
                    stopService(startE4Service);
                    E4DataService.isRecording = false;
                    setButtonLogic();
                }
            });
        }
    }
}
