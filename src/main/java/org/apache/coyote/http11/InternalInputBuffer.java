package org.apache.coyote.http11;

import java.io.IOException;
import java.io.InputStream;
import java.io.EOFException;

import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.buf.MessageBytes;
import org.apache.tomcat.util.http.MimeHeaders;
import org.apache.tomcat.util.res.StringManager;

import org.apache.coyote.InputBuffer;
import org.apache.coyote.Request;

public class InternalInputBuffer implements InputBuffer {

    public InternalInputBuffer(Request request) {
        this(request, Constants.DEFAULT_HTTP_HEADER_BUFFER_SIZE);
    }

    public InternalInputBuffer(Request request, int headerBufferSize) {
        this.request = request;
        headers = request.getMimeHeaders();

        headerBuffer1 = new byte[headerBufferSize];
        headerBuffer2 = new byte[headerBufferSize];
        bodyBuffer = new byte[headerBufferSize];
        buf = headerBuffer1;

        headerBuffer = new char[headerBufferSize];
        ascbuf = headerBuffer;

        inputStreamInputBuffer = new InputStreamInputBuffer();

        filterLibrary = new InputFilter[0];
        activeFilters = new InputFilter[0];
        lastActiveFilter = -1;

        parsingHeader = true;
        swallowInput = true;
    }

    protected static StringManager sm = StringManager.getManager(Constants.Package);

    protected Request request;

    protected MimeHeaders headers;

    protected boolean parsingHeader;

    protected boolean swallowInput;

    protected byte[] buf; // Pointer to the current read buffer.

    protected char[] ascbuf; // Pointer to the US-ASCII header buffer.

    protected int lastValid; // Last valid byte.

    protected int pos; // Position in the buffer.

    protected byte[] headerBuffer1; // HTTP header buffer no 1.

    protected byte[] headerBuffer2; // HTTP header buffer no 2.

    protected byte[] bodyBuffer; // HTTP body buffer.

    protected char[] headerBuffer; // US-ASCII header buffer.

    protected InputStream inputStream; // Underlying input stream.

    protected InputBuffer inputStreamInputBuffer; // Underlying input buffer.

    protected InputFilter[] filterLibrary; // Filter library|Note: Filter[0] is always the "chunked" filter.

    protected InputFilter[] activeFilters; // Active filters (in order).

    protected int lastActiveFilter; // Index of the last active filter.

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void addFilter(InputFilter filter) {
        InputFilter[] newFilterLibrary = new InputFilter[filterLibrary.length + 1];
        for (int i = 0; i < filterLibrary.length; i++) {
            newFilterLibrary[i] = filterLibrary[i];
        }
        newFilterLibrary[filterLibrary.length] = filter;
        filterLibrary = newFilterLibrary;

        activeFilters = new InputFilter[filterLibrary.length];
    }

    public InputFilter[] getFilters() {
        return filterLibrary;
    }

    public void clearFilters() {
        filterLibrary = new InputFilter[0];
        lastActiveFilter = -1;
    }

    public void addActiveFilter(InputFilter filter) {
        if (lastActiveFilter == -1) {
            filter.setBuffer(inputStreamInputBuffer);
        } else {
            for (int i = 0; i <= lastActiveFilter; i++) {
                if (activeFilters[i] == filter)
                    return;
            }
            filter.setBuffer(activeFilters[lastActiveFilter]);
        }

        activeFilters[++lastActiveFilter] = filter;

        filter.setRequest(request);
    }

    public void setSwallowInput(boolean swallowInput) {
        this.swallowInput = swallowInput;
    }

    /**
     * Recycle the input buffer. This should be called when closing the
     * connection.
     */
    public void recycle() {

        // Recycle Request object
        request.recycle();

        inputStream = null;
        buf = headerBuffer1;
        lastValid = 0;
        pos = 0;
        lastActiveFilter = -1;
        parsingHeader = true;
        swallowInput = true;

    }

    /**
     * End processing of current HTTP request.
     * Note: All bytes of the current request should have been already
     * consumed. This method only resets all the pointers so that we are ready
     * to parse the next HTTP request.
     */
    public void nextRequest() throws IOException {

        // Recycle Request object
        request.recycle();

        // Determine the header buffer used for next request
        byte[] newHeaderBuf = null;
        if (buf == headerBuffer1) {
            newHeaderBuf = headerBuffer2;
        } else {
            newHeaderBuf = headerBuffer1;
        }

        // Copy leftover bytes from buf to newHeaderBuf
        System.arraycopy(buf, pos, newHeaderBuf, 0, lastValid - pos);

        // Swap buffers
        buf = newHeaderBuf;

        // Recycle filters
        for (int i = 0; i <= lastActiveFilter; i++) {
            activeFilters[i].recycle();
        }

        // Reset pointers
        lastValid = lastValid - pos;
        pos = 0;
        lastActiveFilter = -1;
        parsingHeader = true;
        swallowInput = true;

    }

