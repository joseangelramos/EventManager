/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gcote.eventmanager.common.protocol.tcp;

public enum Command {

    //HEARTBEAT
    HEARTBEAT_REQUEST(0),
    HEARTBEAT_RESPONSE(1),

    //HANDSHAKE
    HELLO_REQUEST(2),
    HELLO_RESPONSE(3),

    //DISCONNECTED
    CLIENT_GOODBYE_REQUEST(4),
    CLIENT_GOODBYE_RESPONSE(5),
    SERVER_GOODBYE_REQUEST(6),
    SERVER_GOODBYE_RESPONSE(7),

    //SUBSCRIBES
    SUBSCRIBE_REQUEST(8),
    SUBSCRIBE_RESPONSE(9),
    UNSUBSCRIBE_REQUEST(10),
    UNSUBSCRIBE_RESPONSE(11),

    //LISTENER
    LISTEN_REQUEST(12),
    LISTEN_RESPONSE(13),

    //REQUESTS
    REQUEST_TO_SERVER(14),
    REQUEST_TO_CLIENT(15),
    REQUEST_TO_CLIENT_ACK(16),
    RESPONSE_TO_SERVER(17),
    RESPONSE_TO_CLIENT(18),
    RESPONSE_TO_CLIENT_ACK(19),

    //ASYNC MESSAGES
    ASYNC_MESSAGE_TO_SERVER(20),
    ASYNC_MESSAGE_TO_SERVER_ACK(21),
    ASYNC_MESSAGE_TO_CLIENT(22),
    ASYNC_MESSAGE_TO_CLIENT_ACK(23),

    //BROADCAST
    BROADCAST_MESSAGE_TO_SERVER(24),
    BROADCAST_MESSAGE_TO_SERVER_ACK(25),
    BROADCAST_MESSAGE_TO_CLIENT(26),
    BROADCAST_MESSAGE_TO_CLIENT_ACK(27),

    //SYS_LOG
    SYS_LOG_TO_LOGSERVER(28),

    //TRACING_LOG
    TRACE_LOG_TO_LOGSERVER(29),

    //REDIRECT
    REDIRECT_TO_CLIENT(30),

    //REGISTRATION
    REGISTER_REQUEST(31),
    REGISTER_RESPONSE(32),

    //UNREGISTER
    UNREGISTER_REQUEST(33),
    UNREGISTER_RESPONSE(34),

    //PROXY RECOMMENDATION
    RECOMMEND_REQUEST(35),
    RECOMMEND_RESPONSE(36);

    private final byte value;

    Command(int value) {
        this.value = (byte) value;
    }

    public byte value() {
        return this.value;
    }

    public static Command valueOf(int value) {
        for (Command t : Command.values()) {
            if (t.value == value) {
                return t;
            }
        }
        throw new IllegalArgumentException("No enum constant value=" + value);
    }
}
