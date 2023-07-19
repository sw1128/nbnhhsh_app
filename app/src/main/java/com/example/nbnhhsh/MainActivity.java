package com.example.nbnhhsh;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView textView = findViewById(R.id.textView4);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        EditText input = findViewById(R.id.searchInput);
        TextView result = findViewById(R.id.result);
        TextView add = findViewById(R.id.add);
        Button button = findViewById(R.id.searchBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
                result.setText("");
                add.setVisibility(View.GONE);
                hideKeyboard(input);
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText input = findViewById(R.id.searchInput);
                String inputText = input.getText().toString();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                LinearLayout linearLayout = new LinearLayout(MainActivity.this);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                linearLayout.setPadding(100, 50, 100, 40);
                EditText editText = new EditText(MainActivity.this);
                editText.setHint("末尾可通过括号简略注明来源");
                linearLayout.addView(editText);
                builder.setTitle(" 输入你对 " + inputText + " 的解释")
                        .setView(linearLayout)
                        .setPositiveButton("确定", (dialogInterface, i) -> {
                            String text = editText.getText().toString();
                            if (text.length() > 0) {
                                hideKeyboard(editText);
                                new Thread(() -> {
                                    addWord(inputText, text);
                                }).start();
                            } else {
                                Toast.makeText(MainActivity.this, "解释不能为空", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });
    }

    private void addWord(String data, String explain) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://lab.magiconch.com/api/nbnhhsh/translation/"+data);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            String param = "text=%s";
            param = String.format(param, explain);
            dos.writeBytes(param);
            int responseCode = connection.getResponseCode();
            Log.d("result111", String.valueOf(responseCode));
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "感谢您的提交，审核通过后将会显示", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "提交失败", Toast.LENGTH_SHORT).show();
            });
            Log.d("result111", e.toString());
            e.printStackTrace();
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void start() {
        new Thread(this::networkRequest).start();
    }

    private void networkRequest() {
        EditText input = findViewById(R.id.searchInput);
        TextView textView = findViewById(R.id.result);
        TextView add = findViewById(R.id.add);
        if (input.getText().toString().equals("")) {
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "输入为空", Toast.LENGTH_SHORT).show();
            });
            return;
        }
        String searchMsg = input.getText().toString();
        HttpURLConnection connection = null;
        try {
            URL url = new URL("https://lab.magiconch.com/api/nbnhhsh/guess");
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();
            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            String param = "text=%s";
            param = String.format(param, searchMsg);
            dos.writeBytes(param);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP error code" + responseCode);
            }
            String result = getStringByStream(connection.getInputStream());
            //转换成json对象
            JSONArray jsonArray = new JSONArray(result);
            String ArrayStr = "";
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                try {
                    ArrayStr = jsonObject.getString("trans") + " ";
                }
                catch (Exception e) {
                    runOnUiThread(() -> {
                        textView.setText("这个我也不知道啥意思 😭");
                        add.setVisibility(View.VISIBLE);
                    });
                }
            }
            JSONArray jsonArr = new JSONArray(ArrayStr);
            String[] stringArray = new String[jsonArr.length()];
            for (int i = 0; i < jsonArr.length(); i++) {
                stringArray[i] = jsonArr.getString(i);
            }
            String resultStr = "";
            for (String element : stringArray) {
                //如果是最后一个元素，不加逗号
                if (element.equals(stringArray[stringArray.length - 1])) {
                    resultStr += element;
                } else {
                    resultStr += element + " , ";
                }
            }
            Log.d("result111", resultStr);
            String finalResultStr = resultStr;
            runOnUiThread(() -> {
                textView.setText(finalResultStr);
                add.setVisibility(View.VISIBLE);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getStringByStream(InputStream inputStream) {
        Reader reader;
        try {
            reader = new InputStreamReader(inputStream, "UTF-8");
            char[] rawBuffer = new char[512];
            StringBuffer buffer = new StringBuffer();
            int length;
            while ((length = reader.read(rawBuffer)) != -1) {
                buffer.append(rawBuffer, 0, length);
            }
            return buffer.toString();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}