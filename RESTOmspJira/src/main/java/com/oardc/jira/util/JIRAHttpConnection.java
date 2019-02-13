/* $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAHttpConnection.java /main/3 2013/07/08 04:17:11 ohkambha Exp $ */

/* Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.*/

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
 *  @version $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAHttpConnection.java /main/3 2013/07/08 04:17:11 ohkambha Exp $
 *  @author  dalai
 *  @since   release specific (what release of product did this appear in)
 */



package com.oardc.jira.util;


import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.HttpOutputStream;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;
import HTTPClient.ParseException;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

//import com.oracle.bugjirabridge.util.LoggerUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.URL;

import java.util.Map;
//import java.util.logging.Logger;


import org.apache.log4j.Logger;

import org.xml.sax.SAXException;


/**
 *  Provides support for HTTP or HTTPS communication and SSO authentication.
 *
 *  @version $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAHttpConnection.java /main/3 2013/07/08 04:17:11 ohkambha Exp $
 *  @author  sujoseph
 *  @since   6.1
 */

public class JIRAHttpConnection extends HTTPConnection {
    private final static Logger s_log =
        Logger.getLogger(JIRAHttpConnection.class);

    private static final String RETURN_DATA_TYPE_STRING = "string";
    private static final String RETURN_DATA_TYPE_INPUT_STREAM = "inputStream";

    private String userName;
    private char[] password;

    /**
     * Constructs a connection to the specified host on proxyPort 80
     *
     * @param host
     */
    public JIRAHttpConnection(String host) {
        super(host);
        instanceCreated(null, null, null);
    }

    /**
     * Constructs a connection to the specified host on proxyPort 80.
     *
     * @param host
     * @param userName
     * @param password
     * @param sharedContext
     */
    public JIRAHttpConnection(String host, String userName, char[] password,
                            Object sharedContext) {
        super(host);
        instanceCreated(userName, password, sharedContext);
    }

    /**
     * Constructs a connection to the specified host on the specified proxyPort
     *
     * @param host
     * @param port
     */
    public JIRAHttpConnection(String host, int port) {
        super(host, port);
        instanceCreated(null, null, null);
    }

    /**
     * Constructs a connection to the specified host on the specified proxyPort
     *
     * @param host
     * @param port
     * @param userName
     * @param password
     * @param sharedContext
     */
    public JIRAHttpConnection(String host, int port, String userName,
                            char[] password, Object sharedContext) {
        super(host, port);
        instanceCreated(userName, password, sharedContext);
    }

    /**
     * Constructs a connection to the specified host on the specified proxyPort, using the specified protocol (http or https).
     *
     * @param protocol
     * @param host
     * @param port
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(String protocol, String host,
                            int port) throws ProtocolNotSuppException {
        super(protocol, host, port);
        instanceCreated(null, null, null);
    }

    /**
     * Constructs a connection to the specified host on the specified proxyPort, using the specified protocol (http or https).
     *
     * @param protocol
     * @param host
     * @param port
     * @param userName
     * @param password
     * @param sharedContext
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(String protocol, String host, int port,
                            String userName, char[] password,
                            Object sharedContext) throws ProtocolNotSuppException {
        super(protocol, host, port);
        instanceCreated(userName, password, sharedContext);
    }

    /**
     * Constructs a connection to the specified host on the specified proxyPort,
     * using the specified protocol (http or https), local address, and local proxyPort.
     *
     * @param protocol
     * @param host
     * @param port
     * @param localAddr
     * @param localPort
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(String protocol, String host, int port,
                            InetAddress localAddr,
                            int localPort) throws ProtocolNotSuppException {
        super(protocol, host, port, localAddr, localPort);
        instanceCreated(null, null, null);
    }

    /**
     * Constructs a connection to the specified host on the specified proxyPort,
     * using the specified protocol (http or https), local address, and local proxyPort.
     *
     * @param protocol
     * @param host
     * @param port
     * @param localAddr
     * @param localPort
     * @param userName
     * @param password
     * @param sharedContext
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(String protocol, String host, int port,
                            InetAddress localAddr, int localPort,
                            String userName, char[] password,
                            Object sharedContext) throws ProtocolNotSuppException {
        super(protocol, host, port, localAddr, localPort);
        instanceCreated(userName, password, sharedContext);
    }

    /**
     * Constructs a connection to the host (proxyPort) as given in the uri.
     *
     * @param uri
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(URI uri) throws ProtocolNotSuppException {
        super(uri);
        instanceCreated(null, null, null);
    }

    /**
     * Constructs a connection to the host (proxyPort) as given in the uri.
     *
     * @param uri
     * @param userName
     * @param password
     * @param sharedContext
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(URI uri, String userName, char[] password,
                            Object sharedContext) throws ProtocolNotSuppException {
        super(uri);
        instanceCreated(userName, password, sharedContext);
    }

    /**
     * Constructs a connection to the host (proxyPort) as given in the url.
     *
     * @param url
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(URL url) throws ProtocolNotSuppException {
        super(url);
        instanceCreated(null, null, null);
    }

    /**
     * Constructs a connection to the host (proxyPort) as given in the url.
     *
     * @param url
     * @param userName
     * @param password
     * @param sharedContext
     * @throws ProtocolNotSuppException
     */
    public JIRAHttpConnection(URL url, String userName, char[] password,
                            Object sharedContext) throws ProtocolNotSuppException {
        super(url);
        instanceCreated(userName, password, sharedContext);
    }

