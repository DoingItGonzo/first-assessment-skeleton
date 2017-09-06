package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {

	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	
//	static HashMap<String, ClientHandler> clientMap = new HashMap<String, ClientHandler>();
	static ConcurrentHashMap<String, ClientHandler> clientMap = new ConcurrentHashMap<String, ClientHandler>();
	
	public ObjectMapper mapper;
	public PrintWriter writer;
	public Message message;
	public Socket socket;
	

	public ClientHandler(Socket socket) {
		super();
		this.socket = socket;
	}

	public void run() {
		try {

			mapper = new ObjectMapper();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

			while (!socket.isClosed()) {
				String raw = reader.readLine();
				message = mapper.readValue(raw, Message.class);
				
				// add error handling for multiple copies of a userName  usernames 
				switch (message.getCommand()) {
				
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						clientMap.put(message.getUsername(), this);
						break;
						
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						this.socket.close();
						break;
						
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						messageOut(message.getContents(), this);
						break;	
						
					case "broadcast":
                        log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
                        for(ClientHandler client : clientMap.values()){
                        	messageOut(message.getUsername() + "(all) " + message.getContents(), client);
                        }
                        break;

					case "users":
						log.info("user <{}> used command<{}>", message.getUsername(), message.getCommand());
						String userList = "";
						for(String userName : clientMap.keySet()){
                        	userList += (userName + "\n");
                        }
						messageOut(userList, this);
						break;	
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
		
	}

	public void messageOut(String outputMessage, ClientHandler client) throws JsonProcessingException {
		Message out = new Message();
		out.setContents(outputMessage);
		String outJSON = client.mapper.writeValueAsString(out);
		client.writer.write(outJSON);
		client.writer.flush();

    }

}
