package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.io.Charsets;
import org.apache.log4j.Logger;

import javax.ws.rs.client.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Created by fflores on 7/7/2015.
 */
public class RFIDPrinterUtil {

    private static Logger logger = Logger.getLogger(RFIDPrinterUtil.class);
    private static long timeout = TimeUnit.MINUTES.toMillis(1);

    public static void sendZplToPrint (String ip, int port, String zpl, Boolean rfidEncode) throws Exception{
        try {
            logger.info("Sending zpl to the RFID printer: " + zpl);
            long timeStamp = System.currentTimeMillis();
            StringBuilder serverResponse = new StringBuilder();
            Socket clientSocket = new Socket(ip,port);
            PrintWriter printwriter = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8),true);
            printwriter.write(zpl);
            printwriter.flush();
            if (rfidEncode.compareTo(Boolean.TRUE) == 0){
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
                RunnableImp runnableImp = new RunnableImp(serverResponse,in);
                Thread runRFIDPrinter = new Thread(runnableImp);
                runRFIDPrinter.start();
                while (runRFIDPrinter.isAlive()){
                    if( (System.currentTimeMillis() - timeStamp) > timeout){
                        runRFIDPrinter.interrupt();
                        break;
                    }
                }
                logger.info("printer response is: " + serverResponse);
                if (serverResponse.length() == 0)
                    throw new UserException("ZPL command is invalid");
            }
            printwriter.close();
            clientSocket.close();
            logger.info("zpl was evaluated in : " + (System.currentTimeMillis() - timeStamp) + " ms");
        } catch (ConnectException e) {
            throw new UserException("Connection timeout, there might a problem with printer connectivity.", e);
        } catch (Exception e){
            throw new UserException(e.getMessage(), e);
        }
    }

    public static void sendZplToWebService (String zpl) throws Exception{
        try {
            Client client = ClientBuilder.newBuilder().register(RFIDPrinterUtil.class).build();
            // adjust print density (8dpmm), label width (4 inches), label height (6 inches), and label index (0) as necessary
            WebTarget target = client.target("http://api.labelary.com/v1/printers/8dpmm/labels/4x6/0/");
            Invocation.Builder request = target.request();
            // request.accept("application/pdf"); // omit this line to get PNG images back
            Response response = request.post(Entity.entity(zpl, MediaType.APPLICATION_FORM_URLENCODED));
            if (response.getStatus() == 200) {
                byte[] body = response.readEntity(byte[].class);
                File file = new File("label.png"); // change file name: pdf, png
                Files.write(file.toPath(), body);
            } else {
                String body = response.readEntity(String.class);
                System.out.println("Error: " + body);
            }
        } catch (IOException e){
            throw new Exception(e.getMessage(), e);
        }
    }

    static class RunnableImp implements Runnable{
        private StringBuilder resp;
        private BufferedReader in;

        public RunnableImp(StringBuilder response, BufferedReader in){
            resp = response;
            this.in = in;
        }
        @Override
        public void run() {
            try {
                resp.append(String.valueOf(in.read()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