    /**
     * Initializes the instance.
     *
     * @param userName
     * @param password
     * @param sharedContext
     */
    private void instanceCreated(String userName, char[] password,
                                 Object sharedContext) {
        this.userName = userName;
        this.password = password;

        //Add SSO Auth Module
        addModule(SSOAuthClientModule.class, -1);

        //Set the Context
        if (sharedContext != null && !"".equals(sharedContext)) {
            setContext(sharedContext);
        }

        //Set the default headers
        HTTPHelper.setDefaultHeaders(this);

        //Other settings
        setAllowUserInteraction(false);
    }

    /**
     * Sets the user name.
     *
     * @param userName
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns the user name.
     *
     * @return
     */
    protected String getUserName() {
        return userName;
    }

    /**
     * Sets the password.
     *
     * @param password
     */
    public void setPassword(char[] password) {
        this.password = password;
    }

    /**
     * Returns the password.
     *
     * @return
     */
    protected char[] getPassword() {
        return password;
    }

    /**
     * Fetches data using the HTTP GET protocol.
     *
     * @param url
     * @return
     * @throws LoginFailureException
     * @throws JIRAHttpException
     * @throws ChangePasswordException
     */
    public String doGet(String url) throws LoginFailureException,
                                           JIRAHttpException,
                                           ChangePasswordException {
        String data = null;
        data = doGet(url, null, null);
        return data;
    }

    /**
     * Fetches data using the HTTP GET protocol.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     */
    public String doGet(String url, Map paramsMap,
                        Map headersMap) throws JIRAHttpException,
                                               LoginFailureException,
                                               ChangePasswordException {
        return doGet(url, paramsMap, headersMap, null);
    }

    /**
     * Fetches data using the HTTP GET protocol.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @param responseMetaData
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     */
    public String doGet(String url, Map paramsMap, Map headersMap,
                        Map responseMetaData) throws JIRAHttpException,
                                                     LoginFailureException,
                                                     ChangePasswordException {
        String data =
            (String)sendRequest(url, paramsMap, headersMap, responseMetaData, JIRAHttpConstants.METHOD_GET,
                                RETURN_DATA_TYPE_STRING);
        return data;
    }

    /**
     * Fetches data using the HTTP POST protocol.
     *
     * @param url
     * @return
     * @throws JIRAHttpException
     */
    public String doPost(String url) throws JIRAHttpException {
        String data = doPost(url, null, null);
        return data;
    }

    /**
     * Fetches data using the HTTP POST protocol.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @return
     * @throws JIRAHttpException
     */
    public String doPost(String url, Map paramsMap,
                         Map headersMap) throws JIRAHttpException {
        return doPost(url, paramsMap, headersMap, null);
    }


    /**
     * Fetches data using the HTTP POST protocol.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @param responseMetaData
     * @return
     * @throws JIRAHttpException
     */
    public String doPost(String url, Map paramsMap, Map headersMap,
                         Map responseMetaData) throws JIRAHttpException {
        String data =
            (String)sendRequest(url, paramsMap, headersMap, responseMetaData, JIRAHttpConstants.METHOD_POST,
                                RETURN_DATA_TYPE_STRING);
        return data;
    }

