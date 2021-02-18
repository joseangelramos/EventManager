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

package com.gcote.eventmanager.common.protocol.http.header.client;


import com.gcote.eventmanager.common.Constants;
import com.gcote.eventmanager.common.protocol.http.common.ProtocolKey;
import com.gcote.eventmanager.common.protocol.http.common.ProtocolVersion;
import com.gcote.eventmanager.common.protocol.http.header.Header;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class HeartbeatRequestHeader extends Header {

    //Codigo Solicitud
    private String code;

    //Idioma del Solicitante
    private String language;

    //Version de protocolo usada por el solicitante
    private ProtocolVersion version;

    //Numero de entorno del solicitante
    private String env;

    //Codigo de region del solicitante
    private String region;

    //IDC donde se encuentra el solicitante
    private String idc;

    //El DCN de la parte solicitante
    private String dcn;

    //Subsistema solicitante
    private String sys;

    //ID de proceso del solicitante
    private String pid;

    //IP del solicitante
    private String ip;

    //USERNAME de la parte solicitante
    private String username = "username";

    //PASSWD del solicitante
    private String passwd = "user@123";

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public ProtocolVersion getVersion() {
        return version;
    }

    public void setVersion(ProtocolVersion version) {
        this.version = version;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getIdc() {
        return idc;
    }

    public void setIdc(String idc) {
        this.idc = idc;
    }

    public String getDcn() {
        return dcn;
    }

    public void setDcn(String dcn) {
        this.dcn = dcn;
    }

    public String getSys() {
        return sys;
    }

    public void setSys(String sys) {
        this.sys = sys;
    }

    public String getPid() {
        return pid;
    }

    public void setPid(String pid) {
        this.pid = pid;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswd() {
        return passwd;
    }

    public void setPasswd(String passwd) {
        this.passwd = passwd;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ProtocolKey.REQUEST_CODE, code);
        map.put(ProtocolKey.LANGUAGE, language);
        map.put(ProtocolKey.VERSION, version);
        map.put(ProtocolKey.ClientInstanceKey.ENV, env);
        map.put(ProtocolKey.ClientInstanceKey.REGION, region);
        map.put(ProtocolKey.ClientInstanceKey.IDC, idc);
        map.put(ProtocolKey.ClientInstanceKey.DCN, dcn);
        map.put(ProtocolKey.ClientInstanceKey.SYS, sys);
        map.put(ProtocolKey.ClientInstanceKey.PID, pid);
        map.put(ProtocolKey.ClientInstanceKey.IP, ip);
        map.put(ProtocolKey.ClientInstanceKey.USERNAME, username);
        map.put(ProtocolKey.ClientInstanceKey.PASSWD, passwd);
        return map;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("heartbeatRequestHeader={")
                .append("code=").append(code).append(",")
                .append("language=").append(language).append(",")
                .append("version=").append(version).append(",")
                .append("env=").append(env).append(",")
                .append("region=").append(region).append(",")
                .append("idc=").append(idc).append(",")
                .append("dcn=").append(dcn).append(",")
                .append("sys=").append(sys).append(",")
                .append("pid=").append(pid).append(",")
                .append("ip=").append(ip).append(",")
                .append("username=").append(username).append(",")
                .append("passwd=").append(passwd).append("}");
        return sb.toString();
    }

    public static HeartbeatRequestHeader buildHeader(Map<String, Object> headerParam) {
        HeartbeatRequestHeader header = new HeartbeatRequestHeader();
        header.setCode(MapUtils.getString(headerParam, ProtocolKey.REQUEST_CODE));
        header.setVersion(ProtocolVersion.get(MapUtils.getString(headerParam, ProtocolKey.VERSION)));
        String lan = StringUtils.isBlank(MapUtils.getString(headerParam, ProtocolKey.LANGUAGE))
                ? Constants.LANGUAGE_JAVA : MapUtils.getString(headerParam, ProtocolKey.LANGUAGE);
        header.setLanguage(lan);
        header.setEnv(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.ENV));
        header.setRegion(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.REGION));
        header.setIdc(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.IDC));
        header.setDcn(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.DCN));
        header.setSys(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.SYS));
        header.setPid(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.PID));
        header.setIp(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.IP));
        header.setUsername(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.USERNAME));
        header.setPasswd(MapUtils.getString(headerParam, ProtocolKey.ClientInstanceKey.PASSWD));
        return header;
    }
}
