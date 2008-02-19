/*
 * $HeadURL:$
 * $Revision:$
 * $Date:$
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

package org.apache.http.nio.entity;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AsyncNHttpServiceHandler;

/**
 * An entity whose content is retrieved from a byte array.
 * This entity is intended for use only as an {@link NHttpEntity}.
 * Blocking methods are not supported.
 *
 * @author <a href="mailto:sberlin at gmail.com">Sam Berlin</a>
 *
 * @version $Revision:$
 *
 * @see AsyncNHttpServiceHandler
 * @since 4.0
 */
public class NByteArrayEntity extends AbstractNHttpEntity implements ProducingNHttpEntity {

    protected final ByteBuffer buffer;

    public NByteArrayEntity(final byte[] b) {
        this.buffer = ByteBuffer.wrap(b);
    }

    public void finish() {
        buffer.rewind();
    }

    public void produceContent(ContentEncoder encoder, IOControl ioctrl)
            throws IOException {
        encoder.write(buffer);
        if(!buffer.hasRemaining())
            encoder.complete();
    }

    public long getContentLength() {
        return buffer.limit();
    }

    public boolean isRepeatable() {
        return true;
    }

}
