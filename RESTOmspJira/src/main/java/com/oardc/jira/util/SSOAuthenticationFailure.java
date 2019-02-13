/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/SSOAuthenticationFailure.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

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
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/SSOAuthenticationFailure.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

import java.io.IOException;

import java.io.StringReader;

import java.util.Map;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SSOAuthenticationFailure {
    private String errorCode;
    private String errorDesc;
    private Map contextParams;

    public SSOAuthenticationFailure() {
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
     * Sets the error description.
     *
     * @param errorDesc
     */
    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    /**
     * Returns the error description.
     *
     * @return
     */
    public String getErrorDesc() {
        return errorDesc;
    }

    public Map getContextParams() {
        if (contextParams == null) {
            contextParams = new HashMap();
        }
        return contextParams;
    }

    /**
     * Parses the XML returned when there is an SSO Authentication Failure
     *
     * @param xml
     * @return
     * @throws SAXException
     * @throws IOException
     */
    public static SSOAuthenticationFailure getInstance(String xml) throws SAXException,
                                                                          IOException {
        SSOAuthenticationFailure ssoAuthFailure =
            new SSOAuthenticationFailure();
        StringReader strReader = new StringReader(xml);
        InputSource xmlInput = new InputSource(strReader);
        SSOAuthenticationFailureHandler handler =
            ssoAuthFailure.new SSOAuthenticationFailureHandler();

        SAXParser saxParser = ssoAuthFailure.getSAXParser();
        saxParser.parse(xmlInput, handler);
        return ssoAuthFailure;
    }

    protected SAXParser getSAXParser() throws SAXException {
        SAXParserFactory saxFactory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        try {
            saxParser = saxFactory.newSAXParser();
        } catch (ParserConfigurationException e) {
            //We do not expect this exception
            throw new RuntimeException("Unable to locate SAXParser", e);
        }
        return saxParser;
    }

    private class SSOAuthenticationFailureHandler extends DefaultHandler {
        private static final String ELEMENT_NAME_ERROR_CODE = "errorCode";
        private static final String ELEMENT_NAME_ERROR_DESC = "errorDesc";

        private StringBuffer elementTextBuffer;

        public void startElement(String uri, String localName, String qName,
                                 Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            elementTextBuffer = new StringBuffer();
        }

        public void characters(char[] ch, int start,
                               int length) throws SAXException {
            super.characters(ch, start, length);
            elementTextBuffer.append(ch, start, length);
        }

        public void endElement(String uri, String localName,
                               String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            if (qName.equals(ELEMENT_NAME_ERROR_CODE) ||
                localName.equals(ELEMENT_NAME_ERROR_CODE)) {
                errorCode = elementTextBuffer.toString();
            } else if (qName.equals(ELEMENT_NAME_ERROR_DESC) ||
                       localName.equals(ELEMENT_NAME_ERROR_DESC)) {
                errorDesc = elementTextBuffer.toString();
            }
        }
    }
}
