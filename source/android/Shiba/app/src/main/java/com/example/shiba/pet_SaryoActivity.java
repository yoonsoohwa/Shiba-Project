package com.example.shiba;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.support.v7.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static android.os.StrictMode.setThreadPolicy;

public class pet_SaryoActivity extends AppCompatActivity {
    TextView textV2, select_v, set_price_v;
    Button bt_plus, bt_minus;
    ImageButton bt_cart;
    String price, item, total_price, temp;
    int count;
    int order;

    final Context context = this;
    ArrayList<String> nameList, priceList, gradeList;

    SocketChannel socketChannel = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pet_activity_saryo);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        setThreadPolicy(policy);

        textV2 = (TextView)findViewById(R.id.counting_textline);
        select_v = (TextView)findViewById(R.id.select_saryo);
        set_price_v = (TextView)findViewById(R.id.set_price_v);
        bt_plus = (Button)findViewById(R.id.plus);
        bt_minus = (Button)findViewById(R.id.minus);
        bt_cart=(ImageButton)findViewById(R.id.cart);
        LinearLayout linear = (LinearLayout) findViewById(R.id.Left_layout);

        count=1;
        textV2.setText(""+count);

        nameList = new ArrayList<>();
        priceList = new ArrayList<>();
        gradeList = new ArrayList<>();

        socketConnect("192.168.13.17", 8402);    //소켓 연결
        sendData("s");      //사료 테이블 요청

        Button btn[] = new Button[nameList.size()]; //버튼 추가
        for(int i=0; i<btn.length; i++) {
            btn[i] = new Button(this);
            btn[i].setText(nameList.get(i));
            btn[i].setId(i);
            linear.addView(btn[i]);

            final int finalI = i;
            btn[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select_v.setText(nameList.get(finalI));
                    set_price_v.setText(priceList.get(finalI).replaceAll(",",""));
                    order = finalI;
                    count = 1;
                    textV2.setText("" + count);
                }
            });
        }
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                select_v.setText(nameList.get(0));
                set_price_v.setText(priceList.get(0).replaceAll(",",""));
            }
        }, 1000);
    }

    public void onClick(final View v) {
        price = priceList.get(order).replaceAll(",", "");
        switch (v.getId()){
            case R.id.plus:
                count++;

                if(count > 10){
                    count = 10;
                    Toast.makeText(this, "한 번에 10개 이상 구매할 수 없어요..", Toast.LENGTH_SHORT).show();
                }
                textV2.setText(""+count); //그대로 넣으면 문자열로 인식 X 그래서 큰따옴표 붙여줌
                set_price_v.setText("" + Integer.parseInt(price)*count);
                break;

            case R.id.minus:
                count--;

                if(count < 1){
                    Toast.makeText(this, "최소 하나 이상 구입해주세요!", Toast.LENGTH_SHORT).show();
                    count = 1;
                }

                textV2.setText(""+count);
                set_price_v.setText("" + Integer.parseInt(price)*count);
                break;

            case R.id.cart:
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {

                    @Override
                    public void run() {

                        item = select_v.getText().toString();
                        total_price = set_price_v.getText().toString();
                        temp = String.valueOf(count);

                        ArrayList cart_name_list = getIntent().getStringArrayListExtra("cart_name_list");
                        ArrayList cart_count_list = getIntent().getStringArrayListExtra("cart_count_list");
                        ArrayList cart_price_list = getIntent().getStringArrayListExtra("cart_price_list");

                        cart_name_list.add(item);
                        cart_count_list.add(temp);
                        cart_price_list.add(total_price);

                        Intent intent = new Intent(pet_SaryoActivity.this, pet_CartActivity.class);
                        intent.putExtra("cart_name_list", cart_name_list); //putextra로 cart액티비티에 넘김
                        intent.putExtra("cart_price_list", cart_price_list);
                        intent.putExtra("cart_count_list", cart_count_list);

                        startActivity(intent);
                        finish();   //화면 종료
                    }
                }, 500); //0.5초 후 카트액티비티로 넘어감
        }
    }

    public void sendData(String data){      //데이터 발신
        Log.d("Send", data);

        try {
            ByteBuffer buffer = null;
            Charset charset = Charset.forName("UTF-8");
            buffer = charset.encode(data);

            socketChannel.write(buffer);

            buffer = ByteBuffer.allocate(15000);
            int len = socketChannel.read(buffer);
            buffer.flip();

            String recive = charset.decode(buffer).toString();
            ReceiveData(recive);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void socketConnect(String host, int port){
        //연결 부분
        try{
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            socketChannel.connect(new InetSocketAddress(host, port));

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        if(socketChannel.isOpen()){
            try {
                socketChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.finish();
    }
    public void ReceiveData(String recive){     //데이터를 받으면

        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(recive);
            for(int i = 0; i<jsonArray.length(); i++){
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String price = jsonObject.getString("price");
                String grade = jsonObject.getString("grade");
                nameList.add(name);
                priceList.add(price);
                gradeList.add(grade);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    public void onBackPressed(){
        Intent intent = new Intent(pet_SaryoActivity.this, pet_MarketActivity.class);
        startActivity(intent);  //마켓으로 이동
        finish();
    }
}