    /**
     * Provides support for retrieving data using the HTTP GET protocol.
     *
     * @param url
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     */
    public InputStream doGetAsInputStream(String url) throws JIRAHttpException,
                                                             LoginFailureException,
                                                             ChangePasswordException {
        InputStream inputStream =
            doGetAsInputStream(url, null, null);
        return inputStream;
    }

    /**
     * Provides support for retrieving data using the HTTP GET protocol.
     * @param url
     * @param paramsMap
     * @param headersMap
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     */
    public InputStream doGetAsInputStream(String url, Map paramsMap,
                                          Map headersMap) throws JIRAHttpException,
                                                                 LoginFailureException,
                                                                 ChangePasswordException {
        InputStream inputStream =
            doGetAsInputStream(url, paramsMap, headersMap, null);
        return inputStream;
    }

    /**
     * Provides support for retrieving data using the HTTP GET protocol.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @param responseMetaData
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     */
    public InputStream doGetAsInputStream(String url, Map paramsMap,
                                          Map headersMap,
                                          Map responseMetaData) throws JIRAHttpException,
                                                                       LoginFailureException,
                                                                       ChangePasswordException {
        InputStream data =
            (InputStream)sendRequest(url, paramsMap, headersMap, responseMetaData, JIRAHttpConstants.METHOD_GET,
                                     RETURN_DATA_TYPE_INPUT_STREAM);
        return data;
    }

    /**
     * Provides support for retrieving data using the HTTP POST protocol.
     *
     * @param url
     * @return
     * @throws JIRAHttpException
     */
    public InputStream doPostAsInputStream(String url) throws JIRAHttpException {
        return doPostAsInputStream(url, null, null);
    }

    /**
     * Provides support for retrieving data using the HTTP POST protocol.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @return
     * @throws JIRAHttpException
     */
    public InputStream doPostAsInputStream(String url, Map paramsMap,
                                           Map headersMap) throws JIRAHttpException {
        return doPostAsInputStream(url, paramsMap, headersMap, null);
    }

    /**
     * Provides support for retrieving data using the HTTP POST protocol.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @param responseMetaData
     * @return
     * @throws JIRAHttpException
     */
    public InputStream doPostAsInputStream(String url, Map paramsMap,
                                           Map headersMap,
                                           Map responseMetaData) throws JIRAHttpException {
        InputStream data =
            (InputStream)sendRequest(url, paramsMap, headersMap, responseMetaData, JIRAHttpConstants.METHOD_POST,
                                     RETURN_DATA_TYPE_INPUT_STREAM);
        return data;
    }

    /**
     * Transfers data from the given input stream to the given output stream.
     *
     * @param inStream
     * @param outStream
     * @throws IOException
     */
    protected void transferToOutputStream(InputStream inStream,
                                          OutputStream outStream) throws IOException {
        int bytesRead = 0;
        byte[] bytes = new byte[2048];
        do {
            bytesRead = inStream.read(bytes);
            if (bytesRead > 0) {
                outStream.write(bytes, 0, bytesRead);
            }
        } while (bytesRead > 0);
        outStream.close();
    }

    /**
     * Returns the data in the HTTP response as a String.
     *
     * @param response
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     * @throws IOException
     * @throws ModuleException
     */
    protected String getResponseAsString(HTTPResponse response) throws JIRAHttpException,
                                                                       LoginFailureException,
                                                                       ChangePasswordException,
                                                                       IOException,
                                                                       ModuleException {

        validateResponse(response);
        try {
            return response.getText();
        } catch (ParseException e) {
            throw new IOException("Unable to get text response: " +
                                  e.getMessage(), e);
        }
    }

    /**
     * Returns a InputStream that can be used to read the data in the HTTP response.
     *
     * @param response
     * @return
     * @throws LoginFailureException
     * @throws ChangePasswordException
     * @throws JIRAHttpException
     * @throws IOException
     * @throws ModuleException
     */
    protected InputStream getResponseAsInputStream(HTTPResponse response) throws LoginFailureException,
                                                                                 ChangePasswordException,
                                                                                 JIRAHttpException,
                                                                                 IOException,
                                                                                 ModuleException {
        validateResponse(response);
        InputStream data = response.getInputStream();
        return data;
    }

