package com.example.dnguyen.ubiq_app;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Button mSubscribe;
    TextView mItemSelected;
    String[] listItems;
    boolean[] checkedItems;
    ArrayList<Integer> mUserItems = new ArrayList<>();
    String EXCHANGE_NAME = "supermarkt_duc";

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupConnectionFactory();

        mSubscribe = findViewById(R.id.btnSubscribe);
        mItemSelected = findViewById(R.id.tvItemSelected);

        listItems = getResources().getStringArray(R.array.subscription_item);
        checkedItems = new boolean[listItems.length];

        ListView view = (ListView)findViewById(R.id.offerListView);
        view.setEmptyView(findViewById(R.id.empty_list_item));

        final List< String > listElementsArrayList = new ArrayList<String>();
        final ArrayAdapter< String > adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listElementsArrayList);
        view.setAdapter(adapter);

        mSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                mBuilder.setTitle(R.string.dialog_title);
                mBuilder.setMultiChoiceItems(listItems, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position, boolean isChecked) {
                        if(isChecked){
                            mUserItems.add(position);
                        }else{
                            mUserItems.remove((Integer.valueOf(position)));
                        }
                    }
                });

                mBuilder.setCancelable(false);
                mBuilder.setPositiveButton(R.string.ok_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String item = "";

                        for(int k = 0; k < mUserItems.size(); k++){
                            item = item + listItems[mUserItems.get(k)];
                            // styling string from last item
                            if(k != mUserItems.size()-1){
                                item = item + ", ";
                            }
                        }

                        mItemSelected.setText(item);
                    }
                });

                mBuilder.setNegativeButton(R.string.back_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                mBuilder.setNeutralButton(R.string.clear_all_label, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int position) {
                        for(int i = 0; i < checkedItems.length; i++){
                            checkedItems[i] = false;
                            mUserItems.clear();
                            mItemSelected.setText("");
                        }
                    }
                });

                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });

        @SuppressLint("HandlerLeak") final Handler incomingMsgHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");
                listElementsArrayList.add("New offer for " + ' ' + message);
                adapter.notifyDataSetChanged();
            }
        };
        subscribe(incomingMsgHandler);
    }

    Thread subscribeThread;

    protected void onDestroy() {
        super.onDestroy();
        subscribeThread.interrupt();
    }

    ConnectionFactory factory = new ConnectionFactory();
    private void setupConnectionFactory() {
        factory.setUsername("master");
        factory.setPassword("master");
        factory.setHost("155.54.204.46");
        factory.setPort(5672);
    }

    void subscribe(final Handler handler) {
        subscribeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Connection conn = factory.newConnection();
                        Channel channel = conn.createChannel();
                        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                        String queueName = channel.queueDeclare().getQueue();
                        for(int i = 0; i < mUserItems.size(); i++) {
                            String productName = listItems[mUserItems.get(i)];
                            channel.queueBind(queueName, EXCHANGE_NAME, "offers." + productName +".*");
                            Log.e("queueBind", "offers." + productName +".*");
                        }

                        Consumer consumer = new DefaultConsumer(channel) {
                            @Override
                            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                                String message = new String(body, "UTF-8");
                                System.out.println(" [x] Received '" + message + "'");
                                Message msg = handler.obtainMessage();
                                Bundle bundle = new Bundle();

                                bundle.putString("msg", message);
                                msg.setData(bundle);
                                handler.sendMessage(msg);
                                Log.e("handleDelivery", message);
                            }
                        };

                        channel.basicConsume(queueName, true, consumer);
                    } catch (Exception e) {
                        Log.e("subscribe", "Connection is broken:" + e.getClass().getName());
                        try {
                            // try to reconnect again
                            Thread.sleep(4000);
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        subscribeThread.start();
    }
}
