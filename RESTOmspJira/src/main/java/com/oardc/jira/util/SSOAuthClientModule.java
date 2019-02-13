/* $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/SSOAuthClientModule.java /main/2 2013/09/10 16:42:36 dalai Exp $ */

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
 *  @version $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/SSOAuthClientModule.java /main/2 2013/09/10 16:42:36 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

import HTTPClient.HTTPClientModule;
import HTTPClient.HTTPClientModuleConstants;
import HTTPClient.HTTPConnection;
import HTTPClient.NVPair;
import HTTPClient.Request;
import HTTPClient.Response;
import HTTPClient.RoRequest;

import java.io.IOException;
import java.io.InputStream;

import java.net.URISyntaxException;

import java.text.MessageFormat;

import java.util.List;
import java.util.Map;


/**
 *  Monitors HTTP requests and responses to detect SSO authentication challenges
 *
 *  @version $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/SSOAuthClientModule.java /main/2 2013/09/10 16:42:36 dalai Exp $
 *  @author  sujoseph
 *  @since   6.1
 */

public class SSOAuthClientModule implements HTTPClientModule {
    private static final String LOGIN_FORM_ACTION = "/sso/auth";
    private static final String CHANGE_PASSWORD_FORM_ACTION =
        "/sso/ChangePwdServlet";

    private static final String LOGIN_FORM_PARAM_NAME_USER_NAME =
        "ssousername";
    private static final String LOGIN_FORM_PARAM_NAME_PASSWORD = "password";

    //HTTP parameters that must be there in the request for a request to be considered
    //as the request for a login page
    private static final String[] LOGIN_PAGE_PARAMETERS_OSSO =
    { "site2pstoretoken", "ssousername", "p_error_code", "p_cancel_url" }; // for osso
    
    private static final String[] LOGIN_PAGE_PARAMETERS_WEBGATE = 
    { "authn_try_count", "request_id", "locale" }; // for webgate

    //HTTP parameters that must be there in the request for a request to be considered
    //as the request for a change password page
    private static final String[] CHANGE_PWD_PAGE_PARAMETERS =
    { "p_username", "p_subscribername", "p_error_code", "p_done_url",
      "site2pstoretoken", "p_pwd_is_exp" };
    private static final String CHANGE_PWD_FORM_PARAM_NAME_ACTION = "p_action";


    private static final String PARAM_NAME_P_ERROR_CODE = "p_error_code";
    private static final String PARAM_NAME_P_PWD_IS_EXP = "p_pwd_is_exp";

    //private static final String P_PWD_IS_EXP_WARN = "WARN";
    private static final String P_PWD_IS_EXP_FORCE = "FORCE";

    //private static final String P_ACTION_OK = "OK";
    private static final String P_ACTION_CANCEL = "CANCEL";


    //Error Message XML Format
    private static final String LOGIN_FAILURE_ERROR_MESSAGE_FORMAT =
        "<ssoAuthenticationFailure>\n" +
        "    <errorCode>{0}</errorCode>\n" +
        "    <errorDesc>{1}</errorDesc>\n" +
        "</ssoAuthenticationFailure>";

    private String userName;
    private char[] password;
    private int loginAttempt = 0;

    //Used to transfer control from responsePhase2Handler() to requestHandler().
    //requestHandler() is expected to send back the generatedResponse.
    private boolean returnGeneratedResponse = false;
    private String generatedResponse = null;

    /**
     * This is invoked before the request is sent. A module will typically use
     * this to make a note of headers, to modify headers and/or data, or even
     * generate and return a response (e.g. for a cache module).
     * If a response is generated the module must return the appropriate return
     * code (REQ_RESPONSE or REQ_RETURN).
     *
     * @param request
     * @param responses
     * @return
     */
    public int requestHandler(Request request, Response[] responses) {
        if (returnGeneratedResponse) {
            NVPair[] headers = null;
            InputStream inputStream = null;
            byte[] data = generatedResponse.getBytes();
            responses[0] =
                    new Response(JIRAHttpConstants.HTTP_VERSION_1_1, JIRAHttpConstants.STATUS_409, JIRAHttpConstants.STATUS_MSG_409, headers, data,
                                 inputStream, data.length);
            return REQ_RETURN;
        }

        HTTPConnection conn = request.getConnection();
        if (userName == null && conn instanceof JIRAHttpConnection) {
            //Copy the username and password for handling SSO authentication challenge.
            //When a redirection occurs the connection object will be different.
            JIRAHttpConnection ssoConn = (JIRAHttpConnection)conn;
            userName = ssoConn.getUserName();
            password = ssoConn.getPassword();
        }
        return HTTPClientModuleConstants.REQ_CONTINUE;
    }

