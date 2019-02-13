/* $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/HTMLHelper.java /main/3 2013/07/08 04:17:11 ohkambha Exp $ */

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
 *  @version $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/HTMLHelper.java /main/3 2013/07/08 04:17:11 ohkambha Exp $
 *  @author  dalai
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;

//import com.oracle.bugjirabridge.util.LoggerUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *  Helper class for parsing HTML.
 *
 *  @version $Header: mos/source/odcs/jira/BugJiraBridgeAQ/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/HTMLHelper.java /main/3 2013/07/08 04:17:11 ohkambha Exp $
 *  @author  sujoseph
 *  @since   6.1
 */

public class HTMLHelper {
    //Setup the logger
    private static final Logger LOGGER =
        Logger.getLogger(HTMLHelper.class);
    private static final Level VERBOSE_LOG_LEVEL = Level.TRACE;
    //Elements that can be recognized
    public static final String ELEMENT_NAME_FORM = "form";
    public static final String ELEMENT_NAME_INPUT = "input";
    public static final String ELEMENT_NAME_IMAGE = "img";

    //Attribute Positions Map - Populated below. An element and some of its
    //attributes are returned in a String array. This map controls the positions
    //of the attributes in the String array.
    private static final Map ATTR_POSITIONS_MAP = new HashMap();

    //Generic Regex
    private static final String REGEX_SPACES = " *?";
    private static final String REGEX_ANY_CHARACTER = ".*?";
    private static final String HTML_ATTR_DELIMITER = "[\"']";
    private static final String REGEX_ATTR_VALUE =
        REGEX_SPACES + "=" + REGEX_SPACES + HTML_ATTR_DELIMITER + "(" +
        REGEX_ANY_CHARACTER + ")" + HTML_ATTR_DELIMITER;

