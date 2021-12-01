package rkn2019;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;

public class Parser {

    private HashMap<String, String> HTTPfields;
    private boolean notParsed;
    private String header;

    public final String HOST = "Host";  // ...
    public final String CONNECT = "CONNECT";
    public final String COOKIE = "Cookie";
    public final String CONTENTLENGTH = "Content-Length";
    public final String TRANSFERENCODING = "Transfer-Encoding";
    public final String CONTENTENCODING = "Content-Encoding";
    public final String CONTENTTYPE = "Content-Type";
    public final String CONNECTION = "Connection";
    public final String LOCATION = "Location";

    public final String KEEP_ALIVE = "keep-alive";
    public final String CHUNKED = "chunked";



    public Parser () {
        HTTPfields = new HashMap<>();

        notParsed = true;
        header = "";
    }

    public boolean parse(ByteArrayOutputStream byteArrayOutputStream) throws IOException
    {
        byte[] str = byteArrayOutputStream.toByteArray();
        if (headerPresent(str) && notParsed)
        {
            //System.out.println("parsing header ============================================");

            // read the content of HTTP reqeust using BufferedReadrer, it reads multiple lines
            // ByteArrayInputStream read data from byte array
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(str)));

            // parse first line
            // this is GET or CONNECT
            String line = bufferedReader.readLine();
            fillHeader(line);

            String ssl[] = line.split(" ");
            /////////////////////////////////////////////////
            if(ssl[0].equals(this.CONNECT))
            {
                HTTPfields.put(this.CONNECT, "true");
            }
            /////////////////////////////////////////////////

            String[] tokens;
            while ( (line = bufferedReader.readLine()) != null && !line.isEmpty()) {

                fillHeader(line);

                tokens = line.split(": ");

                // split every line into parameter and its content, for example host: www.abcd.com
                if (tokens.length == 2) {
                    HTTPfields.put(tokens[0].trim(), tokens[1].trim());
                    //System.out.println(tokens[0] + "->" + tokens[1]);
                }

            }
            fillHeader("");

