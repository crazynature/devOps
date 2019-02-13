/* $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAJsonHelper.java /main/1 2013/01/24 19:52:33 dalai Exp $ */

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
 *  @version $Header: mos/source/km/kminfra/BugJiraBridge/BugJiraBridge/src/com/oracle/bugjirabridge/jira/util/JIRAJsonHelper.java /main/1 2013/01/24 19:52:33 dalai Exp $
 *  @author  dalai   
 *  @since   release specific (what release of product did this appear in)
 */

package com.oardc.jira.util;



import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;

//import oracle.mos.fwk.extsvc.common.ExtSvcResponse;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;



public class JIRAJsonHelper {
    
    //Default Encoding
    public static final String ENCODING_UTF_8 = "UTF-8";
    private static final SerializationConfig.Feature[] DEFAULT_SER_WITH =     
            new SerializationConfig.Feature[] { 
                SerializationConfig.Feature.AUTO_DETECT_GETTERS,
                SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS
            };
                //SerializationConfig.Feature.REQUIRE_SETTERS_FOR_GETTERS,

    private static final SerializationConfig.Feature[] DEFAULT_SER_WITHOUT =     
            new SerializationConfig.Feature[] { 
                SerializationConfig.Feature.AUTO_DETECT_FIELDS
            };    
                //SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS

    private static final DeserializationConfig.Feature[] DEFAULT_DESER_WITH =     
            new DeserializationConfig.Feature[] { 
                DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS
            };

    private static final DeserializationConfig.Feature[] DEFAULT_DESER_WITHOUT =     
            new DeserializationConfig.Feature[] { 
            }; 

    public static String serializeObject(Object object) throws Exception{
        ObjectMapper mapper = createMapper(true);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        mapper.writeValue(baos, object);
        return baos.toString(ENCODING_UTF_8);
    }


    static Object deserializeResponse(InputStream response) throws Exception{
        InputStreamReader isr = new InputStreamReader(response, ENCODING_UTF_8);
        int pushBackBufferSize = 1024;
        PushbackReader pbr = new  PushbackReader(isr, pushBackBufferSize);
        
        if(!isValidJsonData(pbr, pushBackBufferSize)){
            String msg = "Invalid JSON Data";
            int maxCharsFromResponse = 1024;
            char[] chars = new char[maxCharsFromResponse];
            int charsRead = pbr.read(chars);
            
            if(charsRead > 0){
                msg = msg + ": "+ new String(chars,0, charsRead);
            }
            
            throw new Exception( msg);
        }
        
        ObjectMapper mapper = createMapper(false);
        Object extSvcResponse = mapper.readValue(pbr, Object.class);
        return extSvcResponse;
    } 
   
    static boolean isValidJsonData(PushbackReader response, int pushBackBufferSize) throws Exception{
        char[] jsonCharBuf = new char[pushBackBufferSize];
        
        int jsonCharsRead = response.read(jsonCharBuf);
        if(jsonCharsRead == -1){
            return false;
        }
        
        boolean validJson = false;
        for (int i = 0; i < jsonCharsRead; i++) {  
            int nextChar = jsonCharBuf[i];
            
            if(nextChar == -1){
                validJson = false;
            }
            
            //Skip spaces
            if(Character.isWhitespace(nextChar)){
                continue;
            }
            
            //Check the first non-space char
            if( nextChar == '[' || nextChar == '{' ){
                validJson = true;
            }
            break;
        }
        
        response.unread(jsonCharBuf, 0, jsonCharsRead);
        
        return validJson;    
    }

    
    public static Object deserializeResponse(String response, Class responseClass) throws Exception{
        ObjectMapper mapper = createMapper(false);
        Object jiraResponse =(Object) mapper.readValue(response, responseClass);
        return jiraResponse;
    } 
    
    private static ObjectMapper createMapper(boolean forSerailization){
        ObjectMapper mapper = new ObjectMapper();
        //mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES,
                         false);
mapper.configure(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS, true);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                                   JsonTypeInfo.As.PROPERTY);       
        if(forSerailization){
            configureMapperForSerialization(mapper);
/*
        }else{
            configureMapperForDeserialization(mapper);
*/
        }
        
        return mapper;
    }    
    
    private static void configureMapperForSerialization(ObjectMapper mapper) {
        SerializationConfig config = mapper.getSerializationConfig();
        
        if(DEFAULT_SER_WITH.length > 0){
            for (SerializationConfig.Feature f : DEFAULT_SER_WITH) {
                config.enable(f);
            }
            //config = config.with(DEFAULT_SER_WITH);    
        }
        
        if(DEFAULT_SER_WITHOUT.length > 0){
            for (SerializationConfig.Feature f : DEFAULT_SER_WITHOUT) {
                config.disable(f);
            }
            //config = config.without(DEFAULT_SER_WITHOUT);    
        }
        
        config.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        mapper.setSerializationConfig(config);
    }


    private static void configureMapperForDeserialization(ObjectMapper mapper) {
        DeserializationConfig config = mapper.getDeserializationConfig();

        if(DEFAULT_DESER_WITH.length > 0){
            for (DeserializationConfig.Feature f : DEFAULT_DESER_WITH) {
                config.enable(f);
            }
            //config = config.with(DEFAULT_DESER_WITH);    
        }
        
        if(DEFAULT_DESER_WITHOUT.length > 0){
            for (DeserializationConfig.Feature f : DEFAULT_DESER_WITHOUT) {
                config.disable(f);
            }
            //config = config.without(DEFAULT_DESER_WITHOUT);    
        }
       
        mapper.setDeserializationConfig(config);
    }    
}