    /**
     * The phase 1 response handler. This will be invoked for every response.
     * Modules will typically make notes of the response and do any header
     * processing which must always be performed.
     *
     * @param response
     * @param roRequest
     */
    public void responsePhase1Handler(Response response, RoRequest roRequest) {
    }

    /**
     * The phase 2 response handler. A module may modify the response or
     * generate a new request (e.g. for redirection). This handler will only be
     * invoked for a given module if all previous modules returned RSP_CONTINUE.
     * If the request is modified the handler must return an appropriate return
     * code (RSP_REQUEST, RSP_SEND, RSP_NEWCON_REQ or RSP_NEWCON_SND).
     * If any other code is return the request must not be modified.
     *
     * @param response
     * @param request
     * @return
     * @throws IOException
     */
    public int responsePhase2Handler(Response response,
                                     Request request) throws IOException {
        String requestURI = request.getRequestURI();

        //Create the query parameters map
        Map urlQueryParamsMap;
        try {
            urlQueryParamsMap = HTTPHelper.extractUriQueryParamsMap(requestURI);
        } catch (URISyntaxException e) {
            //We do not expect this exception
            throw new IOException(e.getMessage());
        }

        //Does Request URI contain all Login Page parameters  or change password parameters
        boolean isLoginPageRequest = isLoginPageRequest(urlQueryParamsMap);
        boolean isChangePasswordPageRequest = false;

        //If it's not the login page request and the login has already been attempted
        //check if the user has been redirected to the change password page
        if (!isLoginPageRequest && loginAttempt > 0) {
            isChangePasswordPageRequest =
                    isChangePasswordPageRequest(urlQueryParamsMap);
        }

        if (!isLoginPageRequest && !isChangePasswordPageRequest) {
            return HTTPClientModuleConstants.RSP_CONTINUE;
        }

        if (isLoginPageRequest) {
            return handleLoginPage(response, request, urlQueryParamsMap);
        } else if (isChangePasswordPageRequest) {
            return handleChangePasswordPage(response, request,
                                            urlQueryParamsMap);
        }

        return HTTPClientModuleConstants.RSP_CONTINUE;
    }

    /**
     * Handles the login page.
     *
     * @param response
     * @param request
     * @param urlQueryParamsMap
     * @return
     * @throws IOException
     */
    protected int handleLoginPage(Response response, Request request,
                                  Map urlQueryParamsMap) throws IOException {
        boolean isSSOAuthChallenge = false;
        //Does the response have a Form that submits the SSO user name and password?
        //Else it's not a SSO Auth challenge
        String[] formData = null;
        String responseHTML = new String(response.getData());
        List formList = HTMLHelper.extractFormElements(responseHTML);
        if (formList != null && formList.size() > 0) {
            for (int i = 0; i < formList.size(); i++) {
                formData = (String[])formList.get(i);
                String action = formData[HTMLHelper.ATTR_POS_FORM_ACTION];
                if (action.indexOf(LOGIN_FORM_ACTION) != -1) {
                    isSSOAuthChallenge = true;
                    break;
                }
            }
        }

        //If SSO Auth Challenge, generate a request that does the authentication
        if (isSSOAuthChallenge) {
            if (loginAttempt > 0) {
                //We have already attempted a login. This must an authentication failure
                //Send an error message.
                generatedResponse = generateLoginFailureXML(urlQueryParamsMap);
                returnGeneratedResponse = true;
                return HTTPClientModuleConstants.RSP_NEWCON_REQ;
            }
            generateSSOLoginRequest(request, formData);
            loginAttempt++;
            return HTTPClientModuleConstants.RSP_REQUEST;
        }

        return HTTPClientModuleConstants.RSP_CONTINUE;
    }

