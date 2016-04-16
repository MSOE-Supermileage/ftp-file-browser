package edu.msoe.smv.logfiledownloader;

import android.support.annotation.NonNull;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;

public class FTPClientManager {
    private String server;
    private String username;
    private String passwd;
    private FTPClient ftpClient;

    public FTPClientManager(String server, String username, String passwd) {
        this.ftpClient = new FTPClient();
        this.server = server;
        this.username = username;
        this.passwd = passwd;

        FTPClientConfig config = new FTPClientConfig();
        config.setServerLanguageCode("en");
        config.setServerTimeZoneId(TimeZone.getDefault().toString());
        this.ftpClient.configure(config);
    }

    /**
     * Connects and logins to the FTP server
     *
     * @throws ConnectException if the FTP server refused connection
     * @throws IOException
     */
    public boolean connect() throws IOException {
        ftpClient.connect(InetAddress.getByName(server));

        if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            ftpClient.disconnect();
            return false;
        }
        if (!ftpClient.login(username, passwd)) {
            ftpClient.logout();
            return false;
        }

        return true;
    }

    /**
     * Returns the present working directory
     *
     * @return The pathname of the current working directory.  If it cannot
     * be obtained, returns null.
     */
    public String pwd() {
        String pwd;
        try {
            pwd = ftpClient.printWorkingDirectory();
        } catch (IOException e) {
            pwd = null;
        }
        return pwd;
    }

    /**
     * Returns a list of {@link org.apache.commons.net.ftp.FTPFile FTPFiles}
     * in the current directory. Any files that failed to parse (and are thus
     * null) are removed from the returned list.
     *
     * @return a list of files in the current directory, or null if an
     * IOException occurred during the request
     */
    public List<FTPFile> listFiles() {
        List<FTPFile> files;
        try {
            FTPFile[] ftpFiles = ftpClient.listFiles();
            files = cleanFtpFiles(ftpFiles);
        } catch (IOException e) {
            files = null;
        }
        return files;
    }

    /**
     * Returns a list of {@link org.apache.commons.net.ftp.FTPFile FTPFiles}
     * that only contain non-null {@linkplain FTPFile#isValid() valid} files.
     *
     * @param ftpFiles the list of FTPFiles to clean
     * @return a {@code List<FTPFiles>} that contains no <tt>null</tt> objects
     */
    @NonNull
    private List<FTPFile> cleanFtpFiles(FTPFile... ftpFiles) {
        List<FTPFile> files = new ArrayList<>(Arrays.asList(ftpFiles));

        ListIterator<FTPFile> literator = files.listIterator();
        while (literator.hasNext()) {
            FTPFile file = literator.next();
            // remove any files that failed to parse
            if (file == null || !file.isValid()) {
                literator.remove();
            }
        }

        return files;
    }

    public List<FTPFile> listDirectories() {
        List<FTPFile> dirs;
        try {
            FTPFile[] ftpFiles = ftpClient.listDirectories();
            dirs = cleanFtpFiles(ftpFiles);
        } catch (IOException e) {
            dirs = null;
        }
        return dirs;
    }

    /**
     * Joins the results of {@link #listDirectories()} and {@link #listFiles()}
     * into a single list.
     *
     * @return the union of {@link #listDirectories()} and {@link #listFiles()}
     */
    public List<FTPFile> ls() {
        ArrayList<FTPFile> result = new ArrayList<>(listDirectories());
        List<FTPFile> files = listFiles();
        result.ensureCapacity(result.size() + files.size());
        result.addAll(files);
        return result;
    }

    /**
     * Returns the specified file contents at the filepath {@code remote}, or
     * null if something bad happened.
     *
     * @param remote the pathname of the remote file to retrieve
     * @return the contents of {@code remote}, or null
     */
    public String retrieveFile(String remote) {
        String fileContents;
        try (OutputStream outputStream = new ByteArrayOutputStream()) {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            if (ftpClient.retrieveFile(remote, outputStream)) {
                fileContents = outputStream.toString();
            } else { // retrieve failed
                fileContents = null;
            }
        } catch (IOException e) {
            fileContents = null;
        }
        return fileContents;
    }

    /**
     * Moves into another directory
     *
     * @param path the path of the directory to move into
     * @return true if the operation succeeded, otherwise false
     */
    public boolean cd(String path) {
        boolean result;
        path = path.trim();
        if (path.equals("..")) {
            result = upDir();
        } else {
            try {
                result = ftpClient.changeWorkingDirectory(path);
            } catch (IOException e) {
                result = false;
            }
        }
        return result;
    }

    /**
     * Moves to the parent directory
     *
     * @return true if the operation succeeded, otherwise false
     */
    public boolean upDir() {
        try {
            return ftpClient.changeToParentDirectory();
        } catch (IOException e) {
            return false;
        }
    }
}
