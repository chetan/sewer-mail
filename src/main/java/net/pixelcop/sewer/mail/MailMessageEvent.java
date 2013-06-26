package net.pixelcop.sewer.mail;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import javax.mail.Folder;
import javax.mail.Message;

import net.pixelcop.sewer.Event;

public class MailMessageEvent implements Event {
    
    private Message message;
    private String dest;
    
    public MailMessageEvent(Message message, String destFolder) {
        this.message = message;
        this.dest = destFolder;
    }
    
    public Message getMessage() {
        return message;
    }
    
    public void setMessage(Message message) {
        this.message = message;
    }
    
    public String getDest() {
        return dest;
    }
    
    public void setDest(String dest) {
        this.dest = dest;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        throw new NoSuchMethodError("not available for this type of event");
    }

    @Override
    public void readFields(DataInput in) throws IOException {
        throw new NoSuchMethodError("not available for this type of event");
    }

}
