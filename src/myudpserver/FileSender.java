/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author Wilson
 */
public class FileSender implements Runnable{
    private ServerSocket server_socket;
    private Socket socket;
    private String file_location;

    FileSender(int port,String file_location){

        this.file_location = file_location;

        try{
            server_socket = new ServerSocket(port);

        }catch(IOException e){
            e.printStackTrace();
        }
    }
    public void run(){
        //System.out.println("FS: Waiting for something...");
        InputStream fis = null;
        byte[] buffer = new byte[100];
        File file_to_send = new File(file_location);

        try {
            socket = server_socket.accept();
            OutputStream os = socket.getOutputStream();

            //If File doesn't exist let the client know
            //otherwise let them know the size of the file

            if(file_to_send.exists() && file_to_send.isFile()){

                try {

                    fis = new FileInputStream(file_to_send);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                //System.out.println("File SIZE IS:" +file_to_send.length() );
                //buffer = ("sze"+file_to_send.length()).getBytes();


               // os.write(buffer,0,buffer.length);
                //os.flush();
                //make a buffer the same size as the file
                buffer = new byte[(int)file_to_send.length()];
                BufferedInputStream bis = new BufferedInputStream(fis);
                bis.read(buffer, 0, buffer.length);
                os.write(buffer,0,buffer.length);
                os.flush();
                fis.close();
            }else{

                buffer = "/mis".getBytes();
                os.write(buffer,0,buffer.length);
            }

            // Closing the FileOutputStream handle
            os.close();
            socket.close();
            server_socket.close();


        } catch (IOException e) {

            e.printStackTrace();
        }

        //System.out.println("FS: Alright I sent the file...");
    }
}
