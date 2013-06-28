package com.example.helloworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
//import com.example.android.BluetoothChat.BluetoothChatService.AcceptThread;
//from http://www.drdobbs.com/tools/hacking-for-fun-programming-a-wearable-a/240007471
// MAC Address:	e4:ce:8f:37:44:4e   

public class MainActivity extends Activity implements GestureDetector.OnGestureListener, SensorEventListener{
    MainActivity master = this;
    int REQUEST_ENABLE_BT;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothDevice laptop;
    Button blueButton;
    Button orangeButton;
    BluetoothSocket mmSocket = null;
    Boolean isConnectingToArduino = false;
    Boolean isController = false;
    Boolean isControlled = false;

    //Activities: blue/red screen vs. knob 

    private boolean taskComplete = false;
    // private static final UUID MY_UUID =
    // UUID.fromString("00001105-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");


    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Layout Views
    private TextView incomingMessage;

    // Device info
    private String deviceType = android.os.Build.DEVICE;
    private static final String DROIDX = "cdma_shadow";
    private static final String GOGGLES = "limo";
    private static final String NEXUS = "grouper";
    private static final String GLASS = "glass-1";

    private String currentDevice;

    private String[] deviceAddresses = new String[7];
    private static final int LAPTOP_INDEX = 0;
    private static final int DROIDX_INDEX = 1;
    private static final int GOGGLES_INDEX = 2;
    private static final int ARDUINO_INDEX=3;
    private static final int NEXUS_INDEX = 6;
    private String connectionAddress;

    public int messageIndex = 0;
    //public boolean isPhone = true;
    public boolean isServer = false;
    public boolean isConnectingToLaptop=true;
    TextView whoSays;
    GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (D)
            Log.i("debugging", "CURRENT DEVICE IS: "+ android.os.Build.DEVICE);
        enableBlueTooth();
        gestureDetector = new GestureDetector(this, this);
        registerDevices();


