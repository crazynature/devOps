/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAHttpConstants.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

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
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAHttpConstants.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

import java.util.Map;
import java.util.HashMap;

public final class JIRAHttpConstants {

    public static final String HTTP_VERSION_1_1 = "HTTP/1.1";
    public static final int STATUS_409 = 409;
    public static final String STATUS_MSG_409 = "Conflict";

    public static final String PROTOCOL_HTTP = "http";
    public static final String PROTOCOL_HTTPS = "https";

    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    public static final String HEADER_CONTENT_TYPE = "Content-Type";

    public static final String HEADER_CONTENT_DISPOSITION =
        "Content-Disposition";

    //Login Page Error Codes
    public static final String LOGIN_PAGE_ERROR_CODE_ACCT_LOCK_ERR =
        "acct_lock_err";
    public static final String LOGIN_PAGE_ERROR_CODE_PWD_EXP_ERR =
        "pwd_exp_err";
    public static final String LOGIN_PAGE_ERROR_CODE_NULL_UNAME_PWD_ERR =
        "null_uname_pwd_err";
    public static final String LOGIN_PAGE_ERROR_CODE_AUTH_FAIL_EXCEPTION =
        "auth_fail_exception";
    public static final String LOGIN_PAGE_ERROR_CODE_NULL_PASSWORD_ERR =
        "null_password_err";
    public static final String LOGIN_PAGE_ERROR_CODE_SSO_FORCED_AUTH =
        "sso_forced_auth";
    public static final String LOGIN_PAGE_ERROR_CODE_UNEXPECTED_EXCEPTION =
        "unexpected_exception";
    public static final String LOGIN_PAGE_ERROR_CODE_UNEXP_ERR = "unexp_err";
    public static final String LOGIN_PAGE_ERROR_CODE_INTERNAL_SERVER_ERR =
        "internal_server_err";
    public static final String LOGIN_PAGE_ERROR_CODE_INTERNAL_SERVER_TRY_AGAIN_ERR =
        "internal_server_try_again_err";
    public static final String LOGIN_PAGE_ERROR_CODE_INTERNAL_SERVER_TRY_LATER_ERR =
        "internal_server_try_later_err";
    public static final String LOGIN_PAGE_ERROR_CODE_GITO_ERR = "gito_err";
    public static final String LOGIN_PAGE_ERROR_CODE_CERT_AUTH_ERR =
        "cert_auth_err";
    public static final String LOGIN_PAGE_ERROR_CODE_SESSION_EXP_ERROR =
        "session_exp_error";
    public static final String LOGIN_PAGE_ERROR_CODE_USERID_MISMATCH =
        "userid_mismatch";

    //Login Page Error Descriptions
    public static final String LOGIN_PAGE_ERROR_DESC_ACCT_LOCK_ERR =
        "Your account is locked. Please notify the system administrator.";
    public static final String LOGIN_PAGE_ERROR_DESC_PWD_EXP_ERR =
        "Your password has expired. Please contact the administrator to reset it.";
    public static final String LOGIN_PAGE_ERROR_DESC_NULL_UNAME_PWD_ERR =
        "You must enter a valid user name.";
    public static final String LOGIN_PAGE_ERROR_DESC_AUTH_FAIL_EXCEPTION =
        "Authentication failed. Please try again.";
    public static final String LOGIN_PAGE_ERROR_DESC_NULL_PASSWORD_ERR =
        "You must enter your logon password.";
    public static final String LOGIN_PAGE_ERROR_DESC_SSO_FORCED_AUTH =
        "The application you are trying to access requires you to sign in again even if you have signed in previously.";
    public static final String LOGIN_PAGE_ERROR_DESC_UNEXPECTED_EXCEPTION =
        "An unexpected error occurred. Please try again.";
    public static final String LOGIN_PAGE_ERROR_DESC_UNEXP_ERR =
        "Unexpected Error. Please contact Administrator.";
    public static final String LOGIN_PAGE_ERROR_DESC_INTERNAL_SERVER_ERR =
        "Internal Server Error. Please contact Administrator.";
    public static final String LOGIN_PAGE_ERROR_DESC_INTERNAL_SERVER_TRY_AGAIN_ERR =
        "Internal Server Error. Please retry the operation.";
    public static final String LOGIN_PAGE_ERROR_DESC_INTERNAL_SERVER_TRY_LATER_ERR =
        "Internal Server Error. Please try the operation later.";
    public static final String LOGIN_PAGE_ERROR_DESC_GITO_ERR =
        "Your Single Sign_on session has expired. For your security, your session expires after some duration of inactivity. Please sign in again.";
    public static final String LOGIN_PAGE_ERROR_DESC_CERT_AUTH_ERR =
        "Certificate-based sign in failed. Please ensure that you have a valid certificate or contact the administrator.";
    public static final String LOGIN_PAGE_ERROR_DESC_SESSION_EXP_ERROR =
        "Your Single Sign-On session has expired. For your security, your session expires after the specified amount of time. Please sign in again.";
    public static final String LOGIN_PAGE_ERROR_DESC_USERID_MISMATCH =
        "The user name submitted for authentication does not match the user name present in the existing Single Sign-On session.";

    //Default error code.
    public static final String DEFAULT_ERROR_CODE = "unknown_error";
    public static final String DEFAULT_ERROR_MSG = "Unknown Error";

    //Login page error codes
    public static final Map LOGIN_PAGE_ERROR_CODES = new HashMap();
    static {
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_ACCT_LOCK_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_ACCT_LOCK_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_PWD_EXP_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_PWD_EXP_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_NULL_UNAME_PWD_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_NULL_UNAME_PWD_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_AUTH_FAIL_EXCEPTION, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_AUTH_FAIL_EXCEPTION);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_NULL_PASSWORD_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_NULL_PASSWORD_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_SSO_FORCED_AUTH, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_SSO_FORCED_AUTH);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_UNEXPECTED_EXCEPTION, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_UNEXPECTED_EXCEPTION);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_UNEXP_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_UNEXP_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_INTERNAL_SERVER_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_INTERNAL_SERVER_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_INTERNAL_SERVER_TRY_AGAIN_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_INTERNAL_SERVER_TRY_AGAIN_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_INTERNAL_SERVER_TRY_LATER_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_INTERNAL_SERVER_TRY_LATER_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_GITO_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_GITO_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_CERT_AUTH_ERR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_CERT_AUTH_ERR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_SESSION_EXP_ERROR, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_SESSION_EXP_ERROR);
        LOGIN_PAGE_ERROR_CODES.put(JIRAHttpConstants.LOGIN_PAGE_ERROR_CODE_USERID_MISMATCH, JIRAHttpConstants.LOGIN_PAGE_ERROR_DESC_USERID_MISMATCH);
    }

    public static final String PASSWORD_EXPIRED_CODE = "password_expired";
    public static final String PASSWORD_EXPIRED_DESC =
        "The password has expired.";
}
