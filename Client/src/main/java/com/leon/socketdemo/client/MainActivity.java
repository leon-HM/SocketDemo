package com.leon.socketdemo.client;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.leon.socketdemo.client.utils.MyUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int MESSAGE_RECEIVE_NEW_MSG =1;
    private static final int MESSAGE_SOCKET_CONNECTED = 2;

    private TextView mTVMessage;
    private Button mBTNSend;
    private EditText mETMessage;

    private PrintWriter mPrintWriter;
    private Socket mClientSocket;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case MESSAGE_RECEIVE_NEW_MSG:
                    mTVMessage.setText(mTVMessage.getText()+(String)msg.obj);
                break;
                case MESSAGE_SOCKET_CONNECTED:
                    mBTNSend.setEnabled(true);
                break;
                default:
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mTVMessage = (TextView) findViewById(R.id.mTV);
        mBTNSend = (Button) findViewById(R.id.mBTN);
        mETMessage = (EditText) findViewById(R.id.mET);

        mBTNSend.setEnabled(false);
        mBTNSend.setOnClickListener(this);

        new Thread(){
            @Override
            public void run() {
                connectTCPServer();
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mClientSocket!=null){
            try{
                mClientSocket.shutdownInput();
                mClientSocket.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void connectTCPServer() {
        Socket socket = null;
        while(socket==null){
            try{
                socket = new Socket("localhost",8688);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),true);
                mHandler.sendEmptyMessage(MESSAGE_SOCKET_CONNECTED);
            }catch (Exception e){
                SystemClock.sleep(1000);
                System.out.println("connect tcp server failed,retry ...");
            }
        }

        try{
            //接收服务器端消息
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while(!MainActivity.this.isFinishing()){
                String msg = br.readLine();
                System.out.println("receive:" + msg);
                if(msg!=null){
                    String time = fomartDateTime(System.currentTimeMillis());
                    final String showedMsg = "server "+time+":"+msg+"\n";
                    mHandler.obtainMessage(MESSAGE_RECEIVE_NEW_MSG,showedMsg).sendToTarget();
                }
            }

            System.out.println("quit...");
            MyUtils.close(mPrintWriter);
            MyUtils.close(br);
            socket.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.mBTN:
                //发送
                final String msg = mETMessage.getText().toString();
                if(!TextUtils.isEmpty(msg) && mPrintWriter!=null){
                    mPrintWriter.println(msg);
                    mETMessage.setText("");
                    String time = fomartDateTime(System.currentTimeMillis());
                    final String showedMsg = "self "+ time + ":" + msg +" \n";
                    mTVMessage.setText(mTVMessage.getText()+showedMsg);
                }
            break;
        }
    }

    private String fomartDateTime(long time) {
        return new SimpleDateFormat("(HH:mm:ss)").format(time);
    }
}
