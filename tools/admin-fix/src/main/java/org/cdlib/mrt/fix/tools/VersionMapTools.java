/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdlib.mrt.fix.tools;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.cdlib.mrt.cloud.ManifestSAX;
import org.cdlib.mrt.cloud.ManifestStr;
import org.cdlib.mrt.cloud.VersionMap;
import org.cdlib.mrt.core.Identifier;
import org.cdlib.mrt.utility.LoggerAbs;
import org.cdlib.mrt.utility.LoggerInf;
import org.cdlib.mrt.utility.PropertiesUtil;
import org.cdlib.mrt.utility.TException;
import org.cdlib.mrt.utility.TFileLogger;
import org.cdlib.mrt.core.FileComponent;
import org.cdlib.mrt.s3.service.NodeIO;
/**
 * 
 *
 * @author replic
 */
public class VersionMapTools 
{
    protected static final String NAME = "VersionMapTools";
    protected static final String MESSAGE = NAME + ": ";
    protected static final boolean DEBUG = false;
    
    
    public static VersionMap getFile(File mapFile, LoggerInf logger)
        throws TException
    {
        try {
            FileInputStream inStream = new FileInputStream(mapFile);
            VersionMap map = ManifestSAX.buildMap(inStream, logger);
            return map;
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            throw tex;
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
            
    public static VersionMap getUrl(String manifestXMLUrlS)
        throws TException
    {
            //String storeURL = "http://uc3-mrtstore05x2-prd:35121/manifest/";
            try {
                VersionMap versionMap = VersionMap.getVersionMap(manifestXMLUrlS);
                return versionMap;
                
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new TException(ex);
            }
    }
    
            
    public static File writeFile(String baseManifest, VersionMap map, String name)
        throws TException
    {
        try {
            File base = new File(baseManifest);
            if (! base.exists()){
                base.mkdirs();
            }
            File mapFile = new File(base, name);

            ManifestStr.buildManifest(map, mapFile);
            return mapFile;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
}
