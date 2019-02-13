/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/LoginFailureException.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

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
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/LoginFailureException.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

public class LoginFailureException extends JIRAHttpException {
    @SuppressWarnings("compatibility:-3937712723249739871")
    private static final long serialVersionUID = -4757406768862669171L;
    private String errorCode;
    private String errorDesc;

    /**
     * Create an instance of this class.
     *
     * @param errorCode
     * @param errorDesc
     *
     * @see JIRAHttpConstants Login Page Error Codes
     */
    public LoginFailureException(String errorCode, String errorDesc) {
        super(JIRAHttpConstants.STATUS_409, JIRAHttpConstants.STATUS_MSG_409);
        this.errorCode = errorCode;
        this.errorDesc = errorDesc;
    }

    /**
     * Sets the login failure error code.
     *
     * @param errorCode
     *
     * @see JIRAHttpConstants Login Page Error Codes
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Returns the login failure error description.
     *
     * @return
     *
     * @see JIRAHttpConstants Login Page Error Codes
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Sets the login page Error Description.
     *
     * @param errorDesc
     *
     * @see JIRAHttpConstants Login Page Error Codes
     */
    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    /**
     * Returns the login page error description.
     *
     * @return
     *
     * @see JIRAHttpConstants Login Page Error Codes
     */
    public String getErrorDesc() {
        return errorDesc;
    }

    private class EMHTTPException {
    }
}