        if (currentDevice.equals("Goggles")) {
            setContentView(R.layout.goggles_main);
            //whoSays =(TextView)findViewById(R.id.who_says);
            //Log.i("debugging", "setting text");
            //whoSays.setText(mmSocket.getRemoteDevice().getName());

        } else {
            if (isController){
                setContentView(R.layout.controller);
                SeekBar slider = (SeekBar)findViewById(R.id.slider);
                slider.setOnSeekBarChangeListener(seekBarListener);
            }
            else if (isControlled){
                setContentView(R.layout.controller_target);

            }
            else {
                //DEFAULT BEHAVIOR
                setContentView(R.layout.activity_main);
                blueButton= (Button)findViewById(R.id.blue_button);
                blueButton.setOnClickListener(blueListener);
                if (!currentDevice.equals("Glass")){
                    ((TextView)findViewById(R.id.who_says)).setTextSize(40);
                    ((TextView)findViewById(R.id.incoming_message)).setTextSize(40);
                }
                //blueButton.setOnFocusChangeListener(blueFocusListener);
                //orangeButton.setOnFocusChangeListener(orangeFocusListener);
                orangeButton= (Button)findViewById(R.id.orange_button);
                orangeButton.setOnClickListener(orangeListener);
                //whoSays =(TextView)findViewById(R.id.who_says);
                //Log.i("debugging", "setting text");
                //whoSays.setText(mmSocket.getRemoteDevice().getName());

                incomingMessage = (TextView) findViewById(R.id.incoming_message);
            }
            Log.i("debugging", "hello anyone");
            checkSensors();


            //registerDevices();

        }
    }




    SensorManager mSensorManager;
    Sensor orientationSensor;
    Sensor gravitySensor;
    SensorEventListener gravityListener;
    SensorEventListener orientationListener;
    public void checkSensors(){

        mSensorManager= (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Log.i("debugging", "Sensors available include: ");
        SensorManager sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        gravitySensor=sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        for (Sensor s: sensorManager.getSensorList(Sensor.TYPE_ALL)){
            Log.i("debugging", " " + s.getName());
            if (s.getName().equals("MPL Orientation")){
                orientationSensor= s ;
            }
        }
    }











    public ConnectedThread connectedThread;

    public void startConnectionThread() {
        connectedThread = new ConnectedThread(mmSocket);
    }

    public void start(){
        if (D){
            Log.i("debugging", "in start");
        }
        if (isServer){
            ensureDiscoverable();
            initiateSocketServer();
        }
        else{
            initiateClient();
        }
    }

    public void registerDevices() {
        Log.i("debugging", "in my code");
        // deviceAddresses[LAPTOP_INDEX] = "E4:CE:8F:37:44:4F";
        deviceAddresses[LAPTOP_INDEX] = "B8:F6:B1:19:69:70";
        deviceAddresses[DROIDX_INDEX] = "D0:37:61:40:1F:F2";
        deviceAddresses[GOGGLES_INDEX] = "64:9C:8E:6B:02:D6";
        deviceAddresses[NEXUS_INDEX] = "10:BF:48:E8:EF:3A";
        deviceAddresses[ARDUINO_INDEX] = "00:A0:96:13:58:5E";


        if (deviceType.equals(DROIDX)) {
            currentDevice = "DroidX";
            //isServer=false;
            isController = true;
            //make sure server is visible
            connectionAddress = deviceAddresses[NEXUS_INDEX];

        }
        if (deviceType.equals(NEXUS)) {
            isServer=true;
            isControlled=true;
            currentDevice = "Nexus";
            //make sure server is visible
            connectionAddress = deviceAddresses[GOGGLES_INDEX];
        }
        if (deviceType.equals(GOGGLES)) {
            if (isConnectingToArduino){
                connectionAddress = deviceAddresses[ARDUINO_INDEX];
            }
            else {
                connectionAddress = deviceAddresses[NEXUS_INDEX];
            }
            currentDevice = "Goggles";

        }
        if (deviceType.equals(GLASS)){
            if (D)
                Log.i("debugging", "device type is glass");
            currentDevice = "Glass";
            if (isConnectingToArduino){
                Log.i("debugging", "connecting to arduino");
                connectionAddress = deviceAddresses[ARDUINO_INDEX];
            }
            else if (isConnectingToLaptop){
                Log.i("debugging", "connecting to ben's laptop");
                connectionAddress = deviceAddresses[LAPTOP_INDEX];
            }
            else {
                Log.i("debugging", "connecting to nexy");
                connectionAddress = deviceAddresses[NEXUS_INDEX];
            }
        }

        start();


    }

    @Override
    public void onStop() {
        super.onStop();
        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void initiateSocketServer() {
        if (D)
            Log.i("debugging", "initiating server socket");
        new AcceptThread().run();

    }

    public void initiateClient() {
        if (D)
            Log.i("debugging", "iniating clientt");
        queryDevices();
        if (mBluetoothAdapter.isDiscovering()) {
            if (D)
                Log.i("debugging", "canceled discovery in initiateClient");
            //mBluetoothAdapter.cancelDiscovery();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    OutputStream mmOutputStream;
    InputStream mmInputStream;


    OnSeekBarChangeListener seekBarListener = new OnSeekBarChangeListener(){

        @Override
        public void onProgressChanged(SeekBar arg0, int seekValue, boolean arg2) {
            // TODO Auto-generated method stub
            if (taskComplete) {
                Log.i("debugging", "clicked");
                try {
                    mmOutputStream = mmSocket.getOutputStream();
                    messageIndex++;
                    String msg = Integer.toString(seekValue);
                    byte[] send = msg.getBytes();
                    mConnectedThread.write(send);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }


        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

    };

    OnClickListener sendListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {

            if (taskComplete) {
                Log.i("debugging", "clicked");
                try {
                    mmOutputStream = mmSocket.getOutputStream();
                    messageIndex++;
                    String msg = currentDevice + " message " + messageIndex;

                    byte[] send = msg.getBytes();
                    mConnectedThread.write(send);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

    };

    OnClickListener orangeListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {

            if (taskComplete) {
                Log.i("debugging", "clicked");
                try {
                    mmOutputStream = mmSocket.getOutputStream();
                    messageIndex++;
                    String msg = "orange";

                    byte[] send = msg.getBytes();
                    mConnectedThread.write(send);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

    };



    OnFocusChangeListener orangeFocusListener = new OnFocusChangeListener() {

        @Override
        public void onFocusChange(View arg0, boolean focused) {
            // TODO Auto-generated method stub
            if (focused){
                if (taskComplete) {
                    Log.i("debugging", "clicked");
                    try {
                        mmOutputStream = mmSocket.getOutputStream();
                        messageIndex++;
                        String msg = "orange";

                        byte[] send = msg.getBytes();
                        mConnectedThread.write(send);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }



    };



    OnClickListener blueListener = new OnClickListener() {

        @Override
        public void onClick(View arg0) {

            if (taskComplete) {
                Log.i("debugging", "clicked");
                try {
                    mmOutputStream = mmSocket.getOutputStream();
                    messageIndex++;
                    String msg = "blue";

                    byte[] send = msg.getBytes();
                    mConnectedThread.write(send);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }

    };


    OnFocusChangeListener blueFocusListener = new OnFocusChangeListener() {

        @Override
        public void onFocusChange(View arg0, boolean focused) {
            // TODO Auto-generated method stub
            if (focused){
                if (taskComplete) {
                    Log.i("debugging", "clicked");
                    try {
                        mmOutputStream = mmSocket.getOutputStream();
                        messageIndex++;
                        String msg = "blue";
                        byte[] send = msg.getBytes();
                        mConnectedThread.write(send);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }

    };



    public void enableBlueTooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
        }
        if (!mBluetoothAdapter.isEnabled()) {
            // Device is not connected to BlueTooth
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    BluetoothDevice serverDevice;
    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            Log.i("debugging", "in broadcast receiver");
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (D) {
                    Log.i("debugging", "Device is: " + device.getName());
                    Log.i("debugging",
                            "Device address:  " + device.getAddress());
                }

                // Add the name and address to an array adapter to show in a
                // ListView
                if (device.getAddress().equals(connectionAddress)) {
                    // connectToDevice(device);
                    if (D)
                        Log.i("debugging", "started the connection task");
                    serverDevice = device;
                    mBluetoothAdapter.cancelDiscovery();
                    try {
                        new ConnectThread(serverDevice).run();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

            }
        }
    };

    public void queryDevices() {
        Log.i("debugging", "querying devices");
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter
                .getBondedDevices();
        // If there are paired devices
        boolean found = false;
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                Log.i("debugging", "in for, we want device address: "+ connectionAddress);
                if (D) {
                    Log.i("debugging", "Device is: " + device.getName());
                    Log.i("debugging",
                            "Device address:  " + device.getAddress());
                }

                // Add the name and address to an array adapter to show in a
                // ListView\
                if (device.getAddress().equals(connectionAddress)) {
                    // connectToDevice(device);
                    Log.i("debugging", "starting connection task");
                    found = true;
                    serverDevice = device;
                    try {
                        new ConnectThread(serverDevice).run();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                    // ConnectionTask cTask = new ConnectionTask(master);
                    // cTask.execute(device, null, null);

                }
            }

        }
        if (!found) {
            Log.i("debugging", "starting discovery");
            mBluetoothAdapter.startDiscovery();
        }

        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister
        // during onDestroy
        registered = true;

    }

    boolean registered = false;

    public void onDestroy() {
        super.onDestroy();

        if (mReceiver!=null) unregisterReceiver(mReceiver);
        if (mmSocket != null) {
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Stop all threads
         */

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }



    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            // Use a temporary object that is later assigned to mmServerSocket,
            // because mmServerSocket is final
            Log.i("debugging", "accept 1");
            mAcceptThread = this;
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client
                // code

                tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(
                        "dreamy", MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmServerSocket = tmp;
            if (D)
                Log.i("debugging", "instantiated Accept Thread");
        }

        public void run() {
            BluetoothSocket socket = null;
            if (D)
                Log.i("debugging", "in run of Accept Thread");
            // Keep listening until exception occurs or a socket is returned
            while (true) {
                try {
                    socket = mmServerSocket.accept();

                    mmSocket = socket;

                    // startConnectionThread();
                } catch (IOException e) {
                    Log.i("debugging", "exception in AcceptThread.run()");
                    Toast toast = Toast.makeText(getApplicationContext(), "the client never came to the party", Toast.LENGTH_SHORT);
                    toast.show();
                    e.printStackTrace();
                    break;
                }
                // If a connection was accepted
                if (socket != null) {
                    Log.i("debugging", "in accept thread we have a socket!");
                    // Do work to manage the connection (in a separate thread)
                    connected(socket);

                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) throws IOException,
        InterruptedException {

            // device.fetchUuidsWithSdp();
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;
            if (D)
                Log.i("debugging",
                        "Instantiating connect thread to " + device.getName());
            // Get a BluetoothSocket to connect with the given BluetoothDevice
            if (Build.VERSION.SDK_INT < 9) { // VK:
                // Build.Version_Codes.GINGERBREAD
                // is not accessible yet so
                // using raw int value
                // VK: 9 is the API Level integer value for Gingerbread
                if (D)
                    Log.i("debugging", "first build");
                try {
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            } else {
                // GOGGLES USE THIS
                if (D)
                    Log.i("debugging", "second build");
                Method m = null;
                try {

                    m = device.getClass().getMethod(
                            "createInsecureRfcommSocketToServiceRecord",
                            new Class[] { UUID.class });
                } catch (NoSuchMethodException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                try {

                    tmp = (BluetoothSocket) m.invoke(device, (UUID) MY_UUID);
                    tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }

            mConnectThread = this;
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection

            if (mBluetoothAdapter.isDiscovering()) {
                if (D)
                    Log.i("debugging", "canceled discovery in run");
                mBluetoothAdapter.cancelDiscovery();
            }

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();

                // startConnectionThread();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.i("debugging", "unable to connect in ConnectThread.run");
                Toast toast = Toast.makeText(getApplicationContext(), "the server is not available", Toast.LENGTH_SHORT);
                toast.show();
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }
            Log.i("debugging", "SUCCESSFULLY CONNECTED");

            connected(mmSocket);
            // Do work to manage the connection (in a separate thread)
            // manageConnectedSocket(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            /**
             * try { mmSocket.close(); } catch (IOException e) { }
             */
        }

    }

    public ConnectThread mConnectThread;

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mConnectedThread = this;
            mmSocket = socket;




            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.i("debugging",
                        "exception in instantiation of ConnectThread");
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            taskComplete = true;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    Log.i("debugging", "reading from inputstream");
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    Log.i("debugging", "obtaining message from handler");
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                    .sendToTarget();
                } catch (IOException e) {
                    Log.i("debugging", "disconnected", e);
                    restartConnection();
                    //make a Toast that says connection is lost 
                    // connectionLost();
                    // Start the service over to restart listening mode
                    // BluetoothChatService.this.start();
                    break;
                }
            }
        }

        public void read() {
            byte[] buffer = new byte[1024]; // buffer store for the stream
            int bytes; // bytes returned from read()

            try {
                // Read from the InputStream
                bytes = mmInStream.read(buffer);
                // Send the obtained bytes to the UI activity
                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                .sendToTarget();
            } catch (IOException e) {

            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            Log.i("debugging", "in connectedthread.write");
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            /**
             * try { mmSocket.close(); } catch (IOException e) { }
             */
        }
    }
    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void restartConnection() {
        if (D) Log.i("debugging", "restarting connection (doesn't make sense");

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}


        // Start the whole dance over again
        start();
    }


    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {

            case MESSAGE_WRITE:
                Log.i("debugging", "message write in handler");
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                break;
            case MESSAGE_READ:
                Log.i("debugging", "message read in handler");
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);

                if (isControlled){
                    LinearLayout mLayout = (LinearLayout) findViewById(R.id.progress);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Integer.parseInt(readMessage)*3);
                    mLayout.setLayoutParams(params);

                }
                else{
                    if (readMessage.equals("blue")){
                        incomingMessage.setTextColor(getResources().getColor(R.color.sky_light));
                    }
                    if (readMessage.equals("orange")){
                        incomingMessage.setTextColor(getResources().getColor(R.color.tangerine_light));
                    }
                    incomingMessage.setText(readMessage);
                }


                break;

            }
        }
    };

    private ConnectedThread mConnectedThread = null;
    private AcceptThread mAcceptThread = null;

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * 
     * @param socket
     *            The BluetoothSocket on which the connection was made
     */
    public synchronized void connected(BluetoothSocket socket) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one
        // device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        if (D)
            Log.i("debugging", "Starting connected thread");
        mConnectedThread.start();
        Log.i("debugging", "name of socket device is: " + mmSocket.getRemoteDevice().getName());
        //whoSays.setText(mmSocket.getRemoteDevice().getName());

    }


    private void ensureDiscoverable() {

        if(D) Log.i("debugging", "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 1000);
            startActivity(discoverableIntent);

        }
    }





    //==================== gesture detection ========================
    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        // Log.i("Gesture", event.toString());
        return true;
    }

    @Override
    public void onBackPressed() {
        Log.i("Gesture", "onBackPressed");
        Toast.makeText(getApplicationContext(), "Go Back", Toast.LENGTH_SHORT).show();
        super.onBackPressed();
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.i("Gesture", "onDown");
        return false;

    }


    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
            float velocityY) {
        Log.i("Gesture", "onFling: velocityX:" + velocityX + " velocityY:" + velocityY);
        if (velocityX < -3500) {
            Toast.makeText(getApplicationContext(), "Fling Right", Toast.LENGTH_SHORT).show();
        } else if (velocityX > 3500) {
            Toast.makeText(getApplicationContext(), "Fling Left", Toast.LENGTH_SHORT).show();
        }
        return true;
    }


    @Override
    public void onLongPress(MotionEvent e) {
        Log.i("Gesture", "onLongPress");

    }


    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
            float distanceY) {
    	String msg;
        if (distanceX < 0){
        	msg = "u\n";
            Log.i("Gesture", "onScroll going forward with distance : " + distanceX);	
        }
        else {
        	msg = "d\n";
            Log.i("Gesture", "onScroll going back");	
        }
        
        if (taskComplete) {
            try {
                mmOutputStream = mmSocket.getOutputStream(); 
                byte[] send = msg.getBytes();
                mConnectedThread.write(send);
            } catch (IOException d) {
                d.printStackTrace();
            }

        }
        return true;
    }


    @Override
    public void onShowPress(MotionEvent e) {
        Log.i("Gesture", "onShowPress");


    }


    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i("Gesture", "onSingleTapUp");
        
        if (taskComplete) {
            try {
                mmOutputStream = mmSocket.getOutputStream();
                String msg = "p\n";
                byte[] send = msg.getBytes();
                mConnectedThread.write(send);
            } catch (IOException d) {
                d.printStackTrace();
            }

        }
        return false;
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        // The light sensor returns a single value.
        // Many sensors return 3 values, one for each axis.
        
        float lux = event.values[0];
        //Log.i("debugging", "sensor type is: "+ event.sensor.getName() + ": " + lux);

        int currentSensor = event.sensor.getType();
        if (currentSensor== Sensor.TYPE_GRAVITY){

            //Log.i("debugging", "sensor type is: "+ event.sensor.getName() + ": " + lux);
        }


        // Do something with this sensor value.
    }

}
