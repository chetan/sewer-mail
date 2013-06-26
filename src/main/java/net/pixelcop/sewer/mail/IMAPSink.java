package net.pixelcop.sewer.mail;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPStore;

import net.pixelcop.sewer.Event;
import net.pixelcop.sewer.PlumbingProvider;
import net.pixelcop.sewer.Sink;
import net.pixelcop.sewer.SinkRegistry;

public class IMAPSink extends Sink implements PlumbingProvider {
    
    private static final Logger LOG = LoggerFactory.getLogger(IMAPSink.class);
    
    private static final Map<String, Integer> foldersOnServer = new HashMap<String, Integer>();
    
    private String host;
    private String user;
    private String pass;
    
    private IMAPStore imapStore;
    
    private String currentFolderName;
    private Folder currentFolder;
    
    public IMAPSink() {
    }
    
    public IMAPSink(String[] args) {
        if (args == null || args.length < 3) {
            throw new IllegalArgumentException("usage: path,imap_hostname,user,pass");
        }
       
        this.host = "imap://" + args[0];
        this.user = args[1];
        this.pass = args[2];
    }
    
    public static synchronized void ensureFolderExists(IMAPStore imapStore, String name)
            throws MessagingException {
        
        if (foldersOnServer.containsKey(name)) {
            return;
        }

        Folder destFolder = imapStore.getFolder(name);
        if (!destFolder.exists()) {
            LOG.info("creating folder " + name + " on server");
            destFolder.create(Folder.HOLDS_MESSAGES & Folder.HOLDS_FOLDERS);
        }

        foldersOnServer.put(name, 1);
    }

    @Override
    public void close() throws IOException {

        if (currentFolder != null) {
            try {
                currentFolder.close(false);
            } catch (MessagingException e) {
                LOG.error("error while closing folder " + currentFolderName, e);
            }
        }
        
        try {
            imapStore.close();
        } catch (MessagingException e) {
            LOG.error("error while disconnecting from IMAP server " + host, e);
        }

    }

    @Override
    public void append(Event event) throws IOException {
        
        LOG.debug("copying message to server");
        
        MailMessageEvent mevent = (MailMessageEvent) event;
        
        try {
            ensureFolderExists(imapStore, mevent.getDest());
            getFolder(mevent.getDest()).appendMessages(new Message[]{ mevent.getMessage() });
            
        } catch (MessagingException e) {
            throw new IOException("error writing to IMAP server " + host, e);
        }
        
    }
    
    public Folder getFolder(String name) throws MessagingException {
        
        if (currentFolderName != null && currentFolderName.equals(name)) {
            return currentFolder;
        }
        
        if (currentFolder != null) {
            try {
                LOG.debug("closing previous folder: " + currentFolderName);
                currentFolder.close(false);
            } catch (MessagingException e) {
                LOG.warn("error closing previous folder: " + currentFolderName);
            }
        }
        
        currentFolderName = name;
        currentFolder = imapStore.getFolder(name);
        currentFolder.open(Folder.READ_WRITE);
        return currentFolder;
    }

    @Override
    public void open() throws IOException {

        Properties props = new Properties();        
        Session session = Session.getDefaultInstance(props);
        
        imapStore = new IMAPStore(session, new URLName(host));
        try {
            imapStore.connect(user, pass);
        } catch (MessagingException e) {
            throw new IOException(e);
        }
        
    }

    @Override
    public void register() {
        SinkRegistry.register("imap", getClass());
    }

}
