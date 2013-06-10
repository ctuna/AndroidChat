package com.example.helloworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
//from http://www.drdobbs.com/tools/hacking-for-fun-programming-a-wearable-a/240007471
// MAC Address:	e4:ce:8f:37:44:4e


public class MainActivity extends Activity {
	MainActivity master = this;
	int REQUEST_ENABLE_BT;
	BluetoothAdapter mBluetoothAdapter;
	ArrayAdapter mArrayAdapter;
	BluetoothDevice laptop;
	static int sdk = Integer.parseInt(Build.VERSION.SDK);
	Button sendButton;
	BluetoothSocket mmSocket;
	private boolean taskComplete = false; 
	private static final UUID MY_UUID = 
	   UUID.fromString((sdk<=8||sdk>=11)?"04c6093b-0000-1000-8000-00805f9b34fb":"00001101-0000-1000-8000-00805F9B34FB");
	
	
	
	
	
	
	
	//from BluetoothChat
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
   
    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private TextView incomingMessage;
    
    // Device info
    private String deviceType = android.os.Build.DEVICE;
    private static final String DROIDX = "cdma_shadow";
    private static final String GOGGLES = "limo";
    private String currentDevice;
    
    private String[] deviceAddresses = new String[3];
    private static final int LAPTOP_INDEX = 0;
    private static final int DROIDX_INDEX = 1;
    private static final int GOGGLES_INDEX = 2;
    private String connectionAddress;

    
    
    
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            /**case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                    mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    setStatus(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    setStatus(R.string.title_not_connected);
                    break;
                }
                break;*/
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
               // mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
                
