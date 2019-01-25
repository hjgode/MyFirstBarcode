package demo.myfirstbarcode;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.CompoundButton;
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
    CheckBox checkbox;

    String TAG="MyFirstBarcode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate ...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView=(TextView)findViewById(R.id.textView2);
        buttonScan=(Button)findViewById(R.id.btnScan);
        checkbox=(CheckBox)findViewById(R.id.checkBox);
        checkbox.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            if(reader!=null){
                                                boolean bIsChecked=checkbox.isChecked();
                                                String sIsChecked="";
                                                if(bIsChecked)
                                                    sIsChecked="True";
                                                else
                                                    sIsChecked="fAlSe";
                                                try {
                                                    // DEC_EAN13_CHECK_DIGIT_TRANSMIT
                                                    reader.setProperty("DEC_EAN13_CHECK_DIGIT_TRANSMIT", bIsChecked);
                                                }
                                                catch (Exception ex){
                                                    Log.e(TAG, "checkbox.setOnClickListener Exception: " +ex.getMessage());
                                                }
                                            }
                                        }
                                    });

                buttonScan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (reader != null) {
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

//  copied to onResume, which is not called after onCreate
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

                    //update checkbox
                    boolean bCheckDigit = reader.getBooleanProperty("DEC_EAN13_CHECK_DIGIT_TRANSMIT");
                    checkbox.setChecked(bCheckDigit);

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

    @Override
    public void onResume(){
        Log.i(TAG, "onResume ...");
        super.onResume();
        // put your code here...
        try {
            if(reader!=null) {
                reader.claim();
            }
        } catch (ScannerUnavailableException e) {
            e.printStackTrace();
            Toast.makeText(context, "Scanner unavailable", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onPause(){
        Log.i(TAG, "onPause ...");
        super.onPause();
        if(reader!=null)
            reader.release();
    }

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
        Log.i(TAG, "onBarcodeEvent ...");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String barcodeData;
                barcodeData = event.getBarcodeData();
                String timestamp = event.getTimestamp();
                // update UI to reflect the data
                textView.setText("Data: "+barcodeData + "\nTime: " + timestamp +
                        "\nAimID: " + event.getAimId()
                        + "CodeID: \n" + event.getCodeId() +
                        "\ndata: "+ strToHexString(event.getBarcodeData()));
            }
        });
    }

    @Override
    public void onFailureEvent(final BarcodeFailureEvent event) {
        Log.i(TAG, "onFailureEvent ...");
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
        Log.i(TAG, "onTriggerEvent ...");
        if(event.getState())
            startScan(true);
        else
            startScan(false);
    }

    String strToHexString(String s){
        StringBuilder sb= new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if( (int)s.charAt(i) < 32){
                sb.append("["+ Integer.toHexString ((int)s.charAt(i))+"]");
            }
            else{
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private String bytesToHexString(byte[] arr) {
        String s = "[]";
        if (arr != null) {
            s = "[";
            for (int i = 0; i < arr.length; i++) {
                if(arr[i]<32)
                    s += "0x" + Integer.toHexString(arr[i]) + ", ";
                else
                    s+=(char) (arr[i] & 0xFF)+", ";
            }
            s = s.substring(0, s.length() - 2) + "]";
        }
        return s;
    }
}