    /**
     * End request (consumes leftover bytes).
     * 
     * @throws IOException
     *             an undelying I/O error occured
     */
    public void endRequest() throws IOException {

        if (swallowInput && (lastActiveFilter != -1)) {
            int extraBytes = (int) activeFilters[lastActiveFilter].end();
            pos = pos - extraBytes;
        }

    }

    /**
     * Read the request line. This function is meant to be used during the
     * HTTP request header parsing. Do NOT attempt to read the request body
     * using it.
     * 
     * @throws IOException
     *             If an exception occurs during the underlying socket
     *             read operations, or if the given buffer is not big enough to accomodate
     *             the whole line.
     */
    public void parseRequestLine() throws IOException {
        int start = 0;
        byte chr = 0;
        do {
            // Read new bytes if needed
            if (pos >= lastValid) {
                if (!fill())
                    throw new EOFException(sm.getString("iib.eof.error"));
            }
            chr = buf[pos++];
        } while ((chr == Constants.CR) || (chr == Constants.LF));

        pos--;

        start = pos; // Mark the current buffer position

        boolean space = false;

        while (!space) {
            // Read new bytes if needed
            if (pos >= lastValid) {
                if (!fill()) {
                    throw new EOFException(sm.getString("iib.eof.error"));
                }
            }

            ascbuf[pos] = (char) buf[pos];
            if (buf[pos] == Constants.SP) {
                space = true;
                request.method().setChars(ascbuf, start, pos - start);
            }

            pos++;
        }

        // Mark the current buffer position
        start = pos;
        int end = 0;
        int questionPos = -1;

        // Reading the URI
        space = false;
        boolean eol = false;

        while (!space) {

            // Read new bytes if needed
            if (pos >= lastValid) {
                if (!fill())
                    throw new EOFException(sm.getString("iib.eof.error"));
            }

            if (buf[pos] == Constants.SP) {
                space = true;
                end = pos;
            } else if ((buf[pos] == Constants.CR) || (buf[pos] == Constants.LF)) {
                // HTTP/0.9 style request
                eol = true;
                space = true;
                end = pos;
            } else if ((buf[pos] == Constants.QUESTION) && (questionPos == -1)) {
                questionPos = pos;
            }

            pos++;

        }

        request.unparsedURI().setBytes(buf, start, end - start);
        if (questionPos >= 0) {
            request.queryString().setBytes(buf, questionPos + 1, end - questionPos - 1);
            request.requestURI().setBytes(buf, start, questionPos - start);
        } else {
            request.requestURI().setBytes(buf, start, end - start);
        }

        // Mark the current buffer position
        start = pos;
        end = 0;

        //
        // Reading the protocol
        // Protocol is always US-ASCII
        //

        while (!eol) {

            // Read new bytes if needed
            if (pos >= lastValid) {
                if (!fill())
                    throw new EOFException(sm.getString("iib.eof.error"));
            }

            ascbuf[pos] = (char) buf[pos];

            if (buf[pos] == Constants.CR) {
                end = pos;
            } else if (buf[pos] == Constants.LF) {
                if (end == 0)
                    end = pos;
                eol = true;
            }

            pos++;

        }

        if ((end - start) > 0) {
            request.protocol().setChars(ascbuf, start, end - start);
        } else {
            request.protocol().setString("");
        }

    }

    /**
     * Parse the HTTP headers.
     */
    public void parseHeaders() throws IOException {

        while (parseHeader()) {
        }

        parsingHeader = false;

    }

