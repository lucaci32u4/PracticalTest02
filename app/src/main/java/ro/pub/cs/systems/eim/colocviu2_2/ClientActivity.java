package ro.pub.cs.systems.eim.colocviu2_2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ClientActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        Client client = new Client("localhost", "8181");
        client.connect();

        findViewById(R.id.bget).setOnClickListener(v -> {
            client.get(((EditText) findViewById(R.id.key)).getText().toString(), s -> {
                runOnUiThread(() -> {
                    ((TextView) findViewById(R.id.result)).setText(s);
                });
            });
        });

        findViewById(R.id.bput).setOnClickListener(v -> {
            client.put(((EditText) findViewById(R.id.key)).getText().toString(), ((EditText) findViewById(R.id.value)).getText().toString());
        });

    }


    private class Client extends Thread {

        private final String host;
        private final String port;
        private Socket socket;
        private BufferedReader reader;

        public Client(String host, String port) {
            this.host = host;
            this.port = port;
        }

        public void connect() {
            start();
        }

        @Override
        public void run() {
            try {
                socket = new Socket(host, Integer.parseInt(port));
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void get(String key, Consumer<String> resp) {
            new Thread(() -> {
                try {
                    socket.getOutputStream().write(("get," + key + "\n").getBytes());
                    String line = reader.readLine();
                    resp.accept(line != null ? line.replaceFirst("\n", "") : null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        public void put(String key, String value) {
            new Thread(() -> {
                try {
                    socket.getOutputStream().write(("put," + key + "," + value + "\n").getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }


    }


}