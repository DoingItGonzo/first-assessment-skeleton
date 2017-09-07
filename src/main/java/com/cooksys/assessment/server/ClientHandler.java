package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {

	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	
	static ConcurrentHashMap<String, ClientHandler> clientMap = new ConcurrentHashMap<String, ClientHandler>();
	
	public ObjectMapper mapper;
	public PrintWriter writer;
	public Message message;
	public Socket socket;
	
	private String recipient;
	

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

				if (message.getCommand().startsWith("@")) {
					recipient = message.getCommand().substring(1);
					message.setCommand(message.getCommand().substring(0, 1));
				}
				
				
				//
				// MESSAGE LOGGING MOTHA FUCKER
				//
				// add error handling for multiple copies of a userName  usernames 
				// using too many for loops on the HashMap. Condense that into one method if possible
				switch (message.getCommand()) {

				
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						for(ClientHandler client : clientMap.values()){
                        	messageOut(message.getUsername() + " connected", client);
						} clientMap.put(message.getUsername(), this);
						break;
						
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						for(ClientHandler client : clientMap.values()){
                        	messageOut(message.getUsername() + " disconnected", client);
						} clientMap.remove(message.getUsername());
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
                        } break;

					case "users":
						log.info("user <{}> used command<{}>", message.getUsername(), message.getCommand());
						String userList = "";
						for(String userName : clientMap.keySet()){
                        	userList += (userName + "\n");
                        } messageOut(userList, this);
						break;
						
					case "@":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						if (!clientMap.containsKey(recipient)) {
							messageOut("There are no users online with that username. Check your spelling, bruh", this);
						} else {
							messageOut(message.getUsername() + "> " + message.getContents(), clientMap.get(recipient));
						} break;	
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
