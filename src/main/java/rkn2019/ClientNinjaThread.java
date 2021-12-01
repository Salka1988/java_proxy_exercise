package rkn2019;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;


public class ClientNinjaThread extends Thread {

    private Proxy proxy;
    private Socket clientSocket, serverSocket;

    private InputStream serverInputStream;
    private OutputStream serverOutputStream;

    private InputStream clientInputStream;
    private OutputStream clientOutputStream;

    ServerNinjaThread serverNinja;

    ClientNinjaThread(ServerSocket clientServerSocket, Proxy proxy) {
        try {
            clientSocket = clientServerSocket.accept();
            clientSocket.setSoTimeout(Constants.TIMEOUT);
        } catch (Exception e) {

        }

        this.proxy = proxy;
    }

    // this does not work
//    ClientNinjaThread(Socket clientServerSocket, Proxy proxy) throws IOException {
//        clientSocket = clientServerSocket;
//        this.proxy = proxy;
//    }


    @Override
    public void run() {
        try {
            clientInputStream = clientSocket.getInputStream();
            clientOutputStream = clientSocket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] byteArray = new byte[Constants.HEADER_SIZE];
        int numberOfBytes;
        boolean keepAlive = false, https = false;
        Parser parser = new Parser();

        ByteArrayOutputStream contentFromClient = new ByteArrayOutputStream(); // Creates a new byte array output stream.
        try {

            for (;;) {

                if ((numberOfBytes = clientInputStream.read(byteArray)) == Constants.STREAM_END)
                    break;

                byte[] contentInBytes = Arrays.copyOfRange(byteArray, 0, numberOfBytes);

                // we content to the buffer
                contentFromClient.write(contentInBytes);

                if(https) {
                    serverOutputStream.write(byteArray, 0, numberOfBytes);
                    serverOutputStream.flush();
                    continue;
                }

                if(parser.parse(contentFromClient)) {

                    if(parser.get(parser.CONNECT) != null) {
                        https = true;
                    }

                    String host = parser.getHost();
                    int port = parser.getPort();

                    if (!keepAlive) {
                        if (openHostServerSocket(host.trim(), port, https)) {
                            keepAlive = true;
                            if (https) continue;
                        }
                        else {
                            System.out.println("ERROR in stream or socket!");
                            break;
                        }
                    }

                    byte[] header;

                    header = Parser.getHeader(contentFromClient);

                    byte[] body = Parser.getBody(contentFromClient);
                    byte[] data_to_send = Parser.mergeHeadAndBody(header, body);


                    try {
                        System.out.println("Data sent to server: " + data_to_send.length);
                        serverOutputStream.write(data_to_send, 0, data_to_send.length);
                        serverOutputStream.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    String connection = parser.get(parser.CONNECTION);

                    if(connection != null && connection.trim().equals(parser.KEEP_ALIVE)) {
                        contentFromClient.close();
                        contentFromClient = new ByteArrayOutputStream();
                        parser = new Parser();
                    } else {
                        break;
                    }
                }

            }

        } catch (IOException e) {

        }

        try {
            if(serverNinja != null)
                serverNinja.join();
            if(clientSocket != null)
                clientSocket.close();
            if(clientSocket != null)
                clientSocket.close();

        } catch (IOException | InterruptedException e) {
        }
    }

    private boolean openHostServerSocket(String host, int port, boolean https)
    {
        try {
            serverSocket = new Socket(host, port);
            serverSocket.setSoTimeout(Constants.TIMEOUT);
            serverInputStream = serverSocket.getInputStream();
            serverOutputStream = serverSocket.getOutputStream();
            if(https)
            {
                DataOutputStream output_stream = new DataOutputStream(clientOutputStream);
                output_stream.writeBytes("HTTP/1.1 200 OK\r\n\r\n");
                output_stream.flush();
            }
        } catch (IOException e) {
            return false;
        }
        ServerNinjaThread serverNinjaThread = new ServerNinjaThread(proxy, serverInputStream, clientOutputStream);
        serverNinjaThread.setHttps(https);
        serverNinjaThread.start();
        return true;
    }
}