    /**
     * Handles the change password page.
     *
     * @param response
     * @param request
     * @param urlQueryParamsMap
     * @return
     * @throws IOException
     */
    protected int handleChangePasswordPage(Response response, Request request,
                                           Map urlQueryParamsMap) throws IOException {
        boolean isChangePasswordForm = false;
        //Does the response have a Form that submits the new password?
        String[] formData = null;
        String responseHTML = new String(response.getData());
        List formList = HTMLHelper.extractFormElements(responseHTML);
        if (formList != null && formList.size() > 0) {
            for (int i = 0; i < formList.size(); i++) {
                formData = (String[])formList.get(i);
                String action = formData[HTMLHelper.ATTR_POS_FORM_ACTION];
                if (action.indexOf(CHANGE_PASSWORD_FORM_ACTION) != -1) {
                    isChangePasswordForm = true;
                    break;
                }
            }
        }
        //Is it the change password form?
        if (isChangePasswordForm) {
            /*
             * If the password is already expired send back an error message
             * If the password is about to expire just cancel the password change page
             */
            boolean passwordExpired = false;
            String pwdIsExp =
                (String)urlQueryParamsMap.get(PARAM_NAME_P_PWD_IS_EXP);
            if (pwdIsExp != null && pwdIsExp.equals(P_PWD_IS_EXP_FORCE)) {
                passwordExpired = true;
            }
            if (passwordExpired) {
                //generate password has expired response
                generatedResponse = generatePasswordExpiredXML();
                returnGeneratedResponse = true;
                return HTTPClientModuleConstants.RSP_NEWCON_REQ;
            }
            generateCancelChangePasswordRequest(request, formData);
            return HTTPClientModuleConstants.RSP_REQUEST;
        }
        return HTTPClientModuleConstants.RSP_CONTINUE;
    }


    /**
     * Check if the HTTP request has all the Login page parameters.
     *
     * The login page parameters are documented here:
     * http://iasdocs.us.oracle.com/iasdl/1014im_final/idmanage.1014/b15988/custom.htm#i1011677
     *
     * @param queryParamsMap
     * @return
     */
    protected boolean isLoginPageRequest(Map queryParamsMap) {
        //Does Request URI contain all Login Page parameters
        boolean requestContainsLoginPageParams = true;
        
        // first check if login page was returned by mod_osso
        for (int i = 0; i < LOGIN_PAGE_PARAMETERS_OSSO.length; i++) {
            String paramName = LOGIN_PAGE_PARAMETERS_OSSO[i];
            if (!queryParamsMap.containsKey(paramName)) {
                requestContainsLoginPageParams = false;
                break;
            }
        }
        
        // if needed, check if login page was returned by webgate
        if (!requestContainsLoginPageParams) {
            requestContainsLoginPageParams = true;
            for (int i = 0; i < LOGIN_PAGE_PARAMETERS_WEBGATE.length; i++) {
                String paramName = LOGIN_PAGE_PARAMETERS_WEBGATE[i];
                if (!queryParamsMap.containsKey(paramName)) {
                    requestContainsLoginPageParams = false;
                    break;
                }
            }
        }
        
        return requestContainsLoginPageParams;
    }

    /**
     * Check if the HTTP request has all the Change password page parameters.
     *
     * These parameters are documented here:
     * http://iasdocs.us.oracle.com/iasdl/1014im_final/idmanage.1014/b15988/custom.htm#i1011677
     *
     * @param queryParamsMap
     * @return
     */
    protected boolean isChangePasswordPageRequest(Map queryParamsMap) {
        //Does Request URI contain all Change Password Page parameters
        boolean requestContainsChangePwdPageParams = true;
        for (int i = 0; i < CHANGE_PWD_PAGE_PARAMETERS.length; i++) {
            String paramName = CHANGE_PWD_PAGE_PARAMETERS[i];
            if (!queryParamsMap.containsKey(paramName)) {
                requestContainsChangePwdPageParams = false;
                break;
            }
        }
        return requestContainsChangePwdPageParams;
    }

    /**
     * The phase 3 response handler. This will only be invoked if no new
     * subrequest was generated in phase 2. Modules should defer any repsonse
     * handling which need only be done if the response is returned to the
     * user to this phase.
     *
     * @param response
     * @param roRequest
     */
    public void responsePhase3Handler(Response response, RoRequest roRequest) {
        //Clear the password, we do not need it any more
        userName = null;
        password = null;
    }

    /**
     * The chunked transfer-encoding (and in future maybe others) can contain
     * trailer fields at the end of the body. Since the responsePhaseXHandler()'s
     * are invoked before the body is read and therefore do not have access to
     * the trailers (unless they force the complete body to be read) this method
     * will be invoked when the trailers have been read and
     * parsed (sort of a post-response handling).
     *
     * @param response
     * @param roRequest
     */
    public void trailerHandler(Response response, RoRequest roRequest) {
    }

