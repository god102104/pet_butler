package com.example.server_side;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity  {
    //initializes all the private properties
    //For any server the ServerSocket and the Socket corresponding to the temp client
    // to be activated must be initialized
    private ServerSocket serverSocket;
    private Socket tempClientSocket;
    public static Context mContext;

    //here it sets the Thread initially to null
    Thread serverThread = null;

    //the SERVER_PORT is initialized which must correspond to the port of the client
    public static final int SERVER_PORT = 5050;

    //the msgList is initialized corresponding to the Linearlayout
    private LinearLayout msgList;
    private Handler handler;
    private int greenColor;
    private TextView text_ip;

// 지니 추가
    private static final String TAG =  "MainActivity";
    public static String TEST_TYPE       = "TEST_TYPE";
    private String[] REQUIRED_PERMISSIONS  = {  Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //This sets the initial content view that would be displayed
        setContentView(R.layout.activity_main);
        setTitle("펫집사");

        //서버 스레드 시작
        this.serverThread = new Thread(new ServerThread());
        this.serverThread.start();

        //지니 인증
        signatureTest();

        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        File dataDirectory = new File(rootPath+"/aisdk/");

        if(!dataDirectory.exists()){
            if(dataDirectory.mkdirs()) {
                Log.i(TAG, "dataDirectory make success");
            }
        }
// 와이파이 ip 가져오기
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress;
        formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        text_ip = findViewById(R.id.ip);
        text_ip.setText(formatedIpAddress);

        //initializes the identifier greenColor to be used anywhere within this file
        greenColor = ContextCompat.getColor(this, R.color.green);

        //initializes a new handler for message queueing
        handler = new Handler();
        msgList = findViewById(R.id.msgList);
    }

    //지니 추가
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart >> ");
        checkPermit();
    }

    private void checkPermit() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permit = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            int permitWrite = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            int permitRecord = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
            if (permit != PackageManager.PERMISSION_GRANTED  || permitWrite != PackageManager.PERMISSION_GRANTED || permitRecord != PackageManager.PERMISSION_GRANTED )  {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[2])) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getString(R.string.require_permission));
                    builder.setPositiveButton(getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    ActivityCompat.requestPermissions( MainActivity.this, REQUIRED_PERMISSIONS,0);
                                }
                            });
                    builder.show();
                } else {
                    ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,0);
                }
            }
        }
    }

    public void onClick_Genie(View v) {
        switch (v.getId()) {

            case R.id.STTOnHTTP01:
            {
                Intent intent = new Intent(this, SttRestActivity.class);
                intent.putExtra(TEST_TYPE, 0);
                startActivity(intent);
            }
            break;
            case R.id.STTOnHTTP02:
            {
                Intent intent = new Intent(this, SttRestActivity.class);
                intent.putExtra(TEST_TYPE, 1);
                startActivity(intent);
            }
            break;
            case R.id.TTSOnHTTP01:
                startActivity( new Intent(this, TTSActivity.class) );
                break;
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionsResult >> ");
        boolean permit = true;
        for (int grant : grantResults) {
            if (grant != PackageManager.PERMISSION_GRANTED) {
                permit = false;
            }
        }
        if (!permit) {
            Log.d(TAG, "no permit >> ");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[2])) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.denied_permission_finish));
                builder.setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                builder.show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.denied_permission_finish));
                builder.setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        });
                builder.show();
            }
        }
    }
    /////////////////////지니 추가///////////////

    //method to implement the different Textviews widget and display the message on
    //the Scrollview LinearLayout...
    public TextView textView(String message, int color, Boolean value) {

        //it checks if the message is empty then displays empty message
        if (null == message || message.trim().isEmpty()) {
            message = "<Empty Message>";
        }
        TextView tv = new TextView(this);
        tv.setTextColor(color);
        tv.setText(message + " [" + getTime() +"]");
        tv.setTextSize(20);
        tv.setPadding(0, 5, 0, 0);
        if (value) {
            tv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
        }
        return tv;
    }

    //showMessage method to handle posting of mesage to the textView
    public void showMessage(final String message, final int color, final Boolean value) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                msgList.addView(textView(message, color, value));
            }
        });
    }

    //onClick method to handle clicking events whether to start up the  server or
    //send a message to the client

    public void goFind(){
        // 찾아줘! 버튼 클릭 시 화면 이동
        Intent intent = new Intent(MainActivity.this, SubActivity.class);
        startActivity(intent);
    }

    public void onClick(View view) {
        if (view.getId() == R.id.btn_find) {
            String msg = "Find";
            showMessage(msg, Color.BLUE, false);
            if (msg.length() > 0) {
                sendMessage(msg);
                // 찾는 화면으로 이동
                goFind();
            }
            return;
        }
        if (view.getId() == R.id.btn_check) {
            String msg = "Check";
            showMessage(msg, Color.BLUE, false);
            if (msg.length() > 0) {

                sendMessage(msg);
            }
            return;
        }
        if (view.getId() == R.id.btn_alpha) {
            String msg = "Alpha";
            showMessage(msg, Color.BLUE, false);
            if (msg.length() > 0) {

                sendMessage(msg);
            }
            return;
        }
    }
    //method implemented to send message to the client
    private void sendMessage(final String message) {
        try {
            if (null != tempClientSocket) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrintWriter out = null;
                        try {
                            out = new PrintWriter(new BufferedWriter(
                                    new OutputStreamWriter(tempClientSocket.getOutputStream())),
                                    true);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        out.println(message);
                    }
                }).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* serverthread method implemented here to activate the server network */
    class ServerThread implements Runnable {

        public void run() {
            Socket socket;
            try {
                serverSocket = new ServerSocket(SERVER_PORT);

                //deactivates the visibility of the button
//               Button button = (Button) findViewById(R.id.start_server);
//               button.setVisibility(View.GONE);
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Starting Server : " + e.getMessage(), Color.RED, false);
            }

            //communicates to client and displays error if communication fails
            if (null != serverSocket) {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        socket = serverSocket.accept();
                        CommunicationThread commThread = new CommunicationThread(socket);
                        new Thread(commThread).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                        showMessage("Error Communicating to Client :" + e.getMessage(), Color.RED, false);
                    }
                }
            }
        }
    }

    /* communicationThread class that implements the runnable class to communicate with the client */
    class CommunicationThread implements Runnable {

        private final Socket clientSocket;

        private BufferedReader input;

        public CommunicationThread(Socket clientSocket) {
            this.clientSocket = clientSocket;
            tempClientSocket = clientSocket;
            try {
                this.input = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                showMessage("Error Connecting to Client!!", Color.RED, false);
            }
            showMessage("Connected to Client!!", greenColor, true);
// 디버그 시 문제 생겨서 주석처리 이게 문제가 될지?? 확인해봐야할듯
//            setContentView(R.layout.activity_main);
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    //checks to see if the client is still connected and displays disconnected if disconnected
                    String read = input.readLine();
                    if (null == read || "Disconnect".contentEquals(read)) {
                        Thread.interrupted();
                        read = "Offline....";
                        showMessage("Client : " + read, greenColor, true);
                        break;
                    } else {
                        showMessage("Client : " + read, greenColor, true);
                        if(read.equals("Founded")){
                            AlertDialog.Builder msgBuilder = new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("찾았어요~") .setMessage("지금 보러가실래요?") .setPositiveButton("보러가요", new DialogInterface.OnClickListener() {
                                        @Override public void onClick(DialogInterface dialogInterface, int i) { goFind(); } })
                                    .setNegativeButton("닫기", new DialogInterface.OnClickListener() { @Override public void onClick(DialogInterface dialogInterface, int i)
                                    { Toast.makeText(MainActivity.this, "닫아요~", Toast.LENGTH_SHORT).show(); } });

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog msgDlg = msgBuilder.create(); msgDlg.show();
                                }
                            });

                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

    }

    //getTime method implemented to format the date into H:m:s
    String getTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(new Date());
    }

    //personally described onDestroy method to disconnect from the network on destroy of the activity
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != serverThread) {
            sendMessage("Disconnect");
            serverThread.interrupt();
            serverThread = null;
        }
    }

    //지니 SDK 추가
    public String getTimeStamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        Date today = new Date();
        String timestamp = simpleDateFormat.format(today);
        return timestamp;
    }

    public String makeSignature(String timestamp, String client_id, String client_secret) {
        String digest = null;
        try {
            SecretKeySpec key = new SecretKeySpec((client_secret).getBytes("UTF-8"), "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            String digestTarget = client_id + ":" +timestamp;
            byte[] bytes = mac.doFinal(digestTarget.getBytes("ASCII"));
            StringBuffer hash = new StringBuffer();
            for(int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(0xFF & bytes[i]);
                if(hex.length() == 1) hash.append('0');
                hash.append(hex);
            }
            digest = hash.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }

        return digest;
    }

    public void signatureTest() {
        String client_id = "36386697-3eb6-4250-9899-11d717f06aa1";
        String timestamp = getTimeStamp();
        String client_secret = "8c1f89068be2e1f775f6373a17afa5fcda04ffbd3418898f4c140e76ed6f7973";
        System.out.println("Signature:"+ makeSignature (client_id, timestamp, client_secret));
    }
}
