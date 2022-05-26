package ro.pub.cs.systems.eim.colocviu2_2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class MainActivity extends AppCompatActivity {


    private static Server server;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        server = new Server("localhost", 8181);
        server.start();

        findViewById(R.id.btnrestart).setOnClickListener(view -> {
            if (server != null) {
                server.close();
            }
            server = new Server(null, Integer.parseInt(((EditText) findViewById(R.id.l_port)).getText().toString()));
            server.start();
        });

        findViewById(R.id.go_client).setOnClickListener(view -> {
            Intent intent = new Intent(this, ClientActivity.class);
            startActivity(intent);
        });



    }



    static class Server extends Thread {
        private Map<String, String> map = new HashMap<>();
        private Map<String, Long> expiry = new HashMap<>();
        private ObjectMapper mapper = new ObjectMapper();
        private OkHttpClient client = new OkHttpClient();


        private final String host;
        private final int port;

        public Server(String host, int port) {
            this.host = host;
            this.port = port;
        }

        ServerSocket socket = null;
        @Override
        public void run() {
            try {
                socket = new ServerSocket(port);
                while (true) {
                    Socket sock = socket.accept();
                    InputStreamReader inputStreamReader = new InputStreamReader(sock.getInputStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    while (sock.isConnected()) {
                        String coommand = bufferedReader.readLine();
                        if (coommand == null) {
                            break;
                        }
                        coommand = coommand.replace("\n", "");
                        String[] args = coommand.split(",");
                        if (args[0].equals("get")) {
                            if (getTimestam() - 60 > expiry.getOrDefault(args[1], -1l)) {
                                sock.getOutputStream().write("none\n".getBytes());
                            } else {
                                sock.getOutputStream().write((map.getOrDefault(args[1], "none") + "\n").getBytes());
                            }
                            sock.getOutputStream().flush();
                        }
                        if (args[0].equals("put")) {
                            map.put(args[1], args[2]);
                            expiry.put(args[1], getTimestam());
                        }
                    }
                }
            } catch (IOException e) {
                server = null;
            }
        }

        private long getTimestam() {
            Request request = new Request.Builder().url("http://worldtimeapi.org/api/ip")
                    .get()
                    .build();
            try {
                String resp = client.newCall(request).execute().body().string();
                return Long.parseLong(mapper.readTree(resp).get("unixtime").asText());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return -1;
        }

        public void close() {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.interrupt();
            server = null;
        }
    }

}