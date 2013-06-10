package com.example.helloworld;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;

public class ConnectionTask extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {

	//int REQUEST_ENABLE_BT;
	private BluetoothSocket mmSocket;
	BluetoothAdapter mBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();
	ArrayAdapter mArrayAdapter;
	BluetoothDevice laptop;
	static int sdk = Integer.parseInt(Build.VERSION.SDK);
	Button sendButton;
	MainActivity master;
	private static final UUID MY_UUID = 
	   UUID.fromString((sdk<=8||sdk>=11)?"04c6093b-0000-1000-8000-00805f9b34fb":"00001101-0000-1000-8000-00805F9B34FB");

	public ConnectionTask(MainActivity m){
		master=m;
		
	}
	@Override
	protected BluetoothSocket doInBackground(BluetoothDevice... devices) {
		Log.i("debugging", "in do in background");
		connectToDevice(devices[0]);
		//queryDevices();
		// TODO Auto-generated method stub
		return mmSocket;
	}
	


	public void connectToDevice(BluetoothDevice device){
		Log.i("debugging", "connecting to " + device.getName());
		
		//make TUNA discoverable
		/**Intent discoverableIntent = new
				Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
				discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
		startActivity(discoverableIntent);*/
		new ConnectThread(device).run();
		
	}

	private class ConnectThread extends Thread {
	    //private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	    
	    OutputStream mmOutputStream;
        InputStream mmInputStream;
        
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
	    public void cancel() {
	        try {
	        	Log.i("debugging", "cancel called, socket closed");
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	    
	    public void write(String msg)
	{
	    	try {
				mmOutputStream.write(msg.getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    	}
	}
	@Override
	   protected void onPostExecute(BluetoothSocket result) {
		   Log.i("debugging", "on post execute");
		   master.makeToast("connected successfully with laptop");
		   master.setSocket(mmSocket);
		   master.setTaskComplete(true);
	     }
}
