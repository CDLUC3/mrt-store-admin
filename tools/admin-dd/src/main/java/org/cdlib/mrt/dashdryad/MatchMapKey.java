/*
Copyright (c) 2005-2012, Regents of the University of California
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
 *
- Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.
- Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
- Neither the name of the University of California nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
OF THE POSSIBILITY OF SUCH DAMAGE.
**********************************************************/
package org.cdlib.mrt.dashdryad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import org.cdlib.mrt.cloud.ManInfo;
import org.cdlib.mrt.cloud.ManifestSAX;
import org.cdlib.mrt.cloud.VersionMap;

import org.cdlib.mrt.core.ComponentContent;
import org.cdlib.mrt.core.DateState;
import org.cdlib.mrt.core.FileComponent;
import org.cdlib.mrt.core.Identifier;
import org.cdlib.mrt.core.Manifest;
import org.cdlib.mrt.core.ManifestRowAbs;
import org.cdlib.mrt.core.ManifestRowAdd;
import org.cdlib.mrt.core.MessageDigest;
import org.cdlib.mrt.utility.FileUtil;
import org.cdlib.mrt.utility.FixityTests;
import org.cdlib.mrt.utility.LinkedHashList;
import org.cdlib.mrt.utility.LoggerInf;
import org.cdlib.mrt.utility.StringUtil;
import org.cdlib.mrt.utility.TException;
import org.cdlib.mrt.utility.TFileLogger;
import org.cdlib.mrt.utility.URLEncoder;

/**
 * This object imports the formatTypes.xml and builds a local table of supported format types.
 * Note, that the ObjectFormat is being deprecated and replaced by a single format id (fmtid).
 * This change is happening because formatName is strictly a description and has no functional
 * use. The scienceMetadata flag is being dropped because the ORE Resource Map is more flexible
 * and allows for a broader set of data type.
 * 
 * @author dloy
 */
public class MatchMapKey
{
    private static final String NAME = "MatchMap";
    private static final String MESSAGE = NAME + ": ";

    private static final String NL = System.getProperty("line.separator");
    private static final boolean DEBUG = false;
    
    protected VersionMap mapOne = null;
    protected VersionMap mapTwo = null;
    protected HashMap<String, FileComponent> hashOne = null;
    protected HashMap<String, FileComponent> hashTwo = null;
    protected ArrayList<String> arrOne = null;
    protected ArrayList<String> arrTwo = null;
    protected int curOne = 0;
    protected int curTwo = 0;
    
    protected PrintWriter  pw = null;
    protected File  pwF = null;
    protected File  mapOneF = null;
    protected File  mapTwoF = null;
    protected LoggerInf logger = null;
    protected String header = null;
    protected boolean missingMap = false;
    protected boolean differentCurrent = false;
    protected boolean differentContent = false;
    protected boolean subset = false;
    
    public MatchMapKey(String header, File mapOneF, File mapTwoF, File pwF, LoggerInf logger)
        throws TException
    {
        this.header = header;
        this.mapOneF = mapOneF;
        this.mapTwoF = mapTwoF;
        this.mapOne = getVersionMap(mapOneF);
        this.mapTwo = getVersionMap(mapTwoF);
        this.pwF = pwF;
        this.logger = logger;
        build();
    }
    
    public MatchMapKey(String header, File mapOneF, File mapTwoF, LoggerInf logger)
        throws TException
    {
        this.header = header;
        this.mapOneF = mapOneF;
        this.mapTwoF = mapTwoF;
        this.mapOne = getVersionMap(mapOneF);
        this.mapTwo = getVersionMap(mapTwoF);
        this.pwF = null;
        this.logger = logger;
        build();
    }
    
    public MatchMapKey(VersionMap mapOne, VersionMap mapTwo, File pwF, LoggerInf logger)
        throws TException
    {
        this.header = header;
        this.mapOne = mapOne;
        this.mapTwo = mapTwo;
        this.pwF = pwF;
        this.logger = logger;
        build();
    }
    
    public static void main(String[] args) 
            throws IOException,TException 
    {
        main_1(args);
    }
    
    public static void main_1(String[] args) 
            throws IOException,TException 
    {
        String basePath = "/home/loy/MRTMaven/github/admin/mrt-store-admin/tasks/210420-current/test/manifests";
        //String localID = "Q67P8W9Z";
        String localID = "Q6CC0XMH";
        File base = new File(basePath);
        File source = new File(base,localID);
        File oldMan = new File(source, "old.xml");
        File currentMan = new File(source, "current.xml");
        File buildMan = new File(source, "build.xml");
        File pw1F = new File(source, "diffoldcur.log");
        File pw2F = new File(source, "diffoldbld.log");
        File pw3F = new File(source, "diffcurbld.log");
        LoggerInf logger = new TFileLogger(NAME, 50, 50);
        MatchMapKey mmk = new MatchMapKey("old - current", oldMan, currentMan, pw1F, logger);
        mmk.match();
        MatchMapKey mmk2 = new MatchMapKey("old - build", oldMan, buildMan, pw2F, logger);
        mmk2.match();
        MatchMapKey mmk3 = new MatchMapKey("current - build", currentMan, buildMan, pw3F, logger);
        mmk3.match();
    }
    
