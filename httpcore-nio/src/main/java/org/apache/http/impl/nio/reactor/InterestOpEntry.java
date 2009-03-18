/*
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

package org.apache.http.impl.nio.reactor;

/**
 * Helper class, representing an element on an {@link java.nio.channels.SelectionKey#interestOps(int)
 * interestOps(int)} queue.
 */
class InterestOpEntry {
    /**
        Constant indicating the <CODE>OPERATION_TYPE_SET_EVENT_MASK</CODE> operation type.
    */
    public static final int OPERATION_TYPE_SET_EVENT_MASK = 0;

    /**
        Constant indicating the <CODE>OPERATION_TYPE_SET_EVENT</CODE> operation type.
    */
    public static final int OPERATION_TYPE_SET_EVENT = 1;

    /**
        Constant indicating the <CODE>OPERATION_TYPE_CLEAR_EVENT</CODE> operation type.
    */
    public static final int OPERATION_TYPE_CLEAR_EVENT = 2;

    // instance members
    private IOSessionImpl ioSession;
    private int operationType;
    private int operationArgument;

    /**
        Default constructor for the <CODE>IOSessionQueueElement</CODE> class.
    */
    public InterestOpEntry() {
        // initialize instance members
        ioSession = null;
        operationType = 0;
        operationArgument = 0;
    }

    /**
        Constructor for the <CODE>IOSessionQueueElement</CODE> class.
    */
    public InterestOpEntry(IOSessionImpl ioSession, int operationType, int operationArgument) {
        // initialize instance members
        this.ioSession = ioSession;
        this.operationType = operationType;
        this.operationArgument = operationArgument;
    }

    /**
        Getter for the <CODE>ioSession</CODE> property.
    */
    public IOSessionImpl getIoSession() {
        return ioSession;
    }

    /**
        Setter for the <CODE>ioSession</CODE> property.
    */
    public void setIoSession(IOSessionImpl ioSession) {
        this.ioSession = ioSession;
    }

    /**
        Getter for the <CODE>operationType</CODE> property.
    */
    public int getOperationType() {
        return operationType;
    }

    /**
        Setter for the <CODE>operationType</CODE> property.
    */
    public void setOperationType(int operationType) {
        this.operationType = operationType;
    }

    /**
        Getter for the <CODE>operationArgument</CODE> property.
    */
    public int getOperationArgument() {
        return operationArgument;
    }

    /**
        Setter for the <CODE>operationArgument</CODE> property.
    */
    public void setOperationArgument(int operationArgument) {
        this.operationArgument = operationArgument;
    }

}
