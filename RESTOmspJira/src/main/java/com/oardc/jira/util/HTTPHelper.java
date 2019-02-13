/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/HTTPHelper.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

/* Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved. */

/*
   DESCRIPTION
    <short description of component this file declares/defines>

   PRIVATE CLASSES
    <list of private classes defined - with one-line descriptions>

   NOTES
    <other useful comments, qualifications, etc.>

   MODIFIED    (MM/DD/YY)
    dalai       01/24/13 - Creation
 */

/**
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/HTTPHelper.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

import HTTPClient.HTTPConnection;
import HTTPClient.NVPair;
import HTTPClient.Request;

import java.io.UnsupportedEncodingException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class HTTPHelper {
    private static final NVPair[] DEFAULT_HEADERS;
    static {
        DEFAULT_HEADERS = new NVPair[2];
        DEFAULT_HEADERS[0] = new NVPair("Accept", "*/*");
        DEFAULT_HEADERS[1] =
                new NVPair("User-Agent-Noversion", "Mozilla/4.0 (compatible; Windows NT 5.1) OracleEMAgentURLTiming/3.0");
    }

    private HTTPHelper() {
    }

    /**
     * Adds the default HTTP headers to the given request.
     *
     * @param request
     */
    public static void setHTTPHeaders(Request request) {
        setHTTPHeaders(request, null);
    }

    /**
     * Adds the default HTTP headers and the given HTTP headers to the request.
     *
     * @param request
     * @param headers
     */
    public static void setHTTPHeaders(Request request, NVPair[] headers) {
        NVPair[] allHeaders;
        if (headers != null) {
            allHeaders = new NVPair[DEFAULT_HEADERS.length + headers.length];
            System.arraycopy(DEFAULT_HEADERS, 0, allHeaders, 0,
                             DEFAULT_HEADERS.length);
            System.arraycopy(headers, 0, allHeaders, DEFAULT_HEADERS.length,
                             headers.length);
        } else {
            allHeaders = DEFAULT_HEADERS;
        }
        request.setHeaders(allHeaders);
    }

    /**
     * Returns the content type used to encode the HTML form data set.
     *
     * @return
     */
    public static NVPair getFormURLEncodedContentType() {
        NVPair contentType =
            new NVPair("Content-Type", "application/x-www-form-urlencoded");
        return contentType;
    }

    /**
     * Associates the default HTTP headers with a HTTPConnection.
     * @param conn
     */
    public static void setDefaultHeaders(HTTPConnection conn) {
        conn.setDefaultHeaders(DEFAULT_HEADERS);
    }

    /**
     * Parses the given URI and picks out all the HTTP request parameters.
     *
     * @param requestURI
     * @return
     * @throws URISyntaxException
     */
    public static Map extractUriQueryParamsMap(String requestURI) throws URISyntaxException {
        Map urlQueryParamsMap;

        URI uri = new URI(requestURI);
        String query = uri.getQuery();
        urlQueryParamsMap = extractQueryParamsMap(query);
        return urlQueryParamsMap;
    }

    /**
     * Parses the query String to creates a Map.
     *
     * @param queryString
     * @return
     */
    public static Map extractQueryParamsMap(String queryString) {
        Map urlQueryParamsMap = new HashMap();
        if (queryString == null || "".equals(queryString)) {
            return urlQueryParamsMap;
        }

        String[] queryParams = queryString.split("&");
        for (int i = 0; i < queryParams.length; i++) {
            String[] paramData = queryParams[i].split("=");
            String paramName = paramData[0];
            String paramValue = "";
            if (paramData.length > 1) {
                paramValue = paramData[1];
            }
            urlQueryParamsMap.put(paramName, paramValue);
        }
        return urlQueryParamsMap;
    }

    /**
     * Use the data in the Map to create an array of NVPair objects.
     *
     * @param paramsMap
     * @return
     */
    public static NVPair[] getNVPairs(Map paramsMap) {
        NVPair[] nvPairs = new NVPair[0];
        if (paramsMap != null && paramsMap.size() > 0) {
            nvPairs = new NVPair[paramsMap.size()];
            Iterator paramsMapIter = paramsMap.entrySet().iterator();
            int pos = 0;
            while (paramsMapIter.hasNext()) {
                Map.Entry entry = (Map.Entry)paramsMapIter.next();
                nvPairs[pos++] =
                        new NVPair((String)entry.getKey(), (String)entry.getValue());
            }
        }
        return nvPairs;
    }

    /**
     * Translates a string into application/x-www-form-urlencoded  format.
     *
     * @param str
     * @return
     */
    public static String encode(String str) {
        String outStr;
        if (str == null) {
            return null;
        }
        try {
            outStr = URLEncoder.encode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //Something unexpected happend
            throw new RuntimeException("Invalid enc: UTF-8", e);
        }
        return outStr;
    }

}
