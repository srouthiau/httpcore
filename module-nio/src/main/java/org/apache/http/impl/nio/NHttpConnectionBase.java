/*
 * $HeadURL$
 * $Revision$
 * $Date$
 *
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.impl.nio;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.apache.http.ConnectionClosedException;
import org.apache.http.Header;
import org.apache.http.HttpConnectionMetrics;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.HttpConnectionMetricsImpl;
import org.apache.http.impl.entity.LaxContentLengthStrategy;
import org.apache.http.impl.entity.StrictContentLengthStrategy;
import org.apache.http.impl.io.HttpTransportMetricsImpl;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpConnection;
import org.apache.http.impl.nio.codecs.ChunkDecoder;
import org.apache.http.impl.nio.codecs.ChunkEncoder;
import org.apache.http.impl.nio.codecs.IdentityDecoder;
import org.apache.http.impl.nio.codecs.IdentityEncoder;
import org.apache.http.impl.nio.codecs.LengthDelimitedDecoder;
import org.apache.http.impl.nio.codecs.LengthDelimitedEncoder;
import org.apache.http.impl.nio.reactor.SessionInputBufferImpl;
import org.apache.http.impl.nio.reactor.SessionOutputBufferImpl;
import org.apache.http.nio.reactor.EventMask;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.reactor.SessionBufferStatus;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.SyncBasicHttpContext;

/**
 * This class serves as a base for all {@link NHttpConnection} implementations
 * and implements functionality common to both client and server 
 * HTTP connections.
 * 
 *
 * @version $Revision$
 *
 * @since 4.0
 */
