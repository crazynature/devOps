/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/TimeoutException.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

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
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/TimeoutException.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

import java.io.InterruptedIOException;

public class TimeoutException extends JIRAHttpException {
    private int statusCode;
    private String reason;

    /**
     * Create an instance of this exeption.
     *
     * @param statusCode
     * @param reason
     * @param cause
     */
    public TimeoutException(int statusCode, String reason, Throwable cause) {
        super(statusCode, reason, cause);
        this.statusCode = statusCode;
        this.reason = reason;

    }


}