    public static void main_2(String[] args) 
            throws IOException,TException 
    {
        String basePath = "/apps/replic/MRTMaven/github/admin/mrt-store-admin/dash-dryad/tasks/210420-current/test/manifests";
        //String localID = "Q67P8W9Z";
        String localID = "Q6CC0XMH";
        File base = new File(basePath);
        File source = new File(base,localID);
        File oldMan = new File(source, "old.xml");
        File currentMan = new File(source, "current.xml");
        File buildMan = new File(source, "build.xml");
        LoggerInf logger = new TFileLogger(NAME, 50, 50);
        MatchMapKey mmk = new MatchMapKey("old - current", oldMan, currentMan, logger);
        mmk.match();
        MatchMapKey mmk2 = new MatchMapKey("old - build", oldMan, buildMan,  logger);
        mmk2.match();
        MatchMapKey mmk3 = new MatchMapKey("current - build", currentMan, buildMan, logger);
        mmk3.match();
    }
    
    protected void build() 
        throws TException
    {
        try {
            if (pwF != null) {
                pw =  new PrintWriter(new FileWriter(pwF));
            }
            curOne = mapOne.getCurrent();
            curTwo = mapTwo.getCurrent();
            hashOne = getVersionHash(mapOne);
            hashTwo = getVersionHash(mapTwo);
            arrOne = getVersionList(mapOne);
            arrTwo = getVersionList(mapTwo);
        } catch (Exception ex) {
            throw new TException(ex);
        }
    }
    
    protected void match() 
        throws TException
    {
        try {
            log(" ");
            log("**********************************************************");
            log("MATCH:" + header + " +++");
            if ((mapOneF != null) && (mapTwoF != null)) {
                log("<<<:" + mapOneF.getCanonicalPath() );
                log(">>>:" + mapTwoF.getCanonicalPath() );
            }
            log(" ");
            int maxVersion = curOne;
            if (curTwo > curOne) maxVersion = curTwo;
            for (int i=1; i<=maxVersion; i++) {
                log(" ");
                log("***VERSION:" + i);
                log(" ");
                String pre = "" + i + "=";

                int mchcnt = 0;
                for (String key: arrOne) {
                    if (!key.startsWith(pre)) continue;
                    if (hashTwo.get(key) != null) {
                        //System.out.println("<->:" + key);
                        mchcnt++;
                    } else {
                        log("<--:" + key);
                    }
                }
                log("<*>:" + i + "=| match count=" + mchcnt);

                for (String key: arrTwo) {
                    if (!key.startsWith(pre)) continue;
                    if (hashOne.get(key) != null) {
                    } else {
                        log("-->:" + key);
                    }
                }
            } 
        } catch (TException tex) {
            tex.printStackTrace();
            throw tex;
            
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
            
        } finally {
            try {
                pw.close();
            } catch (Exception e) { }
        
        }
    }
    
    public static HashMap<String, FileComponent> getVersionHash(VersionMap versionMap)
         throws TException
    {
        HashMap<String, FileComponent> hash = new HashMap<String, FileComponent>();
        try {
                
            int verCnt = versionMap.getVersionCount();
            for (int i=1; i<=verCnt; i++) {
                List<FileComponent> comps = versionMap.getVersionComponents(i);
                
                for (FileComponent comp: comps) {
                    String key = comp.getLocalID();
                    String [] parts = key.split("\\|");
                    String testKey = "" + i + "=" + parts[1] + "|" + parts[2];
                    //System.out.println("getversionHash:" + testKey);
                    hash.put(testKey, comp);
                }
            }
            return hash;
                
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    public static ArrayList<String> getVersionList(VersionMap versionMap)
         throws TException
    {
        ArrayList<String> list = new ArrayList<String>();
        try {
                
            int verCnt = versionMap.getVersionCount();
            for (int i=1; i<=verCnt; i++) {
                List<FileComponent> comps = versionMap.getVersionComponents(i);
                
                for (FileComponent comp: comps) {
                    String key = comp.getLocalID();
                    String [] parts = key.split("\\|");
                    String testKey = "" + i + "=" + parts[1] + "|" + parts[2];
                    //System.out.println("getversionHash:" + testKey);
                    list.add(testKey);
                }
            }
            return list;
                
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    
    
    public VersionMap getVersionMap(File mapFile)
        throws TException
    {
        try {
            FileInputStream inStream = new FileInputStream(mapFile);
            VersionMap map = ManifestSAX.buildMap(inStream, logger);
            return map;
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    
    
    protected void log(String msg)
        throws TException
    {
       
        if (pw != null) { 
            msg += "\n";
            pw.write(msg);
        } else {
            System.out.println(msg);
        }
    }
}
