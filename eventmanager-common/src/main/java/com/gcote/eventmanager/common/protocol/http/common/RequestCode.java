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

package com.gcote.eventmanager.common.protocol.http.common;

public enum RequestCode {

    MSG_BATCH_SEND(102, "ENVIO BATCH"),

    MSG_BATCH_SEND_V2(107, "ENVIO BATCH V2"),

    MSG_SEND_SYNC(101, "ENVIO MENSAJE SINCRONO"),

    MSG_SEND_ASYNC(104, "ENVIO MENSAJE ASINCRONO"),

    HTTP_PUSH_CLIENT_ASYNC(105, "PUSH CLIENT BY HTTP POST"),

    HTTP_PUSH_CLIENT_SYNC(106, "PUSH CLIENT BY HTTP POST"),

    REGISTER(201, "REGISTRADO"),

    UNREGISTER(202, "NO REGISTRADO"),

    HEARTBEAT(203, "HEARTBEAT"),

    SUBSCRIBE(206, "SUBSCRIBE"),

    UNSUBSCRIBE(207, "UNSUBSCRIBE"),

    REPLY_MESSAGE(301, "REPLY_MESSAGE"),

    ADMIN_METRICS(603, "ADMIN_METRICS"),

    ADMIN_SHUTDOWN(601, "ADMIN_SHUTDOWN");

    private Integer requestCode;

    private String desc;

    RequestCode(Integer requestCode, String desc) {
        this.requestCode = requestCode;
        this.desc = desc;
    }

    public static boolean contains(Integer requestCode) {
        boolean flag = false;
        for (RequestCode itr : RequestCode.values()) {
            if (itr.requestCode.intValue() == requestCode.intValue()) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static RequestCode get(Integer requestCode) {
        RequestCode ret = null;
        for (RequestCode itr : RequestCode.values()) {
            if (itr.requestCode.intValue() == requestCode.intValue()) {
                ret = itr;
                break;
            }
        }
        return ret;
    }

    public Integer getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(Integer requestCode) {
        this.requestCode = requestCode;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}
