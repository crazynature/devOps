/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAHttpException.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

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
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAHttpException.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

public class JIRAHttpException extends Exception {
    @SuppressWarnings("compatibility:97837941389611547")
    private static final long serialVersionUID = 3599328375935534423L;
    private int statusCode;
    private String reason;

    /**
     * Create an instance of this exeption.
     *
     * @param statusCode
     * @param reason
     */
    public JIRAHttpException(int statusCode, String reason) {
        this(statusCode, reason, null);
    }

    /**
     * Create an instance of this exeption.
     *
     * @param statusCode
     * @param reason
     * @param cause
     */
    public JIRAHttpException(int statusCode, String reason, Throwable cause) {
        super(reason, cause);
        this.statusCode = statusCode;
        this.reason = reason;
    }

    /**
     * Set the status code.
     *
     * @param statusCode
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Returns the status code.
     *
     * @return
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Set the reason for the error.
     *
     * @param reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Returns the reason for the error.
     *
     * @return
     */
    public String getReason() {
        return reason;
    }
}
