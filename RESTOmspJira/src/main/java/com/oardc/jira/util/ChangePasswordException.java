/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/ChangePasswordException.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

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
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/ChangePasswordException.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

import java.util.Map;

/**
 * This exception is thrown when the SSO password has expired. The  user is
 * required to change the password.
 *
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/ChangePasswordException.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  sujoseph
 *  @since   6.1
 */

public class ChangePasswordException extends LoginFailureException {
    @SuppressWarnings("compatibility:-2226468302185485699")
    private static final long serialVersionUID = 1614192648278476831L;
    private String errorCode;
    private String errorDesc;
    private Map contextParamsMap;

    /**
     * Create an instance of this exception.
     *
     * @param errorCode
     * @param errorDesc
     */
    public ChangePasswordException(String errorCode, String errorDesc) {
        this(errorCode, errorDesc, null);
    }

    /**
     * Create an instance of this exception.
     *
     * @param errorCode
     * @param errorDesc
     * @param contextParamsMap
     */
    public ChangePasswordException(String errorCode, String errorDesc,
                                   Map contextParamsMap) {
        super(errorCode, errorDesc);
        this.contextParamsMap = contextParamsMap;
    }

    /**
     * Sets the error code.
     *
     * @param errorCode
     */
    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code.
     *
     * @return
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the error description.
     *
     * @return
     */
    public String getErrorDesc() {
        return errorDesc;
    }

    public Map getContextParamsMap() {
        return contextParamsMap;
    }
}
