package com.example.domin.myapplication;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.util.Calendar;

public class MainActivity extends Activity {
    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter = 0;
    int count = 0;
    volatile boolean stopWorker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button shadowAus = findViewById(R.id.ShadowAus);
        Button shadow = findViewById(R.id.Shadow);
        Button dunkelTimer = findViewById(R.id.DunkelTimer);
        Button manuell = findViewById(R.id.Manuell);
        Button timer = findViewById(R.id.Timer);
        Button ferienAus = findViewById(R.id.FerienAus);
        Button ferienEin = findViewById(R.id.FerienEin);
        Button setDate = findViewById(R.id.button4);
        Button stop = findViewById(R.id.button3);
        Button hoch = findViewById(R.id.button);
        Button runter = findViewById(R.id.button2);
        Button openButton = (Button)findViewById(R.id.open);
        Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        myLabel = (TextView)findViewById(R.id.label);
        myTextbox = (EditText)findViewById(R.id.entry);

        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });

        shadow.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendCommand("W");
                }
                catch (IOException ex) { }
            }
        });

        shadowAus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("w");
                } catch (IOException e) {}
            }
        });

        dunkelTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("D");
                } catch (IOException e) {}
            }
        });

        timer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendCommand("T");
                }
                catch (IOException ex) { }
            }
        });

        manuell.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendCommand("M");
                }
                catch (IOException ex) { }
            }
        });

        ferienAus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("f");
                } catch (IOException e) {}
            }
        });

        ferienEin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("F");
                } catch (IOException e) {}
            }
        });

        hoch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("H");
                } catch (IOException e) {}
            }
        });

        runter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("R");
                } catch (IOException e) {}
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("S");
                } catch (IOException e) {}
            }
        });

        runter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    sendCommand("R");
                } catch (IOException e) {}
            }
        });



        //Send Button
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    sendData();
                }  catch (IOException ex) { }
            }
        });

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    closeBT();
                }  catch (IOException ex) { }
            }
        });
    }

    void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null) {
            myLabel.setText("No bluetooth adapter available");
        }

        if(!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                if(device.getName().equals("WORO")) {
                    mmDevice = device;
                    break;
                }
            }
        }
        myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++) {
                                byte b = packetBytes[i];
                                if(b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    handler.post(new Runnable() {
                                        public void run() {
                                            myLabel.setText(data);
                                        }
                                    });
                                }
                                else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendCommand(String s) throws IOException {
        mmOutputStream.write(s.getBytes());
    }

    void sendData() throws IOException {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }

    void closeBT() throws IOException {
        stopWorker = true;
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }

    String[] getCurrentTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String strDate = sdf.format(c.getTime());
        return strDate.split(":");
    }

    void setTime() {
        String[] ar = getCurrentTime();

        try {
            sendCommand("");
        } catch (IOException e) {}
    }
}