    /**
     * Generate a HTTP request for submitting the use name and password.
     *
     * @param request
     * @param formData
     */
    public void generateSSOLoginRequest(Request request, String[] formData) {
        List inputList = HTMLHelper.extractInputElements(formData[0]);

        //Prepare the request
        StringBuffer dataBuffer = new StringBuffer();
        for (int i = 0; i < inputList.size(); i++) {
            String[] inputData = (String[])inputList.get(i);
            String name = inputData[HTMLHelper.ATTR_POS_INPUT_NAME];
            //Value is a char[] to prevent converting password to String
            String value = HTTPHelper.encode(inputData[HTMLHelper.ATTR_POS_INPUT_VALUE]);
            if (LOGIN_FORM_PARAM_NAME_USER_NAME.equalsIgnoreCase(name)) {
                value = HTTPHelper.encode(userName);
            } else if (LOGIN_FORM_PARAM_NAME_PASSWORD.equalsIgnoreCase(name)) {
                String valueBeforeEnc = new String(password);
                value = HTTPHelper.encode(valueBeforeEnc);
            }
            if (dataBuffer.length() > 0) {
                dataBuffer.append("&");
            }
            dataBuffer.append(name).append("=").append(value);
        }

        //Configure the request
        request.setAllowUI(false);
        request.setMethod(formData[HTMLHelper.ATTR_POS_FORM_METHOD].toUpperCase());
        request.setRequestURI(LOGIN_FORM_ACTION);

        //Set the content type
        NVPair contentType = HTTPHelper.getFormURLEncodedContentType();

        //Set the Headers
        NVPair[] headers = new NVPair[1];
        headers[0] = contentType;
        HTTPHelper.setHTTPHeaders(request, headers);

        //Set the content
        request.setData(dataBuffer.toString().getBytes());
        //Clear the data buffer, because it has the password in it
        for (int i = 0; i < dataBuffer.length(); i++) {
            dataBuffer.setCharAt(i, ' ');
        }

    }

    /**
     * Generate a HTTP request for canceling the password change.
     *
     * @param request
     * @param formData
     */
    public void generateCancelChangePasswordRequest(Request request,
                                                    String[] formData) {
        List inputList = HTMLHelper.extractInputElements(formData[0]);

        //Prepare the request
        StringBuffer dataBuffer = new StringBuffer();
        for (int i = 0; i < inputList.size(); i++) {
            String[] inputData = (String[])inputList.get(i);
            String name = inputData[HTMLHelper.ATTR_POS_INPUT_NAME];
            String value = HTTPHelper.encode(inputData[HTMLHelper.ATTR_POS_INPUT_VALUE]);
            if (CHANGE_PWD_FORM_PARAM_NAME_ACTION.equalsIgnoreCase(name)) {
                value = P_ACTION_CANCEL;
            }
            if (dataBuffer.length() > 0) {
                dataBuffer.append("&");
            }
            dataBuffer.append(name).append("=").append(value);
        }

        //Configure the request
        request.setAllowUI(false);
        request.setMethod(formData[HTMLHelper.ATTR_POS_FORM_METHOD]);
        request.setRequestURI(CHANGE_PASSWORD_FORM_ACTION);

        //Set the content type
        NVPair contentType = HTTPHelper.getFormURLEncodedContentType();

        //Set the Headers
        NVPair[] headers = new NVPair[1];
        headers[0] = contentType;
        HTTPHelper.setHTTPHeaders(request, headers);

        //Set the content
        request.setData(dataBuffer.toString().getBytes());
    }

    /**
     * Generate a XML message notifying that the login has
     * failed.
     *
     * @param queryParamsMap
     * @return
     */
    private static String generateLoginFailureXML(Map queryParamsMap) {
        //Get the errorCode from the query parameters
        String errorCode = (String)queryParamsMap.get(PARAM_NAME_P_ERROR_CODE);
        String errorMsg = null;
        if (errorCode != null) {
            errorMsg =
                    (String)JIRAHttpConstants.LOGIN_PAGE_ERROR_CODES.get(errorCode);
        }
        if (errorMsg == null) {
            errorMsg = JIRAHttpConstants.DEFAULT_ERROR_MSG;
        }
        //Format the error message and return it
        String msg =
            MessageFormat.format(LOGIN_FAILURE_ERROR_MESSAGE_FORMAT, new Object[] { errorCode,
                                                                                    errorMsg });
        return msg;
    }

    /**
     * Generate a XML message notifying that the password has
     * expired.
     *
     * @return
     */
    private static String generatePasswordExpiredXML() {
        //Get the errorCode from the query parameters
        String errorCode = JIRAHttpConstants.PASSWORD_EXPIRED_CODE;
        String errorMsg = JIRAHttpConstants.PASSWORD_EXPIRED_DESC;
        //Format the error message and return it
        String msg =
            MessageFormat.format(LOGIN_FAILURE_ERROR_MESSAGE_FORMAT, new Object[] { errorCode,
                                                                                    errorMsg });
        return msg;
    }


}
