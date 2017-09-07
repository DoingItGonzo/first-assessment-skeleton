package com.cooksys.assessment.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cooksys.assessment.model.Message;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientHandler implements Runnable {
	
	static ConcurrentHashMap<String, ClientHandler> clientMap = new ConcurrentHashMap<String, ClientHandler>();

	private Logger log = LoggerFactory.getLogger(ClientHandler.class);
	private Message message;
	private Socket socket;
	private String recipient;
	String timeStamp;
	
	private ObjectMapper mapper;
	public ObjectMapper getMapper() {
		return mapper;
	}
	private PrintWriter writer;
	public PrintWriter getWriter() {
		return writer;
	}
	
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
				timeStamp = (new SimpleDateFormat("h:mm a").format(new Date()));

				if (message.getCommand().startsWith("@")) {
					recipient = message.getCommand().substring(1);
					message.setCommand(message.getCommand().substring(0, 1));
				}
				// MESSAGE LOGGING MOTHA FUCKER
				// add error handling for multiple copies of a userName  usernames 
				// using too many for loops on the HashMap. Condense that into one method if possible
				switch (message.getCommand()) {

				
					case "connect":
						log.info("user <{}> connected", message.getUsername());
						for(ClientHandler client : clientMap.values()){
                        	messageOut("has connected", client, true);
						} clientMap.put(message.getUsername(), this);
						break;
						
					case "disconnect":
						log.info("user <{}> disconnected", message.getUsername());
						for(ClientHandler client : clientMap.values()){
                        	messageOut("has disconnected", client, true);
						} clientMap.remove(message.getUsername());
						this.socket.close();
						break;
						
					case "echo":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						messageOut("(echo): " +  message.getContents(), this, false);
						break;	
						
					case "broadcast":
                        log.info("user <{}> broadcasted message <{}>", message.getUsername(), message.getContents());
                        for(ClientHandler client : clientMap.values()){
                        	messageOut("(all): " + message.getContents(), client, true);
                        } break;

					case "users":
						log.info("user <{}> used command<{}>", message.getUsername(), message.getCommand());
						String userList = "";
						for(String userName : clientMap.keySet()){
                        	userList += (userName + "\n");
                        } messageOut(userList, this, false);
						break;
						
					case "@":
						log.info("user <{}> echoed message <{}>", message.getUsername(), message.getContents());
						if (!clientMap.containsKey(recipient)) {
							messageOut("There are no users online with that username. Check your spelling, bruh", this, false);
						} else {
							messageOut("(whisper): " + message.getContents(), clientMap.get(recipient), true);
						} break;	
				}
			}

		} catch (IOException e) {
			log.error("Something went wrong :/", e);
		}
		
	}

	public void messageOut(String outputMessage, ClientHandler client, boolean needsUsername) throws JsonProcessingException {
		Message out = new Message(); 
		if (needsUsername == true) {
			out.setContents(timeStamp + " <" + message.getUsername() + "> " + outputMessage);
		} else {
			out.setContents(timeStamp + " " + outputMessage);
		} String outJSON = client.getMapper().writeValueAsString(out);
		
		client.getWriter().write(outJSON);
		client.getWriter().flush();
    }

}