public class NHttpConnectionBase 
        implements NHttpConnection, HttpInetConnection, SessionBufferStatus {

    protected final HttpContext context;
    
    protected final ContentLengthStrategy incomingContentStrategy;
    protected final ContentLengthStrategy outgoingContentStrategy;
    
    protected final SessionInputBufferImpl inbuf;
    protected final SessionOutputBufferImpl outbuf;
    
    protected final HttpTransportMetricsImpl inTransportMetrics;
    protected final HttpTransportMetricsImpl outTransportMetrics;
    protected final HttpConnectionMetricsImpl connMetrics;
    
    protected IOSession session;
    protected volatile ContentDecoder contentDecoder;
    protected volatile boolean hasBufferedInput;
    protected volatile ContentEncoder contentEncoder;
    protected volatile boolean hasBufferedOutput;
    protected volatile HttpRequest request;
    protected volatile HttpResponse response;

    protected volatile int status;
    
    /**
     * Creates a new instance of this class given the underlying I/O session.
     * <p>
     * The following HTTP parameters affect configuration of this connection:
     * <p>
     * The {@link CoreConnectionPNames#SOCKET_BUFFER_SIZE}
     * parameter determines the size of the internal socket buffer. If not 
     * defined or set to <code>-1</code> the default value will be chosen
     * automatically.
     * 
     * @param session the underlying I/O session.
     * @param allocator byte buffer allocator.
     * @param params HTTP parameters.
     */
    public NHttpConnectionBase(
            final IOSession session,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super();
        if (session == null) {
            throw new IllegalArgumentException("I/O session may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("HTTP params may not be null");
        }
        this.session = session;
        this.context = new SyncBasicHttpContext(null);
        
        int buffersize = HttpConnectionParams.getSocketBufferSize(params);
        int linebuffersize = buffersize;
        if (linebuffersize > 512) {
            linebuffersize = 512;
        }
        
        this.inbuf = new SessionInputBufferImpl(buffersize, linebuffersize, allocator, params); 
        this.outbuf = new SessionOutputBufferImpl(buffersize, linebuffersize, allocator, params); 
        
        this.incomingContentStrategy = new LaxContentLengthStrategy();
        this.outgoingContentStrategy = new StrictContentLengthStrategy();
        
        this.inTransportMetrics = new HttpTransportMetricsImpl();
        this.outTransportMetrics = new HttpTransportMetricsImpl();
        this.connMetrics = new HttpConnectionMetricsImpl(
                this.inTransportMetrics, 
                this.outTransportMetrics);
        
        this.session.setBufferStatus(this);
        this.session.setEvent(EventMask.READ);
        this.status = ACTIVE;
    }

    public int getStatus() {
        return this.status;
    }

    public HttpContext getContext() {
        return this.context;
    }

    public HttpRequest getHttpRequest() {
        return this.request;
    }

    public HttpResponse getHttpResponse() {
        return this.response;
    }

    public void requestInput() {
        this.session.setEvent(EventMask.READ);
    }

    public void requestOutput() {
        this.session.setEvent(EventMask.WRITE);
    }

    public void suspendInput() {
        this.session.clearEvent(EventMask.READ);
    }

    public void suspendOutput() {
        this.session.clearEvent(EventMask.WRITE);
    }

    /**
     * Initializes a specific {@link ContentDecoder} implementation based on the
     * properties of the given {@link HttpMessage} and generates an instance of
     * {@link HttpEntity} matching the properties of the content decoder.
     * 
     * @param message the HTTP message.
     * @return HTTP entity.
     * @throws HttpException in case of an HTTP protocol violation.
     */
    protected HttpEntity prepareDecoder(final HttpMessage message) throws HttpException {
        BasicHttpEntity entity = new BasicHttpEntity();
        long len = this.incomingContentStrategy.determineLength(message);
        if (len == ContentLengthStrategy.CHUNKED) {
            this.contentDecoder = new ChunkDecoder(
                    this.session.channel(), 
                    this.inbuf, 
                    this.inTransportMetrics);
            entity.setChunked(true);
            entity.setContentLength(-1);
        } else if (len == ContentLengthStrategy.IDENTITY) {
            this.contentDecoder = new IdentityDecoder(
                    this.session.channel(), 
                    this.inbuf, 
                    this.inTransportMetrics);
            entity.setChunked(false);
            entity.setContentLength(-1);
        } else {
            this.contentDecoder = new LengthDelimitedDecoder(
                    this.session.channel(), 
                    this.inbuf, 
                    this.inTransportMetrics,
                    len);
            entity.setChunked(false);
            entity.setContentLength(len);
        }
        
        Header contentTypeHeader = message.getFirstHeader(HTTP.CONTENT_TYPE);
        if (contentTypeHeader != null) {
            entity.setContentType(contentTypeHeader);    
        }
        Header contentEncodingHeader = message.getFirstHeader(HTTP.CONTENT_ENCODING);
        if (contentEncodingHeader != null) {
            entity.setContentEncoding(contentEncodingHeader);    
        }
        return entity;
    }

    /**
     * Initializes a specific {@link ContentEncoder} implementation based on the
     * properties of the given {@link HttpMessage}.
     * 
     * @param message the HTTP message.
     * @throws HttpException in case of an HTTP protocol violation.
     */
    protected void prepareEncoder(final HttpMessage message) throws HttpException {
        long len = this.outgoingContentStrategy.determineLength(message);
        if (len == ContentLengthStrategy.CHUNKED) {
            this.contentEncoder = new ChunkEncoder(
                    this.session.channel(),
                    this.outbuf,
                    this.outTransportMetrics);
        } else if (len == ContentLengthStrategy.IDENTITY) {
            this.contentEncoder = new IdentityEncoder(
                    this.session.channel(),
                    this.outbuf,
                    this.outTransportMetrics);
        } else {
            this.contentEncoder = new LengthDelimitedEncoder(
                    this.session.channel(),
                    this.outbuf,
                    this.outTransportMetrics,
                    len);
        }
    }

    public boolean hasBufferedInput() {
        return this.hasBufferedInput;
    }

    public boolean hasBufferedOutput() {
        return this.hasBufferedOutput;
    }
    
    /**
     * Assets if the connection is still open.
     * 
     * @throws ConnectionClosedException in case the connection has already 
     *   been closed.
     */
    protected void assertNotClosed() throws ConnectionClosedException {
        if (this.status != ACTIVE) {
            throw new ConnectionClosedException("Connection is closed");
        }
    }

    public void close() throws IOException {
        if (this.status != ACTIVE) {
            return;
        }
        this.status = CLOSING;
        if (this.outbuf.hasData()) {
            this.session.setEvent(EventMask.WRITE);
        } else {
            this.session.close();
            this.status = CLOSED;
        }
    }

    public boolean isOpen() {
        return this.status == ACTIVE && !this.session.isClosed();
    }

    public boolean isStale() {
        return this.session.isClosed();
    }
    
    public InetAddress getLocalAddress() {
        SocketAddress address = this.session.getLocalAddress();
        if (address instanceof InetSocketAddress) {
            return ((InetSocketAddress) address).getAddress();
        } else {
            return null;
        }
    }

    public int getLocalPort() {
        SocketAddress address = this.session.getLocalAddress();
        if (address instanceof InetSocketAddress) {
            return ((InetSocketAddress) address).getPort();
        } else {
            return -1;
        }
    }

    public InetAddress getRemoteAddress() {
        SocketAddress address = this.session.getRemoteAddress();
        if (address instanceof InetSocketAddress) {
            return ((InetSocketAddress) address).getAddress();
        } else {
            return null;
        }
    }

    public int getRemotePort() {
        SocketAddress address = this.session.getRemoteAddress();
        if (address instanceof InetSocketAddress) {
            return ((InetSocketAddress) address).getPort();
        } else {
            return -1;
        }
    }

    public void setSocketTimeout(int timeout) {
        this.session.setSocketTimeout(timeout);
    }

    public int getSocketTimeout() {
        return this.session.getSocketTimeout();
    }

    public void shutdown() throws IOException {
        this.status = CLOSED;
        this.session.shutdown();
    }

    public HttpConnectionMetrics getMetrics() {
        return this.connMetrics;
    }
    
}