    /**
     * Validate the HTTP response.
     *
     * @param response
     * @throws LoginFailureException
     * @throws ChangePasswordException
     * @throws IOException
     * @throws ModuleException
     */
    protected void validateResponse(HTTPResponse response) throws LoginFailureException,
                                                                  ChangePasswordException,
                                                                  IOException,
                                                                  ModuleException {
        if (response.getStatusCode() != 200) {
            if (response.getStatusCode() == JIRAHttpConstants.STATUS_409) {
                //Check for Login Failure and Change Password
                String xmlError = new String(response.getData());
                try {
                    SSOAuthenticationFailure ssoAuthFailure = SSOAuthenticationFailure.getInstance(xmlError);
                    //TODO: Need to check if this is a ChangePasswordException
                    throw new LoginFailureException(ssoAuthFailure.getErrorCode(),
                                                    ssoAuthFailure.getErrorDesc());
                } catch (SAXException e) {
                    //Unable to parse the XML, ignore so that IOException is thrown
                    e.printStackTrace();
                }
            }

            throw new IOException("IO Error: Status code: " +
                                  response.getStatusCode() + ", Reason: " +
                                  response.getReasonLine() + ", HTML:" +
                                  new String(response.getData()));
        }
    }

    /**
     * Upload data from the given inputstream using POST.
     *
     * @param url
     * @param inStream
     * @param uploadSize -1 if size is unknown
     * @return
     * @throws JIRAHttpException
     */
    public String doUpload(String url, InputStream inStream,
                           int uploadSize) throws JIRAHttpException {
        return doUpload(url, inStream, uploadSize, null);
    }

    /**
     * Upload data from the given inputstream using POST.
     *
     * @param url
     * @param inStream
     * @param uploadSize -1 if size is unknown
     * @param headersMap
     * @return
     * @throws JIRAHttpException
     */
    public String doUpload(String url, InputStream inStream, int uploadSize,
                           Map headersMap) throws JIRAHttpException {
        return doUpload(url, inStream, uploadSize, headersMap, null);
    }

    /**
     * Upload data from the given inputstream using POST.
     *
     * @param url
     * @param inStream
     * @param uploadSize -1 if size is unknown
     * @param headersMap
     * @param responseMetaData
     * @return
     * @throws JIRAHttpException
     */
    public String doUpload(String url, InputStream inStream, int uploadSize,
                           Map headersMap,
                           Map responseMetaData) throws JIRAHttpException {
        String data =
            (String)sendRequestUsingOutputStream(url, inStream, uploadSize,
                                                 headersMap, responseMetaData,
                                                 RETURN_DATA_TYPE_STRING);
        return data;
    }

    /**
     * Upload data from the given inputstream using POST.
     *
     * @param url
     * @param inStream
     * @param uploadSize
     * @return
     * @throws JIRAHttpException
     */
    public InputStream doUploadAsInputStream(String url, InputStream inStream,
                                             int uploadSize) throws JIRAHttpException {
        return doUploadAsInputStream(url, inStream, uploadSize, null);
    }

    /**
     * Upload data from the given inputstream using POST.
     *
     * @param url
     * @param inStream
     * @param uploadSize
     * @param headersMap
     * @return
     * @throws JIRAHttpException
     */
    public InputStream doUploadAsInputStream(String url, InputStream inStream,
                                             int uploadSize,
                                             Map headersMap) throws JIRAHttpException {
        return doUploadAsInputStream(url, inStream, uploadSize, headersMap,
                                     null);
    }

    /**
     * Upload data from the given inputstream using POST.
     *
     * @param url
     * @param inStream
     * @param uploadSize
     * @param headersMap
     * @param responseMetaData
     * @return
     * @throws JIRAHttpException
     */
    public InputStream doUploadAsInputStream(String url, InputStream inStream,
                                             int uploadSize, Map headersMap,
                                             Map responseMetaData) throws JIRAHttpException {
        InputStream data =
            (InputStream)sendRequestUsingOutputStream(url, inStream,
                                                      uploadSize, headersMap,
                                                      responseMetaData,
                                                      RETURN_DATA_TYPE_INPUT_STREAM);
        return data;
    }


    /**
     * Transfer the response meta data to the given Map.
     *
     * @param responseMetaData
     * @param response
     * @throws IOException
     * @throws ModuleException
     */
    protected void populateResponseMetaDataMap(Map responseMetaData,
                                               HTTPResponse response) throws IOException,
                                                                             ModuleException {
        String contentType =
            response.getHeader(JIRAHttpConstants.HEADER_CONTENT_TYPE);
        String contentDisposition =
            response.getHeader(JIRAHttpConstants.HEADER_CONTENT_DISPOSITION);

        responseMetaData.put(JIRAHttpConstants.HEADER_CONTENT_TYPE, contentType);

        if (contentDisposition != null) {
            responseMetaData.put(JIRAHttpConstants.HEADER_CONTENT_DISPOSITION,
                                 contentDisposition);
        }

    }