    //Regex for Form Element
    private static final String TAG_START_FORM = "<form";
    private static final String TAG_END_FORM = "</form>";
    private static final String REGEX_FORM =
        TAG_START_FORM + REGEX_ANY_CHARACTER + "(method|action)" +
        REGEX_ATTR_VALUE + REGEX_ANY_CHARACTER + "(action|method)" +
        REGEX_ATTR_VALUE + REGEX_ANY_CHARACTER + TAG_END_FORM;
    private static final Pattern PATTERN_FORM =
        Pattern.compile(REGEX_FORM, Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    //Form element Attributes - must be lower case
    public static final String ATTR_NAME_FORM_METHOD = "method";
    public static final String ATTR_NAME_FORM_ACTION = "action";

    public static final int ATTR_FORM_COUNT = 2;
    public static final int ATTR_POS_FORM_METHOD = 1;
    public static final int ATTR_POS_FORM_ACTION = 2;

    //Specify the position of form element attributes
    static {
        ATTR_POSITIONS_MAP.put(ELEMENT_NAME_FORM + ATTR_NAME_FORM_METHOD,
                               new Integer(ATTR_POS_FORM_METHOD));
        ATTR_POSITIONS_MAP.put(ELEMENT_NAME_FORM + ATTR_NAME_FORM_ACTION,
                               new Integer(ATTR_POS_FORM_ACTION));
    }

    //Regex for Input Element
    private static final String TAG_START_INPUT = "<Input";
    private static final String REGEX_INPUT =
        TAG_START_INPUT + REGEX_ANY_CHARACTER + "(name|type|value)" +
        REGEX_ATTR_VALUE + REGEX_ANY_CHARACTER + "(name|type|value)" +
        REGEX_ATTR_VALUE + REGEX_ANY_CHARACTER + "(name|type|value)" +
        REGEX_ATTR_VALUE + REGEX_ANY_CHARACTER + ">";

    private static final Pattern PATTERN_INPUT =
        Pattern.compile(REGEX_INPUT, Pattern.DOTALL |
                        Pattern.CASE_INSENSITIVE);

    //Input element Attributes - must be lower case
    public static final String ATTR_NAME_INPUT_TYPE = "type";
    public static final String ATTR_NAME_INPUT_NAME = "name";
    public static final String ATTR_NAME_INPUT_VALUE = "value";

    public static final int ATTR_INPUT_COUNT = 3;
    public static final int ATTR_POS_INPUT_TYPE = 1;
    public static final int ATTR_POS_INPUT_NAME = 2;
    public static final int ATTR_POS_INPUT_VALUE = 3;

    //Specify the position of input element attributes
    static {
        ATTR_POSITIONS_MAP.put(ELEMENT_NAME_INPUT + ATTR_NAME_INPUT_TYPE,
                               new Integer(ATTR_POS_INPUT_TYPE));
        ATTR_POSITIONS_MAP.put(ELEMENT_NAME_INPUT + ATTR_NAME_INPUT_NAME,
                               new Integer(ATTR_POS_INPUT_NAME));
        ATTR_POSITIONS_MAP.put(ELEMENT_NAME_INPUT + ATTR_NAME_INPUT_VALUE,
                               new Integer(ATTR_POS_INPUT_VALUE));
    }

    //Regex for Image Element
    private static final String REGEX_IMAGE = "<img.*?>";
    private static final Pattern PATTERN_IMAGE =
        Pattern.compile(REGEX_IMAGE, Pattern.DOTALL |
                        Pattern.CASE_INSENSITIVE);

    public static final int ATTR_IMAGE_COUNT = 0;

    /**
     * This class should have only static methods.
     */
    private HTMLHelper() {
    }

    /**
     * Extracts all the form elements from the given input text.
     *
     * The returned List has String[] objects. The first element in the array is the entire
     * form element. The 2nd element is the METHOD of the form and the 3rd element
     * is the ACTION of the form.
     *
     * @param inputText
     * @return
     */
    public static List extractFormElements(String inputText) {
        Matcher matcher = PATTERN_FORM.matcher(inputText);
        List elementList =
            extractElements(inputText, ELEMENT_NAME_FORM, matcher,
                            ATTR_FORM_COUNT);
        return elementList;
    }

    /**
     * Extracts all the Input elements from the given input text.
     *
     * The returned List has String[] objects. The first element in the array is the entire
     * input element. The 2nd element is the TYPE of the input and the 3rd element
     * is the NAME of the input and the 4th element is the VALUE of the input.
     *
     * @param inputText
     * @return
     */
    public static List extractInputElements(String inputText) {
        Matcher matcher = PATTERN_INPUT.matcher(inputText);
        List elementList =
            extractElements(inputText, ELEMENT_NAME_INPUT, matcher,
                            ATTR_INPUT_COUNT);
        return elementList;
    }

    /**
     * Extracts all the Image elements from the given input text.
     *
     * The returned List has String[] objects. The only element in the array has the entire
     * image element.
     *
     * @param inputText
     * @return
     */
    public static List extractImageElements(String inputText) {
        Matcher matcher = PATTERN_IMAGE.matcher(inputText);
        List elementList =
            extractElements(inputText, ELEMENT_NAME_IMAGE, matcher,
                            ATTR_IMAGE_COUNT);
        return elementList;
    }

    /**
     * Extracts elements of the specified type using the given matcher.
     *
     * @param inputText
     * @param elementName
     * @param matcher
     * @param attributeCount
     * @return
     */
    private static List extractElements(String inputText, String elementName,
                                        Matcher matcher, int attributeCount) {
        List elementList = new ArrayList();
        if ( LOGGER.isTraceEnabled()) {
            LOGGER.log(VERBOSE_LOG_LEVEL,
                       "Searching for " + elementName + " Element in: " +
                       inputText);
            LOGGER.log(VERBOSE_LOG_LEVEL,
                       "Using Pattern: " + matcher.pattern().pattern());
        }
        while (matcher.find()) {
            String[] matchingStrings = new String[attributeCount + 1];
            //Get the entire String
            matchingStrings[0] = matcher.group();
            if ( LOGGER.isTraceEnabled()) {
                LOGGER.log(VERBOSE_LOG_LEVEL,
                           "Found Element: " + matchingStrings[0] +
                           ", Group Count: " + matcher.groupCount());
            }

            //Get the attributes
            StringBuffer logBuffer = null;
            if (LOGGER.isTraceEnabled()) {
                logBuffer = new StringBuffer("Attributes: ");
            }
            for (int i = 1; i < (attributeCount + 1); i++) {
                int attrNamePos = i * 2 - 1;
                int attrValuePos = i * 2;
                String attrName = matcher.group(attrNamePos);
                int attrPos = locateAttributePosition(elementName, attrName);
                String value = matcher.group(attrValuePos);
                ;
                matchingStrings[attrPos] = value;
                if (LOGGER.isTraceEnabled()) {
                    logBuffer.append(":Name=" + attrName + ", Value='" +
                                     value + "':");
                }
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.log(VERBOSE_LOG_LEVEL, logBuffer.toString());
            }
            elementList.add(matchingStrings);
        }
        return elementList;
    }

    /**
     * Locates the posititon of an attribute in the String array returned back.
     *
     * @param elementType
     * @param attributeName
     * @return
     */
    private static int locateAttributePosition(String elementType,
                                               String attributeName) {
        Integer pos =
            (Integer)ATTR_POSITIONS_MAP.get(elementType + attributeName.toLowerCase());
        if (pos != null) {
            return pos.intValue();
        }
        return -1;
    }

    /**
     * Extract the attributes of the first instance of the given
     * HTML element found in the given text. Use them to populate a map.
     *
     * The attributes must be well formed. The value may or may not be within
     * quotes.
     *
     * @param text
     * @param tagName
     * @return
     */
    public static Map extractTagAttributes(String text, String tagName) {
        Pattern pattern =
            Pattern.compile("<(" + tagName + ").*?>", Pattern.CASE_INSENSITIVE |
                            Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            //Extract the tag
            String tag = matcher.group();
            String actualTagName = matcher.group(1);

            //Remove the unwanted elements
            tag = tag.replaceAll("<" + actualTagName, "");
            tag = tag.replaceAll(">", "");
            tag = tag.trim();
            return extractTagAttributes(tag);
        }
        return new HashMap();
    }

    /**
     * Extracts the name value pairs from a string containing many of them
     * separated by spaces. The value may or may not be enclosed by single or
     * double quotes. Spaces in a value within quotes will be recognized
     *
     * @param attributeFragment
     * @return
     */
    private static Map extractTagAttributes(String attributeFragment) {
        //Trim and append a space to the end so that for loop can get all the attributes
        attributeFragment = attributeFragment.trim() + " ";
        Map attribsMap = new HashMap();
        StringBuffer nameBuffer = new StringBuffer();
        StringBuffer valueBuffer = new StringBuffer();
        //We are scanning either for a name or value
        boolean nameScan = true;
        //Spaces need to be ignored

        boolean inQuote = false;
        char quoteChar = ' ';
        boolean firstCharFound = false;
        boolean movingThroSpaces = false;
        for (int i = 0; i < attributeFragment.length(); i++) {
            char c = attributeFragment.charAt(i);
            if (c == '=') {
                movingThroSpaces = false;
                if (nameScan) {
                    //Switch from name scan to value scan
                    nameScan = false;
                    //Ignore all spaces till a char is found
                    movingThroSpaces = true;
                    firstCharFound = false;
                    continue;
                }
            } else if (c == '\'' || c == '"') {
                //Add the character to either name buffer or value buffer
                if (nameScan) {
                    nameBuffer.append(c);
                } else {
                    valueBuffer.append(c);
                }
                //Spaces for values within quotes must be recognized
                if (!firstCharFound) {
                    inQuote = true;
                    movingThroSpaces = false;
                    quoteChar = c;
                } else {
                    if (c == quoteChar) {
                        inQuote = false;
                        quoteChar = ' ';
                    }
                }
                firstCharFound = true;
            } else if (c != ' ') {
                movingThroSpaces = false;
                firstCharFound = true;
                //Add the character to either name buffer or value buffer
                if (nameScan) {
                    nameBuffer.append(c);
                } else {
                    valueBuffer.append(c);
                }
            } else {
                //This must be a space.
                if (inQuote) {
                    //Space in the quote must be recognized
                    if (nameScan) {
                        nameBuffer.append(c);
                    } else {
                        valueBuffer.append(c);
                    }
                    continue;
                }
                if (movingThroSpaces) {
                    continue;
                }
                movingThroSpaces = true;
                if (!nameScan) {
                    String attrName =
                        nameBuffer.toString().toLowerCase().trim();
                    String attrValue = valueBuffer.toString();
                    attrValue = removeQuotes(attrValue);
                    attribsMap.put(attrName, attrValue);
                    nameBuffer = new StringBuffer();
                    valueBuffer = new StringBuffer();
                    //We have got the name value pair so switch to name scan
                    nameScan = true;
                    inQuote = false;
                    firstCharFound = false;
                }
            }
        }
        return attribsMap;
    }

    /**
     * Remove the leading and trailing single or double quotes from
     * the given string. The quote must be the first and last character.
     *
     * @param inString
     * @return
     */
    private static String removeQuotes(String inString) {
        String quote = "";
        if (inString.charAt(0) == '\'') {
            quote = "'";
        } else if (inString.charAt(0) == '"') {
            quote = "\"";
        }
        if (!quote.equals("")) {
            inString = inString.replaceAll(quote, "");
        }

        return inString;
    }

    /**
     * Create a name value pair using the INPUT elements extracted from the
     * given HTML form.
     *
     * @param formData
     * @return
     */
    public static Map createHttpParamsMap(String[] formData) {
        List inputList = HTMLHelper.extractInputElements(formData[0]);
        Map paramsMap = new HashMap();
        for (int i = 0; i < inputList.size(); i++) {
            String[] inputData = (String[])inputList.get(i);
            String name = inputData[HTMLHelper.ATTR_POS_INPUT_NAME];
            String value = inputData[HTMLHelper.ATTR_POS_INPUT_VALUE];
            paramsMap.put(name, value);
        }
        return paramsMap;
    }


    private static String testString =
        "<td colspan=\"2\" height=\"25\" nowrap=\"nowrap\"><span class=\"bodycopy\">Enter your Single Sign-On user name and password.</span></td>                                         \n" +
        "\n" +
        "</tr>\n" +
        "<Form  empty=\"\" Method =  \" POST\" name = LoginForm action=\"/sso/auth\" AutoComplete=\"off\" test=' ab\"c ' test2=\" a b'c \">\n" +
        "<INPUT NAME=\"v\" value=\"v1.4\"  TYPE=\"hidden\" >\n" +
        "<INPUT TYPE=\"hidden\" NAME=\"site2pstoretoken\" value=\"v1.2~DE70790A~B4483FDC70FCCEF9246386D131FFEACEE9E97B9DDBC0CFC2F78F6376B5124E13844EF22E14830ECCE8ED5DF92F9ACEEBFA7688175227E501938FAFC587D94949A766B7368CAFE28687AB22C06F14F15A08D2D9B31B9F3AF066D077AAF64369A353AA8338147EA69C696EF6DACD90D579E158C37E1CDC5F9CD9F002B88AE299D59E5C71734C0EE090F30B2CCA615F0055C27FC1D8D87AEA8533784B8257612D03796DFC599D9DE1EABD767816CDF79B53394569F1045B4CFEAEF23D1509E2DC0A5C1B93F25EE5FAEA857BECD0FFF90DA4\">\n" +
        "<INPUT TYPE=\"hidden\" NAME=\"locale\" value=\"\">\n" +
        "<INPUT TYPE=\"hidden\" NAME=\"appctx\" value=\"\">\n" +
        "<tr>\n" +
        "<td class=\"bodycopy\" align=\"right\" valign=\"middle\">Username&nbsp;&nbsp;</td>\n" +
        "<td class=\"bodycopy\" align=\"left\" height=\"27\" valign=\"top\">\n" +
        "<input name=\"ssousername\" size=\"20\" maxlength=\"80\" value=\"\" type=\"text\"></td>\n" +
        "</tr>\n" +
        "<tr>\n" +
        "\n" +
        "<td align=\"right\" height=\"27\" valign=\"middle\"> <span class=\"bodycopy\">Password&nbsp;&nbsp;</span> </td>\n" +
        "<td class=\"bodycopy\" align=\"left\" height=\"27\" valign=\"top\">\n" +
        "<input name=\"password\" value=\"\" size=\"20\" maxlength=\"255\" type=\"password\">\n" +
        "<a href=\"javascript:doLogin(document.LoginForm);\" onmouseover=\"self.status='Press Go to Sign In'; return true\"> <img src=\"/sso_loginui/go_button.gif\" alt=\"Sign In\" align=\"middle\" border=\"0\"> </a> </td>\n" +
        "</tr>\n" +
        "</form>\n" +
        "<tr>\n" +
        "<td colspan=\"2\" height=\"43\"><a href=\"http://orion-dev.oracle.com:7778/sso/pages/change_password.jsp\" target=\"new\" class=\"bodylink\">Lost your password?</a></td>\n" +
        "</tr>";

    /**
     * @param args
     */
    public static void main(String[] args) {
        List matchingStringsList = extractFormElements(testString);
        String[] formData = (String[])matchingStringsList.get(0);
        String formElement = formData[0];
        System.out.println(formElement);
        Map attribsMap = extractTagAttributes(formElement, "form");
        Iterator keySetIter = attribsMap.keySet().iterator();
        while (keySetIter.hasNext()) {
            String name = (String)keySetIter.next();
            String value = (String)attribsMap.get(name);
            System.out.println("name=:" + name + ":, value=:" + value + ":");
        }
        String[] matchingStrings = (String[])matchingStringsList.get(0);
        extractInputElements(matchingStrings[0]);
    }

}
