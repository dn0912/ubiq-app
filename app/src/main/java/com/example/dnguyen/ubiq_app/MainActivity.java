package com.example.dnguyen.ubiq_app;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    Button mSubscribe;
    TextView mItemSelected;
    String[] listItems;
    boolean[] checkedItems;
    ArrayList<Integer> mUserItems = new ArrayList<>();
    String EXCHANGE_NAME = "supermarkt_duc";
    Set<String> currentSelected = new HashSet<String>();
    Set<String> allItemsSet;
    HashMap<String, Product> prodMap;
    ArrayList<String> offerItems;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupConnectionFactory();

        mSubscribe = findViewById(R.id.btnSubscribe);
        mItemSelected = findViewById(R.id.tvItemSelected);

        offerItems = new ArrayList<>();

        prodMap = new HashMap<>();
        prodMap.put("Mascara", new Product("Mascara", "4.99 Euro", "Beauty"));
        prodMap.put("Banana", new Product("Banana", "1.99 Euro/Kg", "Fruits"));
        prodMap.put("Chicken", new Product("Chicken", "3.99 Euro", "Meat"));
        prodMap.put("Beer", new Product("Beer", "0.99 Euro", "Alcohol"));
        prodMap.put("Cola", new Product("Cola", "0.49 Euro", "Soft Drink"));

        listItems = prodMap.keySet().toArray(new String[prodMap.size()]);
        allItemsSet = new HashSet<String>(Arrays.asList(listItems));
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
                        currentSelected.clear();

                        for(int k = 0; k < mUserItems.size(); k++){
                            item = item + listItems[mUserItems.get(k)];
                            currentSelected.add(listItems[mUserItems.get(k)]);
                            // styling string from last item
                            if(k != mUserItems.size()-1){
                                item = item + ", ";
                            }
                        }

                        allItemsSet.removeAll(currentSelected);
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
                        Collections.addAll(allItemsSet, listItems);
                    }
                });

                AlertDialog mDialog = mBuilder.create();
                mDialog.show();
            }
        });

        view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Product currentSelectedProd = prodMap.get(offerItems.get(position));
                Snackbar.make(view, currentSelectedProd.getProduct_type()+" - "+currentSelectedProd.getName()+" current price: "+currentSelectedProd.getPrice(), Snackbar.LENGTH_LONG).setAction("No action", null).show();;
            }
        });

        @SuppressLint("HandlerLeak") final Handler incomingMsgHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String message = msg.getData().getString("msg");
                String listViewString = "New offer for " + message;
                if(!listElementsArrayList.contains(listViewString)){
                    listElementsArrayList.add(listViewString);
                    offerItems.add(message.split(":")[0]);
                    adapter.notifyDataSetChanged();
                }
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
                while (true) {
                    try {
                        Connection conn = factory.newConnection();
                        Channel channel = conn.createChannel();
                        channel.basicQos(1);
                        channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);
                        String queueName = channel.queueDeclare().getQueue();
                        for (String productName : currentSelected) {
                            channel.queueBind(queueName, EXCHANGE_NAME, "offers." + productName + ".*");
                        }

                        for (String productName : allItemsSet) {
                            channel.queueUnbind(queueName, EXCHANGE_NAME, "offers." + productName + ".*");
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
                                getChannel().basicAck(envelope.getDeliveryTag(), false);
                            }
                        };
                        channel.basicConsume(queueName, true, consumer);
                    } catch (Exception e) {
                        Log.e("subscribe", "Connection is broken:" + e.getClass().getName());
                        try{
                            // try to reconnect
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