    /**
     * Retrieves data using a GET or POST.
     *
     * @param url
     * @param paramsMap
     * @param headersMap
     * @param responseMetaData
     * @param method
     * @param returnDataType
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     */
    protected Object sendRequest(String url, Map paramsMap, Map headersMap,
                                 Map responseMetaData, String method,
                                 String returnDataType) throws JIRAHttpException,
                                                               LoginFailureException,
                                                               ChangePasswordException {
        NVPair[] formData = HTTPHelper.getNVPairs(paramsMap);
        NVPair[] headers = HTTPHelper.getNVPairs(headersMap);
        HTTPResponse response = null;
        try {
            try {
                if (JIRAHttpConstants.METHOD_GET.equalsIgnoreCase(method)) {
                    s_log.trace("GET  " + url + "?" + formData.toString());
                    response = super.Get(url, formData, headers);
                } else if (JIRAHttpConstants.METHOD_POST.equalsIgnoreCase(method)) {
                    s_log.trace("POST " + url + "?" + formData.toString());
                    response = super.Post(url, formData, headers);
                } else {
                    throw new RuntimeException("Unsupported method: " +
                                               method);
                }

                Object data =
                    getResponse(response, returnDataType, responseMetaData);

                return data;
            } catch (InterruptedIOException e) {
                throw new TimeoutException(0, e.getMessage(), e);
            } catch (IOException e) {
                int statusCode = 0;
                if(response != null){
                    try {
                        statusCode = response.getStatusCode();
                    } catch (IOException f) {
                        //Just ignore
                    }
                }
                throw new JIRAHttpException(statusCode, e.getMessage(), e);
            }
        } catch (ModuleException e) {
            throw new JIRAHttpException(0, e.getMessage(), e);
        }
    }

    /**
     * Upload data from the given InputStream using POST.
     *
     * @param url
     * @param inStream
     * @param uploadSize -1 if size is unknown
     * @param headersMap
     * @param responseMetaData
     * @param returnDataType
     * @return
     * @throws JIRAHttpException
     */
    protected Object sendRequestUsingOutputStream(String url,
                                                  InputStream inStream,
                                                  int uploadSize,
                                                  Map headersMap,
                                                  Map responseMetaData,
                                                  String returnDataType) throws JIRAHttpException {
        try {
            HttpOutputStream outStream;
            if (uploadSize > 0) {
                outStream = new HttpOutputStream(uploadSize);
            } else {
                outStream = new HttpOutputStream();
            }

            NVPair[] headers = HTTPHelper.getNVPairs(headersMap);
            HTTPResponse response = Post(url, outStream, headers);
            transferToOutputStream(inStream, outStream);

            Object data =
                getResponse(response, returnDataType, responseMetaData);

            return data;
        } catch (JIRAHttpException e) {
            throw new JIRAHttpException(0, e.getMessage(), e);
        } catch (ModuleException e) {
            throw new JIRAHttpException(0, e.getMessage(), e);
        } catch (IOException e) {
            throw new JIRAHttpException(0, e.getMessage(), e);
        }
    }

    /**
     * Get the data from the response in the given type.
     *
     * @param response
     * @param returnDataType
     * @param respMetaDataMap
     * @return
     * @throws JIRAHttpException
     * @throws LoginFailureException
     * @throws ChangePasswordException
     * @throws IOException
     * @throws ModuleException
     */
    protected Object getResponse(HTTPResponse response, String returnDataType,
                                 Map respMetaDataMap) throws JIRAHttpException,
                                                             LoginFailureException,
                                                             ChangePasswordException,
                                                             IOException,
                                                             ModuleException {
        Object data;
        if (RETURN_DATA_TYPE_STRING.equalsIgnoreCase(returnDataType)) {
            data = getResponseAsString(response);
        } else if (RETURN_DATA_TYPE_INPUT_STREAM.equalsIgnoreCase(returnDataType)) {
            data = getResponseAsInputStream(response);
        } else {
            throw new RuntimeException("Unknown Return Type:" +
                                       returnDataType);
        }
        if (respMetaDataMap != null) {
            populateResponseMetaDataMap(respMetaDataMap, response);
        }
        return data;
    }
}

