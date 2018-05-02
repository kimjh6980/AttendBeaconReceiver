package asan.hospital.asanbeacon;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.distance.AndroidModel;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import asan.hospital.asanbeacon.databinding.ActivityMainBinding;

public class MainActivity extends BaseActivity implements BeaconConsumer {

    private static final String BEACON_PARSER = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-27";

    private DecimalFormat decimalFormat = new DecimalFormat("#.##");

    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 100;

    BluetoothAdapter mBluetoothAdapter;

    BeaconAdapter beaconAdapter;

    ActivityMainBinding binding;

    BeaconManager mBeaconManager;

    Vector<Item> items;

    LinearLayoutManager manager;

    TextView FindValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        FindValue = (TextView) findViewById(R.id.FindValue);

        AndroidModel am = AndroidModel.forThisDevice();
        Log.d("getManufacturer()",am.getManufacturer());
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            mBeaconManager = BeaconManager.getInstanceForApplication(this);
            mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_PARSER));
            //BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
        }

        binding.scanBleFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBeaconManager.isBound(MainActivity.this)) {
                    /*
                    binding.scanBleFAB.setImageResource(R.drawable.ic_visibility_white_24dp);
                    Log.i(TAG, "Stop BLE Scanning...");
                    mBeaconManager.unbind(MainActivity.this);
                    */
                    StopBLE();
                } else {
                    /*
                    binding.scanBleFAB.setImageResource(R.drawable.ic_visibility_off_white_24dp);
                    Log.i(TAG, "Start BLE Scanning...");
                    mBeaconManager.bind(MainActivity.this);
                    */
                    StartBLE();
                }
            }
        });
        manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        binding.beaconListView.setLayoutManager(manager);
    }

    private void StopBLE()  {
        binding.scanBleFAB.setImageResource(R.drawable.ic_visibility_white_24dp);
        Log.i(TAG, "Stop BLE Scanning...");
        mBeaconManager.unbind(MainActivity.this);
    }
    private void StartBLE() {
        binding.scanBleFAB.setImageResource(R.drawable.ic_visibility_off_white_24dp);
        Log.i(TAG, "Start BLE Scanning...");
        mBeaconManager.bind(MainActivity.this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBeaconManager.unbind(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            mBeaconManager = BeaconManager.getInstanceForApplication(this);
            mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(BEACON_PARSER));
            //BeaconManager.setRssiFilterImplClass(ArmaRssiFilter.class);
        }

    }

    @Override
    public void onBeaconServiceConnect() {
        mBeaconManager.addRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                if (beacons.size() > 0) {
                    Iterator<Beacon> iterator = beacons.iterator();
                    items = new Vector<>();
                    while (iterator.hasNext()) {
                        Beacon beacon = iterator.next();
                        List<Long> data = beacon.getDataFields();
                        String address = beacon.getBluetoothAddress();
                            final double rssi = beacon.getRssi();
                            final int txPower = beacon.getTxPower();
                            final double distance = Double.parseDouble(decimalFormat.format(beacon.getDistance()));
                            final String uuid = beacon.getId1().toString();
                            final int major = beacon.getId2().toInt();
                            final int minor = beacon.getId3().toInt();
                            final String datafield = beacon.getDataFields().toString();
                            Log.e("UUID =", uuid);
                            Log.e("Datafield = ", datafield);
                            items.add(new Item(address, rssi, txPower, distance, major, minor));
                            if(uuid.equals("2f234454-cf6d-4a0f-adf2-f4911ba9ffa6"))  {
                                Log.e("UUID is", "same");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        runOnUiThread(new Runnable(){
                                            @Override
                                            public void run() {
                                                Toast.makeText(getApplicationContext(), "Find", Toast.LENGTH_SHORT).show();
                                                FindValue.setText(uuid +"/"+major+"/"+minor+"/"+datafield+"/"+rssi+"/"+txPower+"/"+distance);
                                                StopBLE();
                                            }
                                        });
                                    }
                                }).start();
                            }
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            beaconAdapter = new BeaconAdapter(items, MainActivity.this);
                            binding.beaconListView.setAdapter(beaconAdapter);
                            beaconAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }
        });
        try {
            mBeaconManager.startRangingBeaconsInRegion(new Region("myRangingUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mBeaconManager.addMonitorNotifier(new MonitorNotifier() {

            @Override
            public void didEnterRegion(Region region) {
                Log.i(TAG, "I just saw an beacon for the first time!");
            }

            @Override
            public void didExitRegion(Region region) {
                Log.i(TAG, "I no longer see an beacon");
            }


            @Override
            public void didDetermineStateForRegion(int state, Region region) {
                    Log.i(TAG, "I have just switched from seeing/not seeing beacons: "+state);
            }
        });
        try {
            mBeaconManager.startMonitoringBeaconsInRegion(new Region("myMonitoringUniqueId", null, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
