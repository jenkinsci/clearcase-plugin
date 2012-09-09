/*
 * The MIT License
 * 
 * Copyright (c) 2011, Sun Microsystems, Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.clearcase.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A {@link FileInputStream} which (optionally) deletes the
 * underlying file when the stream is closed.
 *
 * A modified version of the class implemented by BJ in his blog,
 * ref http://bimalesh.blogspot.com/2010/02/auto-deleting-fileinputstream.html
 *
 * @author BJ 
 * @author Christophe Guillon <christophe.guillon@st.com>
 */
public class DeleteOnCloseFileInputStream extends FileInputStream {

    /**
     * Lock for managing isDelete.
     */
    private final Object deleteLock = new Object();

    /**
     * Logger if defined
     */
    protected Logger logger;

    /**
     * Underlying file object
     */
    private File fileObj;

    /**
     * Was underlying File object deleted.
     */
    private boolean isDeleted;

    /**
     * Flag to control auto-delete of file on close()
     */
    private final boolean deleteOnClose;

    /**
     * Set logger.
     *
     * @param logger logger to use
     */
    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Creates a fileInputStream wrapped around the given file and deletes the
     * file when the FileInputStream is closed.
     *
     * @param file an opened file
     * @throws FileNotFoundException
     */
    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
        this(file, true);
    }

    /**
     * Creates a fileInputStream for the given file name and deletes the
     * file when the FileInputStream is closed.
     *
     * @param name the filename that will be opened for input
     * @throws FileNotFoundException
     */
    public DeleteOnCloseFileInputStream(String name) throws FileNotFoundException {
        this(name != null ? new File(name) : null);
    }

    /**
     * Creates a fileInputStream wrapped around the given file.
     *
     * @param file the filename that will be opened for input
     * @param deleteOnClose set to true for activating deletion on close
     * @throws FileNotFoundException
     */
    public DeleteOnCloseFileInputStream(final File file,
                                        final boolean deleteOnClose) throws FileNotFoundException {
        super(file);
        this.fileObj = file;
        this.deleteOnClose = deleteOnClose;
        isDeleted = false;
    }


    /**
     * @return boolean flag, true if the file should be deleted on close().
     *         Default is true.
     */
    public final boolean isDeleteOnClose() {
        return deleteOnClose;
    }


    /**
     * Closes the underlying FileInputStream and also deletes the
     * file object from disk if the isDeleteOnClose()
     * is set to true.
     *
     * @see java.io.FileInputStream#close()
     */
    @Override
    public void close() throws IOException {
        if (logger != null)
            logger.log(Level.INFO, "closing file" + this.fileObj);
        super.close();
        if (deleteOnClose) {
            deleteFile();
        }
    }


    /**
     * Deletes the file object from disk as soon as the file descriptor
     * have been released.
     *
     * @see java.io.FileInputStream#close()
     */
    private void deleteFile() throws IOException {
        FileDescriptor fd = super.getFD();
        /* Delete only once and if the fd is invalid (i.e. released). */
        if (fd.valid()) {
            return;
        }
        synchronized (deleteLock) {
            if (isDeleted) {
                return;
            }
            isDeleted = true;
        }
        if (logger != null)
            logger.log(Level.INFO, "deleting file" + this.fileObj);
        fileObj.delete() ;
    }

    /**
     * Ensures that the <code>close</code> method of this file input stream is
     * called when there are no more references to it.
     *
     * @exception  IOException  if an I/O error occurs.
     */
    @Override
    protected void finalize() throws IOException {
        super.finalize();
        if (deleteOnClose) {
            deleteFile();
        }
    }
}
