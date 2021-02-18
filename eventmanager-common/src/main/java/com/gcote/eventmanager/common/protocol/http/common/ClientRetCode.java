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

public enum ClientRetCode {

    /**
     * Este REINTENTAR significa que cuando el cliente descubre que el mensaje entregado no está monitoreado,
     * le dice a PROXY que lo envíe al siguiente, vuelva a intentarlo varias veces para lograr la escala
     * de grises y reserve
     */

    OK(1, "ok, DEVOLUCION_SDK"),
    RETRY(2, "retry, DEVOLUCION_SDK, En este caso, debes probar como maximo max(default, config)"),
    FAIL(3, "fail, DEVOLUCION_SDK"),
    NOLISTEN(5, "Sin escuchar, DEVOLUCION_SDK, Se puede utilizar para la publicacion en escala de grises. En este caso, debe probar todas las URL");

    ClientRetCode(Integer retCode, String errMsg) {
        this.retCode = retCode;
        this.errMsg = errMsg;
    }

    public static boolean contains(Integer clientRetCode) {
        boolean flag = false;
        for (ClientRetCode itr : ClientRetCode.values()) {
            if (itr.retCode.intValue() == clientRetCode.intValue()) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    public static ClientRetCode get(Integer clientRetCode) {
        ClientRetCode ret = null;
        for (ClientRetCode itr : ClientRetCode.values()) {
            if (itr.retCode.intValue() == clientRetCode.intValue()) {
                ret = itr;
                break;
            }
        }
        return ret;
    }

    private Integer retCode;

    private String errMsg;

    public Integer getRetCode() {
        return retCode;
    }

    public void setRetCode(Integer retCode) {
        this.retCode = retCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }
}
