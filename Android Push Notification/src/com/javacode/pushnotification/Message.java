package com.javacode.pushnotification;


public class Message {

	String id = null;
	String from = null;
	String subject = null;
	String date = null;
	boolean selected = false;

	public Message(String id, String from, String subject, String date,
			boolean selected) {
		super();
		this.id = id;
		this.from = from;
		this.subject = subject;
		this.date = date;
		this.selected = selected;
		
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

}
