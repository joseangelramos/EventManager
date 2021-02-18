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

package com.gcote.eventmanager.common.protocol.http.header.message;


import com.gcote.eventmanager.common.protocol.http.common.ProtocolKey;
import com.gcote.eventmanager.common.protocol.http.header.Header;

import java.util.HashMap;
import java.util.Map;

public class ReplyMessageResponseHeader extends Header {

    //Codigo de respuesta, consistente con el codigo de la Solicitud correspondiente
    private int code;

    //El nombre del cluster del proxy que proceso la solicitud.
    private String proxyCluster;

    //La IP del proxy que proceso la solicitud
    private String proxyIp;

    //El numero de entorno del proxy que proceso la solicitud.
    private String proxyEnv;

    //El area del proxy que proceso la solicitud
    private String proxyRegion;

    //El IDC del proxy que proceso la solicitud
    private String proxyIdc;
    //El DCN del proxy que proceso la solicitud
    private String proxyDcn;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getProxyCluster() {
        return proxyCluster;
    }

    public void setProxyCluster(String proxyCluster) {
        this.proxyCluster = proxyCluster;
    }

    public String getProxyIp() {
        return proxyIp;
    }

    public void setProxyIp(String proxyIp) {
        this.proxyIp = proxyIp;
    }

    public String getProxyEnv() {
        return proxyEnv;
    }

    public void setProxyEnv(String proxyEnv) {
        this.proxyEnv = proxyEnv;
    }

    public String getProxyRegion() {
        return proxyRegion;
    }

    public void setProxyRegion(String proxyRegion) {
        this.proxyRegion = proxyRegion;
    }

    public String getProxyIdc() {
        return proxyIdc;
    }

    public void setProxyIdc(String proxyIdc) {
        this.proxyIdc = proxyIdc;
    }

    public String getProxyDcn() {
        return proxyDcn;
    }

    public void setProxyDcn(String proxyDcn) {
        this.proxyDcn = proxyDcn;
    }

    public static ReplyMessageResponseHeader buildHeader(Integer requestCode, String proxyCluster,
                                                        String proxyIp, String proxyEnv, String proxyRegion,
                                                         String proxyDcn, String proxyIDC) {
        ReplyMessageResponseHeader replyMessageResponseHeader = new ReplyMessageResponseHeader();
        replyMessageResponseHeader.setCode(requestCode);
        replyMessageResponseHeader.setProxyCluster(proxyCluster);
        replyMessageResponseHeader.setProxyDcn(proxyDcn);
        replyMessageResponseHeader.setProxyIp(proxyIp);
        replyMessageResponseHeader.setProxyEnv(proxyEnv);
        replyMessageResponseHeader.setProxyRegion(proxyRegion);
        replyMessageResponseHeader.setProxyIdc(proxyIDC);
        return replyMessageResponseHeader;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("replyMessageResponseHeader={")
                .append("code=").append(code).append(",")
                .append("proxyEnv=").append(proxyEnv).append(",")
                .append("proxyRegion=").append(proxyRegion).append(",")
                .append("proxyIdc=").append(proxyIdc).append(",")
                .append("proxyDcn=").append(proxyDcn).append(",")
                .append("proxyCluster=").append(proxyCluster).append(",")
                .append("proxyIp=").append(proxyIp).append("}");
        return sb.toString();
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ProtocolKey.REQUEST_CODE, code);
        map.put(ProtocolKey.ProxyInstanceKey.PROXYCLUSTER, proxyCluster);
        map.put(ProtocolKey.ProxyInstanceKey.PROXYIP, proxyIp);
        map.put(ProtocolKey.ProxyInstanceKey.PROXYENV, proxyEnv);
        map.put(ProtocolKey.ProxyInstanceKey.PROXYIDC, proxyIdc);
        map.put(ProtocolKey.ProxyInstanceKey.PROXYREGION, proxyRegion);
        map.put(ProtocolKey.ProxyInstanceKey.PROXYDCN, proxyDcn);
        return map;
    }
}