              //  mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                break;
            /**case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;*/
            }
        }
    };

    BluetoothChatService mChatService = new BluetoothChatService(this, mHandler);
    StringBuffer mOutStringBuffer;
    public int messageIndex=0;
	//UUID MY_UUID =  UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		registerDevices();
		sendButton = (Button) findViewById(R.id.send_button);
		sendButton.setOnClickListener(sendListener);
		incomingMessage = (TextView) findViewById(R.id.incoming_message);
		
		enableBlueTooth();
		
		if (D) Log.i("debugging", "bluetooth enabled");
		queryDevices();
		if (D) {
			Log.i("debugging", "devices queried");
			if (deviceType.equals(DROIDX)){
				Log.i("debugging", "called by DroidX");
			}
			else if (deviceType.equals(GOGGLES)){
				Log.i("debugging", "called by Goggles");
			}
			else {
				Log.i("debugging","Unfamiliar device /n Device : "+ deviceType);
			}
		
		}
		mOutStringBuffer = new StringBuffer("");
		
	
	    
	    
	    
	}
		public void registerDevices(){
			deviceAddresses[LAPTOP_INDEX] = "E4:CE:8F:37:44:4F";
		    deviceAddresses[DROIDX_INDEX] = "D0:37:61:40:1F:F2";
		    deviceAddresses[GOGGLES_INDEX] = "64:9C:8E:6B:02:D6";
		    
			if (deviceType.equals(DROIDX)) 
				{
				currentDevice="DroidX";
				connectionAddress = deviceAddresses[GOGGLES_INDEX];
				}
			if (deviceType.equals(GOGGLES)) {
				currentDevice="Goggles";
				connectionAddress = deviceAddresses[DROIDX_INDEX];
			}
			if (D) Log.i("debugging", "connecting to address: " + connectionAddress);
		}
		
		  /**
	     * Sends a message.
	     * @param message  A string of text to send.
	     */
	    private void sendMessage(String message) {
	        // Check that we're actually connected before trying anything
	        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
	            //Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
	            return;
	        }

	        // Check that there's actually something to send
	        if (message.length() > 0) {
	            // Get the message bytes and tell the BluetoothChatService to write
	            byte[] send = message.getBytes();
	            mChatService.write(send);

	            // Reset out string buffer to zero and clear the edit text field
	            mOutStringBuffer.setLength(0);
	            mOutEditText.setText(mOutStringBuffer);
	        }
	    }

		
	

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	
	public void setTaskComplete(boolean complete){
		//called by ConnectionTask 
		taskComplete = complete;
	}
	public void makeToast(String msg){
		//called by ConnectionTask 
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
		toast.show();
	}	
	
	public void setSocket(BluetoothSocket s){
		//called by ConnectionTask 
		Log.i("debugging", "received socket from ConnectionTask");
		mmSocket=s;
	}
	
	OnClickListener sendListener = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			 OutputStream mmOutputStream;
		     InputStream mmInputStream;
		     StringBuffer mOutStringBuffer;
		  
			 
			if (taskComplete){
				   try {
						mmOutputStream = mmSocket.getOutputStream();
						messageIndex++;
						String msg = deviceType + " message " + messageIndex;
						byte[] send = msg.getBytes();
						//mmOutputStream.write(send);
						sendMessage(msg);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
			Log.i("debugging", "clicked");
			
		}
		}
		
	};
	
	
	public void enableBlueTooth(){
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
		    // Device does not support Bluetooth
		}
		if (!mBluetoothAdapter.isEnabled()) {
			//Device is not connected to BlueTooth
		    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
	}
	 
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		    public void onReceive(Context context, Intent intent) {
		        String action = intent.getAction();
		        // When discovery finds a device
		        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
		            // Get the BluetoothDevice object from the Intent
		            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
		            if (D){
		            	Log.i("debugging", "Device is: "+ device.getName());
		            	Log.i("debugging", "Device address:  " + device.getAddress());
		            }
		            
		            // Add the name and address to an array adapter to show in a ListView
		            if (device.getAddress().equals(connectionAddress)){
			    		//connectToDevice(device);
		            	if (D) Log.i("debugging", "started the connection task");
		            	ConnectionTask cTask = new ConnectionTask(master);
			    		cTask.execute(device, null, null);
			    		
			    	}
		            
		            
		        }
		    }
		};
	public void queryDevices(){
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		// If there are paired devices
		boolean found = false;
		if (pairedDevices.size() > 0) {
		    // Loop through paired devices
		    for (BluetoothDevice device : pairedDevices) {
		    	  if (D){
		            	Log.i("debugging", "Device is: "+ device.getName());
		            	Log.i("debugging", "Device address:  " + device.getAddress());
		            }
		    	
		        // Add the name and address to an array adapter to show in a ListView\
		    	  if (device.getAddress().equals(connectionAddress)){
		    		//connectToDevice(device);
		    		  Log.i("debugging", "starting connection task");
		    		  found = true;
		    		  ConnectionTask cTask = new ConnectionTask(master);
		    		  cTask.execute(device, null, null);
		    		
		    	}
		    }
		    if (!found){
		    	Log.i("debugging", "starting discovery");
		    	 mBluetoothAdapter.startDiscovery();
		    }
		}
		
		 
			
			// Register the BroadcastReceiver
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
						
		
	}
	
	public void onDestroy(){
		super.onDestroy();
		unregisterReceiver(mReceiver);
	}
	
	/**public void connectToDevice(BluetoothDevice device){
		Log.i("debugging", "connecting to " + device.getName());
		
		//make TUNA discoverable
		/**Intent discoverableIntent = new
				Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivity(discoverableIntent);
		new ConnectThread(device).run();
		Toast toast = Toast.makeText(this, "connected successfully with laptop", Toast.LENGTH_LONG);
        
		toast.show();

	}

	private class ConnectThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	    
	   
	    public ConnectThread(BluetoothDevice device) {
	    	Log.i("debugging", "instantiating ConnectThread to "+ device.getName());
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	 
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	           // tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	            Method m = device.getClass().getMethod("createRfcommSocket", new Class[] {int.class});
	            tmp = (BluetoothSocket) m.invoke(device, 1);
	        
	        }  catch (NoSuchMethodException e) {
	        	
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        mBluetoothAdapter.cancelDiscovery();
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	            mmOutputStream = mmSocket.getOutputStream();
	            mmInputStream = mmSocket.getInputStream();
	            Log.i("debugging", "connected successfully");
	        } catch (IOException connectException) {
	        	Log.i("debugging", "exception when trying to connect");
	        	connectException.printStackTrace();
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	        //manageConnectedSocket(mmSocket);
	    }
	 
	    /** Will cancel an in-progress connection, and close the socket */
	   


}
