package rkn2019;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


public class ServerNinjaThread extends Thread {

    private Proxy proxy;
    private InputStream serverInputStream;
    private OutputStream clientOs;
    private boolean https;

    ServerNinjaThread(Proxy proxy, InputStream serverInputStream, OutputStream clientOutputStream) {
        this.proxy = proxy;
        this.serverInputStream = serverInputStream;
        this.clientOs = clientOutputStream;
    }

    void setHttps(boolean isHttps) {
        https = isHttps;
    }

    @Override
    public void run() {
        byte[] write = new byte[Constants.HEADER_SIZE];
        int bytes_read;
        StringBuilder data = new StringBuilder();
        Parser parser = new Parser();
        ByteArrayOutputStream contentToClient = new ByteArrayOutputStream();

        try {
            for (;;) {
                if ((bytes_read = serverInputStream.read(write)) == Constants.STREAM_END)
                    break;

                byte[] contentBytes = Arrays.copyOfRange(write, 0, bytes_read);
                data.append(new String(contentBytes, StandardCharsets.UTF_8));
                contentToClient.write(contentBytes);

                if(https) {
                    clientOs.write(write, 0, bytes_read);
                    clientOs.flush();
                    continue;
                }

                if(parser.parse(contentToClient)) {
                    if(parser.isChunked()) {
                        ByteArrayOutputStream newByteArrayOutputStream = new ByteArrayOutputStream();
                        newByteArrayOutputStream.write(contentToClient.toByteArray());

                        System.out.println(!sendToClient(newByteArrayOutputStream) ?
                                "Failed to send data to the client!" :
                                "Client received data - size: " + newByteArrayOutputStream.size());
                    } else {
                        System.out.println(!sendToClient(contentToClient) ?
                                "Failed to send data to the client!" :
                                "Client received data - size: " + contentToClient.size());
                    }

                    if (parser.checkIfKeepAlive()) {
                        data = new StringBuilder();
                        contentToClient.close();
                        contentToClient = new ByteArrayOutputStream();
                        parser = new Parser();
                    } else {
                        break;
                    }
                }
            }
        } catch (IOException e) {

        }

    }


    public boolean sendToClient(ByteArrayOutputStream byteArrayOutputStream) {

        byte[] header = Parser.getHeader(byteArrayOutputStream);
        byte[] body = Parser.getBody(byteArrayOutputStream);
        byte[] data_to_send = Parser.mergeHeadAndBody(header, body);

        try {
            clientOs.write(data_to_send, 0, data_to_send.length);
            clientOs.flush();
            return true;
        } catch (IOException e) {
            System.out.println("Error sending data to client!");
            return false;
        }
    }



}