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

import org.apache.http.HttpEntity;

/**
 * A default implementation of {@link ConsumingNHttpEntity}.
 * Blocking output methods will throw {@link UnsupportedOperationException}.
 *
 * @author <a href="mailto:sberlin at gmail.com">Sam Berlin</a>
 */
public class BasicConsumingNHttpEntity extends AbstractNHttpEntity implements ConsumingNHttpEntity {

    private final ContentListener contentListener;
    private long contentLength;
    private boolean handled;

    public BasicConsumingNHttpEntity(ContentListener contentListener, HttpEntity httpEntity) {
        this.contentListener = contentListener;
        setChunked(httpEntity.isChunked());
        setContentEncoding(httpEntity.getContentEncoding());
        setContentLength(httpEntity.getContentLength());
        setContentType(httpEntity.getContentType());
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

    public boolean isHandled() {
        return handled;
    }

    /**
     * Specifies the length of the content.
     *
     * @param len       the number of bytes in the content, or
     *                  a negative number to indicate an unknown length
     */
    public void setContentLength(long len) {
        this.contentLength = len;
    }

    public ContentListener getContentListener() {
        return contentListener;
    }

    public long getContentLength() {
        return contentLength;
    }

    public boolean isRepeatable() {
        return false;
    }

}
