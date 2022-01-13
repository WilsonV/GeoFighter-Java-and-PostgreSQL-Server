/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package myudpserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;



/**
 *
 * @author Wilson
 */
public class FileReciever implements Runnable{
    private ServerSocket server_socket;
    private Socket socket;
    private String file_location;
    private int bytesRead;
    
    FileReciever(int port,String file_location){
        
        this.file_location = file_location;
        
        try{
            server_socket = new ServerSocket(port);
            
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void run(){
        //System.out.println("FR: Waiting for something...");
        OutputStream fos = null;
        
        //check if file exist, if not make a new one
        if(!new File(file_location).exists()){
            try {
                new File(file_location).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        //create file stream to write
        try {
            fos = new FileOutputStream(new File(file_location));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        try {
            socket = server_socket.accept();
            
            byte[] buffer = new byte[1024];
            
            InputStream is = socket.getInputStream();
            
            //bytesRead = is.read(buffer);
            
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            
            // Closing the FileOutputStream handle
            is.close();
            fos.close();
            socket.close();
            server_socket.close();
            
            
        } catch (IOException e) {
            
            e.printStackTrace();
        }
        
        //System.out.println("FR: Alright I got the file...");
    }
}