    /**
     * Parse an HTTP header.
     * 
     * @return false after reading a blank line (which indicates that the
     *         HTTP header parsing is done
     */
    public boolean parseHeader() throws IOException {

        //
        // Check for blank line
        //

        byte chr = 0;
        while (true) {

            // Read new bytes if needed
            if (pos >= lastValid) {
                if (!fill())
                    throw new EOFException(sm.getString("iib.eof.error"));
            }

            chr = buf[pos];

            if ((chr == Constants.CR) || (chr == Constants.LF)) {
                if (chr == Constants.LF) {
                    pos++;
                    return false;
                }
            } else {
                break;
            }

            pos++;

        }

        // Mark the current buffer position
        int start = pos;

        //
        // Reading the header name
        // Header name is always US-ASCII
        //

        boolean colon = false;
        MessageBytes headerValue = null;

        while (!colon) {

            // Read new bytes if needed
            if (pos >= lastValid) {
                if (!fill())
                    throw new EOFException(sm.getString("iib.eof.error"));
            }

            if (buf[pos] == Constants.COLON) {
                colon = true;
                headerValue = headers.addValue(ascbuf, start, pos - start);
            }
            chr = buf[pos];
            if ((chr >= Constants.A) && (chr <= Constants.Z)) {
                buf[pos] = (byte) (chr - Constants.LC_OFFSET);
            }

            ascbuf[pos] = (char) buf[pos];

            pos++;

        }

        // Mark the current buffer position
        start = pos;
        int realPos = pos;

        //
        // Reading the header value (which can be spanned over multiple lines)
        //

        boolean eol = false;
        boolean validLine = true;

        while (validLine) {

            boolean space = true;

            // Skipping spaces
            while (space) {

                // Read new bytes if needed
                if (pos >= lastValid) {
                    if (!fill())
                        throw new EOFException(sm.getString("iib.eof.error"));
                }

                if ((buf[pos] == Constants.SP) || (buf[pos] == Constants.HT)) {
                    pos++;
                } else {
                    space = false;
                }

            }

            int lastSignificantChar = realPos;

            // Reading bytes until the end of the line
            while (!eol) {

                // Read new bytes if needed
                if (pos >= lastValid) {
                    if (!fill())
                        throw new EOFException(sm.getString("iib.eof.error"));
                }

                if (buf[pos] == Constants.CR) {
                } else if (buf[pos] == Constants.LF) {
                    eol = true;
                } else if (buf[pos] == Constants.SP) {
                    buf[realPos] = buf[pos];
                    realPos++;
                } else {
                    buf[realPos] = buf[pos];
                    realPos++;
                    lastSignificantChar = realPos;
                }

                pos++;

            }

            realPos = lastSignificantChar;

            // Checking the first character of the new line. If the character
            // is a LWS, then it's a multiline header

            // Read new bytes if needed
            if (pos >= lastValid) {
                if (!fill())
                    throw new EOFException(sm.getString("iib.eof.error"));
            }

            chr = buf[pos];
            if ((chr != Constants.SP) && (chr != Constants.HT)) {
                validLine = false;
            } else {
                eol = false;
                // Copying one extra space in the buffer (since there must
                // be at least one space inserted between the lines)
                buf[realPos] = chr;
                realPos++;
            }

        }

        // Set the header value
        headerValue.setBytes(buf, start, realPos - start);

        return true;

    }

    // ---------------------------------------------------- InputBuffer Methods

    /**
     * Read some bytes.
     */
    public int doRead(ByteChunk chunk, Request req) throws IOException {

        if (lastActiveFilter == -1)
            return inputStreamInputBuffer.doRead(chunk, req);
        else
            return activeFilters[lastActiveFilter].doRead(chunk, req);

    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Fill the internal buffer using data from the undelying input stream.
     * 
     * @return false if at end of stream
     */
    protected boolean fill() throws IOException {
        int nRead = 0;
        if (parsingHeader) {
            if (lastValid == buf.length) {
                throw new IOException(sm.getString("iib.requestheadertoolarge.error"));
            }

            nRead = inputStream.read(buf, pos, buf.length - lastValid);
            if (nRead > 0) {
                lastValid = pos + nRead;
            }

        } else {

            buf = bodyBuffer;
            pos = 0;
            lastValid = 0;
            nRead = inputStream.read(buf, 0, buf.length);
            if (nRead > 0) {
                lastValid = nRead;
            }

        }

        return (nRead > 0);

    }

    // ------------------------------------- InputStreamInputBuffer Inner Class

    /**
     * This class is an input buffer which will read its data from an input
     * stream.
     */
    protected class InputStreamInputBuffer implements InputBuffer {

        /**
         * Read bytes into the specified chunk.
         */
        public int doRead(ByteChunk chunk, Request req) throws IOException {

            if (pos >= lastValid) {
                if (!fill())
                    return -1;
            }

            int length = lastValid - pos;
            chunk.setBytes(buf, pos, length);
            pos = lastValid;

            return (length);

        }

    }

}
