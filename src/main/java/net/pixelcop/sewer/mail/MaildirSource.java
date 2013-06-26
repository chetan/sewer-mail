package net.pixelcop.sewer.mail;

import java.io.IOException;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.URLName;

import net.pixelcop.sewer.PlumbingProvider;
import net.pixelcop.sewer.Sink;
import net.pixelcop.sewer.Source;
import net.pixelcop.sewer.SourceRegistry;
import net.ukrpost.storage.maildir.MaildirStore;

import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MaildirSource extends Source implements PlumbingProvider {
    
    class MaildirReader extends Thread {
        public MaildirReader() {
            setName("maildir reader " + getId());
        }
        
        @Override
        public void run() {
            
            try {
                recurseFolders(localStore.getDefaultFolder());
                
            } catch (MessagingException e) {
                LOG.error("failed to open default folder", e);
                setStatus(ERROR);
                System.exit(1);
                
            } catch (IOException e) {
                LOG.error("failed to append message to sink", e);
                setStatus(ERROR);
                System.exit(1);
            
            } finally {                
                try {
                    sink.close();
                } catch (IOException e) {
                    LOG.error("failed to close sink", e);
                    setStatus(ERROR);
                    System.exit(1);
                }
            }           
            
            LOG.info("finished copying all messages");
            System.exit(0);
            
        }
        
        private void recurseFolders(Folder parentFolder) throws MessagingException, IOException {
            
            Folder[] folders = parentFolder.list();
            for (Folder localFolder : folders) {
                
                localFolder.open(Folder.READ_ONLY);
                LOG.info(localFolder.getFullName());
                LOG.info("count: "+localFolder.getMessageCount());
                
                // create dest folder name
                String name = localFolder.getFullName();
                if (name != "INBOX") {
                    if (name.startsWith(".")) {
                        name = "INBOX" + name;
                    } else {
                        name = "INBOX" + "." + name;
                    }
                }
                                
                copyMessages(localFolder.getMessages(), name);
                
                recurseFolders(localFolder);
                localFolder.close(false);
            }
            
        }
        
        private void copyMessages(Message[] messages, String destFolder) throws MessagingException, IOException {
            if (messages == null && messages.length == 0) {
                return;
            }
            
            LOG.info("copying " + messages.length + " messages to server");
            for (Message message : messages) {
                MailMessageEvent event = new MailMessageEvent(message, destFolder);
                sink.append(event);
            }            
        }
        
    }
    

    private static final Logger LOG = LoggerFactory.getLogger(MaildirSource.class);
    
    private String rootDir;
    
    private MaildirStore localStore;
    
    private MaildirReader thread;
    private Sink sink;
    
    private StopWatch stopwatch;
    
    public MaildirSource() {
    }
    
    public MaildirSource(String[] args) {
        if (args == null || args.length < 1) {
            throw new IllegalArgumentException("usage: path,imap_hostname,user,pass");
        }
        
        this.rootDir = args[0];      
        this.stopwatch = new StopWatch();
    }

    @Override
    public void close() throws IOException {
        LOG.debug("closing");
        LOG.info("finished run in " + stopwatch.toString());
        setStatus(CLOSING);
        
        thread.interrupt();
        sink.close();
        
        setStatus(CLOSED);
        LOG.debug("closed");
    }

    @Override
    public Class<?> getEventClass() {
        return MailMessageEvent.class;
    }

    @Override
    public void open() throws IOException {
        
        LOG.debug("opening");
        setStatus(OPENING);
        
        stopwatch.start();

        Properties props = new Properties();        
        Session session = Session.getDefaultInstance(props);
        
        localStore = new MaildirStore(session, new URLName(rootDir));
        
        sink = createSink();
        
        thread = new MaildirReader();
        thread.start();
        
        setStatus(FLOWING);
        LOG.debug("flowing");
    }

    @Override
    public void register() {
        SourceRegistry.register("maildir", getClass());
    }

}
