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

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPStore;

import net.pixelcop.sewer.Event;
import net.pixelcop.sewer.PlumbingProvider;
import net.pixelcop.sewer.Sink;
import net.pixelcop.sewer.SinkRegistry;
import net.pixelcop.sewer.util.BatchHelper;

public class IMAPSink extends Sink implements PlumbingProvider {

    private static final Logger LOG = LoggerFactory.getLogger(IMAPSink.class);

    private static final Map<String, Integer> foldersOnServer = new HashMap<String, Integer>();

    private String host;
    private String user;
    private String pass;

    private IMAPStore imapStore;

    private String currentFolderName;
    private Folder currentFolder;

    private BatchHelper batchHelper;

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

        flushBatch();

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

        MailMessageEvent mevent = (MailMessageEvent) event;
        try {
            ensureFolderExists(imapStore, mevent.getDest());
        } catch (MessagingException e) {
            throw new IOException("error writing to IMAP server " + host, e);
        }

        // see if we got a message for a new folder
        if (currentFolderName != null && !currentFolderName.equals(mevent.getDest()) && !batchHelper.isEmpty()) {
            flushBatch();
        }

        if (batchHelper.append(mevent)) {
            return; // stored in batch
        }

        // batch is full, send previous batch
        flushBatch();

        batchHelper.append(mevent);
    }

    /**
     * Flush the current batch to the server
     *
     * @throws IOException
     */
    private void flushBatch() throws IOException {

        if (batchHelper.isEmpty()) {
            return;
        }

        try {
            LOG.debug("copying message batch to server (" + batchHelper.size() + " msgs)");

            Event[] batch = batchHelper.getBatch();
            Message[] messages = new Message[batch.length];
            for (int i = 0; i < batch.length; i++) {
                MailMessageEvent mevent = (MailMessageEvent) batch[i];
                messages[i] = mevent.getMessage();
            }

            getFolder(((MailMessageEvent) batch[0]).getDest()).appendMessages(messages);

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

        batchHelper = new BatchHelper(10);
    }

    @Override
    public void register() {
        SinkRegistry.register("imap", getClass());
    }

}