            //System.out.println("end of parsing header ============================================");
            notParsed = false;
        }

        // check if request ends with \r\n\r\n
        return this.checkEnd(str);
    }

    public void fillHeader(String line)
    {
        this.header += line + "\r\n";
    }

    private String getConnection() {
        String t = HTTPfields.get(CONNECTION);
        if (t != null)
            return t.trim();
        else
            return null;
    }


    public String get(String header)
    {
        String t = HTTPfields.get(header);
        if (t != null)
            return t.trim();
        else
            return null;
    }

    public boolean checkEnd(byte[] request) throws NumberFormatException, UnsupportedEncodingException
    {
        if (this.isChunked())
        {
            if (request[request.length-1] == 10 &&
                    request[request.length-2] == 13 &&
                    request[request.length-3] == 10 &&
                    request[request.length-4] == 13 &&
                    request[request.length-5] == 48) {

                //System.out.println("ended chunked");
                return true;
            }

            return false;
        }

        /////////////////////////////////////////////////////////
        else if (this.get(this.CONTENTLENGTH) != null)
        {
            //System.out.println("content-length provjera");
            // CONTENT LENGTH is the number of bytes of data in the body of the request or response.
            // Generally the Content-Length header is used for HTTP 1.1 so that the receiving party knows when
            // the current response has finished, so the connection can be reused for another request.
            if (Integer.parseInt(this.get(this.CONTENTLENGTH)) == request.length - this.header.getBytes().length)
                return true;
        }
        ////////////////////////////////////////////////////

        else if (request[request.length-1] == 10 &&
                request[request.length-2] == 13 &&
                request[request.length-3] == 10 &&
                request[request.length-4] == 13)
        {
            //System.out.println("*End provjera");
            return true;
        }

        else
            return false;
        return false;
    }

    String getHost() {
        String host = HTTPfields.get(HOST);
        if (host != null)
            return host.split(":")[0];
        else
            return null;
    }

    int getPort() {
        String[] host = HTTPfields.get(HOST).split(":");
        if (host.length == 2)
            return Integer.parseInt(host[1]);
        else
            return Constants.DEFAULT_PORT;
    }

    public boolean checkIfKeepAlive()
    {
        if (getConnection() != null)
            return getConnection().trim().equals("keep-alive");
        return false;
    }

    boolean isChunked() {
        return HTTPfields.get(this.TRANSFERENCODING) != null;
    }

    public static byte[] getHeader(ByteArrayOutputStream byteArrayOutputStream) {
        byte[] request = byteArrayOutputStream.toByteArray();
        return Arrays.copyOfRange(request, 0, getBodyIndex(request));
    }

    // header is separated from body with: \r\n\r\n; the first index after that denotes the beginning of body
    public static int getBodyIndex(byte[] request)
    {
        for (int bodyIndex = 0; bodyIndex < request.length - 3; bodyIndex++) {
            if(request[bodyIndex] == 13 &&
                    request[bodyIndex+1] == 10 &&
                    request[bodyIndex+2] == 13 &&
                    request[bodyIndex+3] == 10) {
                return bodyIndex + 4;
            }
        }
        return 0;
    }

    public static byte[] getBody(ByteArrayOutputStream byteArrayOutputStream) {
        byte[] request = byteArrayOutputStream.toByteArray();
        return Arrays.copyOfRange(request, getBodyIndex(request), request.length);
    }

    public static byte[] mergeHeadAndBody(byte[] a, byte[] b) {
        byte[] c = new byte[a.length + b.length];
        // arraycopy copies a from index 0 into c at position 0 to a.length
        System.arraycopy(a, 0, c, 0, a.length);
        // arraycopy copies b from index 0 into c at position a.length to b.length
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static boolean headerPresent(byte[] rawReplay) {
        /*
         * End of HTTP header is denoted with:
         * ...\r\n
         * \r\n
         * */
        for (int i = 0; i < rawReplay.length - 3; ++i) {
            if(rawReplay[i] == 13 &&
                    rawReplay[i+1] == 10 &&
                    rawReplay[i+2] == 13 &&
                    rawReplay[i+3] == 10)
                return true;
        }
        return false;
    }

    public static byte[] unchunk(byte[] request) throws IOException
    {
        ByteArrayOutputStream new_request = new ByteArrayOutputStream();

        int curr_index = 0;
        //new_request.write(Arrays.copyOfRange(request, 0, curr_index));

        for (int i = curr_index; i < request.length; i++) {

            if (request[i] == 13 && request[i+1] == 10) { // e.g. 17\r\n ---

                // length of chunk; should be hexadecimal
                byte [] number = Arrays.copyOfRange(request, curr_index, i);
                int length = Integer.parseInt(new String(number), 16);
                //System.out.println("currindex :"+curr_index);
                //System.out.println("number :"+length);


                // kraj requesta; zero-length chunk
                if (length == 0) {

                    new_request.write(new byte[] {13,10,13,10}); //????
                    return new_request.toByteArray();
                }

                byte [] temp = Arrays.copyOfRange(request, i+2, i+2+length);
                new_request.write(temp);

                curr_index += i+4+length;
                i = curr_index;
            }
        }

        return new_request.toByteArray();
    }

    public static byte[] removeLine(byte[] request, int from, int to) {
        byte[] new_request_1 = Arrays.copyOfRange(request, 0, from);
        byte[] new_request_2 = Arrays.copyOfRange(request, to, request.length);
        byte[] new_request = new byte[new_request_1.length + new_request_2.length];
        System.arraycopy(new_request_1, 0, new_request, 0, new_request_1.length);
        System.arraycopy(new_request_2, 0, new_request, new_request_1.length, new_request_2.length);
        return new_request;
    }

    public String getHdr() {
        return header;
    }
}




