package demo.myfirstbarcode;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.honeywell.aidc.*;

import java.util.ConcurrentModificationException;

public class MainActivity extends AppCompatActivity  implements BarcodeReader.BarcodeListener, BarcodeReader.TriggerListener {

    private AidcManager manager;
    private BarcodeReader reader;
    private Context context=this;
    TextView textView;
    Button buttonScan;
    String TAG="MyFirstBarcode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Starting ...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView=(TextView)findViewById(R.id.textView2);
        buttonScan=(Button)findViewById(R.id.btnScan);

        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(reader!=null){
                    startScan(true);
                    // see https://stackoverflow.com/questions/3072173/how-to-call-a-method-after-a-delay-in-android
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            //Do something after 100ms
                            startScan(false);
                        }
                    }, 5000);
                }
            }
        });

        // create the AidcManager providing a Context and an
        // CreatedCallback implementation.
        AidcManager.create(this, new AidcManager.CreatedCallback() {

            @Override
            public void onCreated(AidcManager aidcManager) {
                manager = aidcManager;
                // use the manager to create a BarcodeReader with a session
                // associated with the internal imager.
                reader = manager.createBarcodeReader("dcs.scanner.imager");

                try {
                    reader.claim();
                } catch (ScannerUnavailableException e) {
                    e.printStackTrace();
                    Toast.makeText(context, "Scanner unavailable", Toast.LENGTH_SHORT).show();
                }

                try {
                    // apply settings
                    reader.setProperty(BarcodeReader.PROPERTY_CODE_39_ENABLED, false);
                    reader.setProperty(BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);

                    // set the trigger mode to automatic control
                    reader.setProperty(BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                            BarcodeReader.TRIGGER_CONTROL_MODE_CLIENT_CONTROL);

                } catch (UnsupportedPropertyException e) {
                    Toast.makeText(MainActivity.this, "Failed to apply properties",
                            Toast.LENGTH_SHORT).show();
                }

                // register bar code event listener
                reader.addBarcodeListener(MainActivity.this);
                // register trigger state change listener
                reader.addTriggerListener(MainActivity.this);
            }
        });

    }//onCreate

    void startScan(boolean triggerState){
            try{
                reader.aim(triggerState);
                reader.light(triggerState);
                reader.decode(triggerState);
            } catch (ScannerNotClaimedException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Scanner is not claimed",
                        Toast.LENGTH_SHORT).show();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                Toast.makeText(MainActivity.this, "Scanner unavailable",
                        Toast.LENGTH_SHORT).show();
            }
        
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String barcodeData;
                barcodeData = event.getBarcodeData();
                String timestamp = event.getTimestamp();
                // update UI to reflect the data
                textView.setText(barcodeData + "\n" + timestamp);
            }
        });
    }

    @Override
    public void onFailureEvent(final BarcodeFailureEvent event) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "Barcode read failed",
                        Toast.LENGTH_LONG).show();
                Log.i(TAG, "onFailureEvent: "+ event.toString());
            }
        });
    }

    @Override
    public void onTriggerEvent(TriggerStateChangeEvent event) {
        if(event.getState())
            startScan(true);
        else
            startScan(false);
    }

}
