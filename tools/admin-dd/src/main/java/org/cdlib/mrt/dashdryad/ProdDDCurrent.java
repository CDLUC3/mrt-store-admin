/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdlib.mrt.dashdryad;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.GetObjectRequest;
import java.net.URLEncoder;
import org.cdlib.mrt.s3.service.CloudResponse;
import org.cdlib.mrt.s3.service.CloudStoreInf;
import org.cdlib.mrt.s3.service.NodeService;
import org.cdlib.mrt.cloud.CloudList;
import org.cdlib.mrt.cloud.ManifestSAX;
import org.cdlib.mrt.cloud.ManifestStr;
import org.cdlib.mrt.cloud.VersionMap;
import org.cdlib.mrt.core.FileComponent;
import org.cdlib.mrt.core.Identifier;
import org.cdlib.mrt.core.MessageDigest;
import org.cdlib.mrt.s3.service.CloudUtil;
import org.cdlib.mrt.s3.tools.CloudManifestCopyNode;
import org.cdlib.mrt.utility.FileUtil;
import org.cdlib.mrt.utility.MessageDigestValue;
import org.cdlib.mrt.utility.LoggerInf;
import org.cdlib.mrt.utility.PropertiesUtil;
import org.cdlib.mrt.utility.StringUtil;
import org.cdlib.mrt.utility.TException;
import org.cdlib.mrt.utility.TFileLogger;
import org.cdlib.mrt.core.FileComponent;
/**
 * 
 *
 * @author replic
 */
public class ProdDDCurrent 
{
    protected static final String NAME = "ProdDDCurrent";
    protected static final String MESSAGE = NAME + ": ";
    protected static final boolean DEBUG = false;
    protected static final String bucketName     = "uc3-s3mrt6001-dev";
    protected static final String prefix        = "mrt";
    protected static final String nameIn        = "node5001";
    protected static final String baseManifestStatic ="/apps/replic/MRTMaven/github/admin/mrt-store-admin/dash-dryad/tasks/210420-current/manifests";
    protected File source = null ;
    protected File baseManifest = null;
    protected LoggerInf logger = null;
    protected File delList = null;
    
    public ProdDDCurrent(String sourcePath, String delListName, LoggerInf logger)
        throws TException
    {
        this.source = new File (sourcePath);
        this.baseManifest = new File(source, "manifests");
        this.delList = new File(source, delListName);
        this.logger = logger;
    }
    
    public static void main(String[] args) 
            throws IOException,TException 
    {
        main_3(args);
    }
    
    public static void main_1(String[] args) 
            throws IOException,TException 
    {
        String sourcePath = "/apps/replic/MRTMaven/github/admin/mrt-store-admin/dash-dryad/tasks/210420-current";
        //String delListName = "tdd.txt";
        String delListName = "dd-del.txt";
        LoggerInf logger = new TFileLogger(NAME, 50, 50);
        ProdDDCurrent dd = new ProdDDCurrent(sourcePath, delListName, logger);
        dd.processList();
    }
    
    public static void main_2(String[] args) 
            throws IOException,TException 
    {
        String basePath = "/apps/replic/MRTMaven/github/admin/mrt-store-admin/dash-dryad/tasks/210420-current/manifests";
        String localID = "Q6H41PB7";
        File base = new File(basePath);
        File source = new File(base,localID);
        File oldMan = new File(source, "old.xml");
        File currentMan = new File(source, "current.xml");
        File buildMan = new File(source, "build.xml");
        File dashLog = new File(source, "dash.log");
        String delListName = "dd-del.txt";
        LoggerInf logger = new TFileLogger(NAME, 50, 50);
        ProdDDCurrent dd = new ProdDDCurrent(basePath, delListName, logger);
        dd.validate(localID,oldMan, currentMan, buildMan, dashLog);
    }
    
    public static void main_3(String[] args) 
            throws IOException,TException 
    {
        String basePath = "/apps/replic/MRTMaven/github/admin/mrt-store-admin/dash-dryad/tasks/210420-current";
        
        String delListName = "dd-del.txt";
        LoggerInf logger = new TFileLogger(NAME, 5, 50);
        ProdDDCurrent dd = new ProdDDCurrent(basePath, delListName, logger);
        dd.validateList();
    }
    
    
    public void processList()
            throws TException 
    {
        try {
            FileInputStream fstream = new FileInputStream(delList);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            int cnt = 0;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                cnt++;
                if (strLine.startsWith("#")) continue;
                String [] parts = strLine.split("\\|");
                if (DEBUG) System.out.println (strLine);
                File groupDir = process(parts[0], parts[1], parts[2]);
                File oldMan = new File(groupDir, "old.xml");
                File currentMan = new File(groupDir, "current.xml");
                File buildMan = new File(groupDir, "build.xml");
                File dashLog = new File(groupDir, "dashLog");
                
                validate(parts[0], oldMan, currentMan, buildMan, dashLog);
            }

         
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            
        }
    }
    
    
    
    public ArrayList<String> getDashArray(File dashFile)
            throws TException 
    {
        try {
            ArrayList<String> dashArray = new ArrayList<String>();
            FileInputStream fstream = new FileInputStream(dashFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            int cnt = 0;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                cnt++;
                if (strLine.startsWith("#")) continue;
                dashArray.add(strLine);
                log(10, "getDashArray add:" + strLine);
            }
            return dashArray;
         
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    
    public static HashMap<String, FileComponent> getObjectHash(VersionMap versionMap)
         throws TException
    {
        HashMap<String, FileComponent> hash = new HashMap<String, FileComponent>();
        try {
                
            int verCnt = versionMap.getVersionCount();
            Identifier id = 
                    versionMap.getObjectID();
            for (int i=1; i<=verCnt; i++) {
                List<FileComponent> comps = versionMap.getVersionComponents(i);
                
                for (FileComponent comp: comps) {
                    String key = comp.getLocalID();
                    hash.put(key, comp);
                }
            }
            return hash;
                
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    public void validateList()
            throws TException 
    {
        try {
            FileInputStream fstream = new FileInputStream(delList);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            int cnt = 0;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                cnt++;
                if (DEBUG) System.out.println("strLine:" + strLine);
                String [] parts = strLine.split("\\|");
                if (DEBUG) System.out.println (strLine);
                String id = getLocalBase(parts[0]);
                File groupDir = new File(baseManifest, id);
                File oldMan = new File(groupDir, "old.xml");
                File currentMan = new File(groupDir, "current.xml");
                File buildMan = new File(groupDir, "build.xml");
                File dashLog = new File(groupDir, "dashcurrent.log");
                validate(parts[0], oldMan, currentMan, buildMan, dashLog);
                if (cnt > 100) break;
            }

         
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            
        }
    }
    
    protected File process(String localID, String oldArk, String currentArk)
            throws TException 
    {
        File groupDir = null;
        try {
            System.out.println("process:"
                    + " - localID=" + localID
                    + " - oldArk=" + oldArk
                    + " - newArk=" + currentArk
            );
            VersionMap oldMap = getVersionMap(5001, oldArk);
            VersionMap currentMap = getVersionMap(3041, currentArk);
            Identifier currentObjectID = currentMap.getObjectID();
            Identifier oldObjectID = oldMap.getObjectID();
            System.out.println("***old id:" + oldObjectID.getValue());
            System.out.println("***new id:" + currentObjectID.getValue());
            
            HashMap<String, FileComponent> currentHash = getObjectHash(currentMap);
            HashMap<String, FileComponent> oldHash = getObjectHash(oldMap);
            VersionMap buildMap = new VersionMap(currentObjectID, logger);
            int verOldCnt = oldMap.getVersionCount();
            int verCurrentCnt = currentMap.getVersionCount();
            List lastComp = null;
            for (int i=1; i<=verOldCnt; i++) {
                ArrayList<FileComponent> buildComp = new ArrayList<FileComponent>();
                List<FileComponent> oldComp = oldMap.getVersionComponents(i);
                List<FileComponent> currentComp = currentMap.getVersionComponents(i);
                addSystem(currentComp, buildComp);
                addProducer(oldComp, buildComp, currentHash, oldObjectID.getValue(),  currentObjectID.getValue());
                addProvenance(currentComp, buildComp);
                buildMap.addVersion(buildComp);
                lastComp = buildComp;
            }
            // work
            if (verCurrentCnt > verOldCnt) {
                System.out.println("Add new");
                for (int i=verOldCnt + 1; i<=verCurrentCnt; i++) {
                    System.out.println("Add new:" + i);
                    List<FileComponent> currentComp = currentMap.getVersionComponents(i);
                    List<FileComponent> newVersionComp = getNewVersion(i, lastComp, currentComp);
                    buildMap.addVersion(newVersionComp);
                    lastComp = newVersionComp;
                }
                //throw new TException.UNIMPLEMENTED_CODE("Needs work:" + localID + "|" + currentArk);
            }
            //
            writeMap(oldMap, localID, "old.xml");
            writeMap(currentMap, localID, "current.xml");
            groupDir = writeMap(buildMap, localID, "build.xml");
            return groupDir;
            
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
    
    protected  List<FileComponent> getNewVersion(
            int currentVersion, 
            List<FileComponent> lastComp, 
            List<FileComponent> currentComp)
        throws TException 
    {
        
        ArrayList<FileComponent> buildComp = new ArrayList<FileComponent>();
        try {
            HashMap<String, FileComponent> lastHash = getComponentHash(lastComp);
            HashMap<String, FileComponent> currentHash = getComponentHash(currentComp);
            //add system on version
            for (FileComponent component: currentComp) {
                String key = component.getLocalID();
                System.out.println("NewVersion: new version system add(" + currentVersion + ")"  + "=" + key);
                if (key.contains("|system/")) {
                    buildComp.add(component);
                    System.out.println(">>NewVersion: new version system add(" + currentVersion + ")"  + "=" + key);
                }
            }
            
            //add new content on version
            for (FileComponent component:currentComp) {
                String key = component.getLocalID();
                System.out.println("NewVersion: new version producer add(" + currentVersion + ")"  + "=" + key);
                if (key.contains("|" + currentVersion + "|") 
                        && (key.contains("|producer/"))) {
                    buildComp.add(component);
                    System.out.println(">>NewVersion: new version producer add(" + currentVersion + ")"  + "=" + key);
                }
            }
            
            //add old version that is continued
            for (FileComponent component:lastComp) {
                String key = component.getLocalID();
                System.out.println("NewVersion: old version add(" + currentVersion + ")" + "=" + key);
                if (key.contains("|system/")) continue;
                FileComponent testComponent = currentHash.get(key);
                if (testComponent != null) {
                    buildComp.add(testComponent);
                    System.out.println(">>NewVersion: old version add(" + currentVersion + ")" + "=" + key);
                }
            }
            return buildComp;
            
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
    
    protected void validate(String localID, File oldMan, File currentMan, File buildMan, File dashLog)
            throws TException 
    {
        try {
            log(2, "***validate:"
                    + " - oldman=" + oldMan.getCanonicalPath() + "\n"
                    + " - currentMan=" + currentMan.getCanonicalPath() + "\n"
                    + " - buildMan=" + buildMan.getCanonicalPath() + "\n"
            );
            VersionMap oldMap = getVersionMap(oldMan);
            VersionMap currentMap = getVersionMap(currentMan);
            VersionMap buildMap = getVersionMap(buildMan);
            
            Identifier oldID = oldMap.getObjectID();
            Identifier currentID = currentMap.getObjectID();
            Identifier buildID = buildMap.getObjectID();
            
            // build has identifier of current
            if (!buildID.getValue().equals(currentID.getValue())) {
                throw new TException.INVALID_OR_MISSING_PARM(MESSAGE + "validate - ID not matching::"
                        + " - buildID:" + buildID.getValue()
                        + " - currentID:" + currentID.getValue()
                );
            }
            
            log(1, "***VALIDATE - IDs:"
            + " - old id:" + oldID.getValue()
            + " - current id:" + currentID.getValue()
            + " - buildID id:" + buildID.getValue()
            );
            
            HashMap<String, FileComponent> oldVersionHash = getVersionHash(oldMap);
            HashMap<String, FileComponent> currentVersionHash = getVersionHash(currentMap);
            HashMap<String, FileComponent> buildVersionHash = getVersionHash(buildMap);
            int oldCnt = oldMap.getVersionCount();
            int currentCnt = currentMap.getVersionCount();
            int buildCnt = buildMap.getVersionCount();
            
            if (buildCnt != currentCnt) {
                throw new TException.INVALID_OR_MISSING_PARM(MESSAGE + "validate - version counts do not match:"
                        + " - buildCnt:" + buildCnt
                        + " - currentCnt:" + currentCnt
                );
            }
            matchVersionHash(localID + "=version: old build prod", oldCnt, oldMap, buildVersionHash, "producer/");
            matchVersionHash(localID + "=version: build old prod", oldCnt, buildMap, oldVersionHash, "producer/");
            matchVersionHash(localID + "=version: current build sys", currentCnt, currentMap, buildVersionHash, "system/");
            matchVersionHash(localID + "=version: build current sys", currentCnt, buildMap, currentVersionHash, "system/");
            
            HashMap<String, FileComponent> currentObjectHash = getObjectHash(currentMap);
            HashMap<String, FileComponent> buildObjectHash = getObjectHash(buildMap);
            matchObjectHash(localID + "=object: current build", currentMap, buildObjectHash);
            matchObjectHash(localID + "=object: build current", buildMap, currentObjectHash);
            
            validateDash(localID, dashLog, buildMap);
            
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
    
    
    protected void validateDash(String doi, File dashFile, VersionMap map)
        throws TException 
    {
        try {
            log(10, "***validateDash:"
                    + " - oldman=" + dashFile.getCanonicalPath() + "\n"
            );
            ArrayList<String> dashArray = getDashArray(dashFile);
            int currentVersion = map.getCurrent();
            List<FileComponent> mapCurrent = map.getVersionComponents(currentVersion);
            HashMap<String, String> versionDashHash = buildCurrentDashHash(dashArray);
            //HashMap<String, FileComponent> versionDashHash = getVersionDashHash(map);
            int cnt = 0;
            int misscnt = 0;
            for (FileComponent current : mapCurrent) {
                String mapPath = current.getIdentifier();
                if (mapPath.startsWith("system/")) continue;
                if (mapPath.startsWith("producer/mrt-")) continue;
                if (mapPath.equals("producer/stash-wrapper.xml")) continue;
                String found = versionDashHash.get(mapPath);
                if (found == null) {
                    log(0, "***NOT FOUND"
                            + " - doi:" + doi
                            + " - current:" + currentVersion
                            + " - file:" + mapPath
                            );
                    misscnt++;
                }
                cnt++;
            }
            log(2, "***validateDash:"
                    + " - cnt=" + cnt
                    + " - misscnt=" + misscnt
                    + " - MATCH=" + dashFile.getCanonicalPath() + "\n"
            );
            
            HashMap<String, FileComponent> mapHash = getVersionHash(map, currentVersion);
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
    
    public HashMap<String, FileComponent> getVersionDashHash(VersionMap versionMap)
         throws TException
    {
        HashMap<String, FileComponent> hash = new HashMap<String, FileComponent>();
        try {
                
            int verCnt = versionMap.getVersionCount();
            Identifier objectID = versionMap.getObjectID();
            String ark = objectID.getValue();
            for (int i=1; i<=verCnt; i++) {
                List<FileComponent> comps = versionMap.getVersionComponents(i);
                String zpad = String.format("%03d", i);
                String prefix = "[" + ark + "|" + zpad + "]=";
                for (FileComponent comp: comps) {
                    String key = comp.getLocalID();
                    String dashKey = prefix + key;
                    hash.put(dashKey, comp);
                    log(10, "getVersionDashHas - key=" + dashKey);
                }
            }
            return hash;
                
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    
    public HashMap<String, FileComponent> getVersionHash(VersionMap versionMap, int versionID)
         throws TException
    {
        HashMap<String, FileComponent> hash = new HashMap<String, FileComponent>();
        try {
            
            int verCnt = versionMap.getVersionCount();
            if (versionID > verCnt) {
                throw new TException.INVALID_OR_MISSING_PARM("getVersionHash - version not found:"
                        + " - map count:" + verCnt
                        + " - select:" + versionID
                );
            }
            
            List<FileComponent> comps = versionMap.getVersionComponents(versionID);
            for (FileComponent comp: comps) {
                String key = comp.getLocalID();
                hash.put(key, comp);
                log(10, "getVersionDashHas - key=" + key);
            }
            return hash;
                
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    public HashMap<String, String> buildCurrentDashHash(List<String> dashPaths)
         throws TException
    {
        HashMap<String, String> hash = new HashMap<String, String>();
        try {
            int size = dashPaths.size();
            String lastLine = dashPaths.get(size-1);
            String currentS = lastLine.substring(0,3);
            for (String comp: dashPaths) {
                if (!comp.startsWith(currentS)) continue;
                String path = comp.substring(4);
                String key = "producer/" + path;
                hash.put(key, path);
                log(10, "buildCurrentDashHash"
                        + " - key=" + key
                        + " - path=" + path
                );
            }
            return hash;
                
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    protected void matchVersionHash(
            String testName, int verCnt, VersionMap inMap, HashMap<String, FileComponent> testHash, String prefix)
        throws TException
    {
        try {
                
            //int verCnt = inMap.getVersionCount();
            for (int i=1; i<=verCnt; i++) {
                List<FileComponent> comps = inMap.getVersionComponents(i);
                
                for (FileComponent comp: comps) {
                    String key = comp.getLocalID();
                    if (key.contains("delete.txt")) continue;
                    if (key.contains("|producer/mrt-provenance.xml")) continue;
                    String [] parts = key.split("\\|");
                    if (parts[2].contains(prefix)) {
                        String testKey = "" + i + "=" + parts[1] + "|" + parts[2];
                        FileComponent testComp = testHash.get(testKey);
                        if (testComp == null) {
                            throw new TException.INVALID_OR_MISSING_PARM(MESSAGE + "matchHash - value not found:"
                                    + " - testKey:" + testKey
                                    + " - testName:" + testName
                                    + " - testComp:" + testComp
                            );
                        }
                    }
                }
            }
            log(1, "match(" + verCnt + "):" + testName);
            
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
    
    protected void matchObjectHash(
            String testName, VersionMap inMap, HashMap<String, FileComponent> testHash)
        throws TException
    {
        try {
                
            int verCnt = inMap.getVersionCount();
            for (int i=1; i<=verCnt; i++) {
                List<FileComponent> comps = inMap.getVersionComponents(i);
                
                for (FileComponent comp: comps) {
                    String key = comp.getLocalID();
                    if (key.contains("delete.txt")) continue;
                    FileComponent testComp = testHash.get(key);
                    if (testComp == null) {
                        throw new TException.INVALID_OR_MISSING_PARM(MESSAGE + "matchObjectHash - value not found:"
                                + " - object test:" + testName
                                + " - key:" + key
                        );
                    }
                    
                }
            }
            log(1, "match(" + verCnt + "):" + testName);
            
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
    
    public VersionMap getVersionMap(File mapFile)
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
    
    public static HashMap<String, FileComponent> getComponentHash(List<FileComponent> comps)
         throws TException
    {
        HashMap<String, FileComponent> hash = new HashMap<String, FileComponent>();
        try {
           
            for (FileComponent comp: comps) {
                String key = comp.getLocalID();
                hash.put(key, comp);
            }
            return hash;
                
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
            
    public static void addProducer(
            List<FileComponent> comps, 
            List<FileComponent> buildcomp, 
            HashMap<String, FileComponent> currentHash,
            String oldArk, 
            String newArk)
        throws TException
    {
        for (FileComponent comp: comps) {
            if (comp.getLocalID().contains("producer/")) {
                String key = comp.getLocalID();
                //System.out.println("inprod  key:" + key);
                String newKey = key.replace(oldArk, newArk);
                //System.out.println("new key:" + newKey);
                FileComponent hashComp = currentHash.get(newKey);
                if (newKey.contains("delete.txt")) continue;
                if (hashComp == null) {
                    throw new TException.REQUESTED_ITEM_NOT_FOUND("Lookup fails:" + newKey);
                }
                comp.setLocalID(newKey);
                buildcomp.add(comp);
            }
        }
    }
            
    public static void addProvenance(
            List<FileComponent> currComp,
            List<FileComponent> buildComp)
        throws TException
    {
        for (FileComponent comp: currComp) {
            String key = comp.getLocalID();
            if (key.contains("producer/mrt-provenance.xml")) {
                System.out.println(">>>FOUND++++");
                buildComp.add(comp);
            }
        }
    }
    
    public static void addSystem(
            List<FileComponent> comps, 
            List<FileComponent> buildcomp)
        throws TException
    {
        for (FileComponent comp: comps) {
            if (comp.getLocalID().contains("system/")) {
                buildcomp.add(comp);
            }
        }
    }

    
    public static VersionMap getVersionMap(long node, String ark)
        throws TException
    {
        String base = "http://uc3-mrtstore05x2-prd:35121/manifest/";
        try {
            String encArk = URLEncoder.encode(ark, "utf-8");
            String manU = base + node + "/" + encArk;
            VersionMap man = getVersionMap(manU);
            System.out.println(man.dump("ark"));
            return man;
            
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
            
    public static VersionMap getVersionMap(String manifestXMLUrlS)
        throws TException
    {
            String storeURL = "http://uc3-mrtstore05x2-prd:35121/manifest/";
            try {
                VersionMap versionMap = VersionMap.getVersionMap(manifestXMLUrlS);
                return versionMap;
                
            } catch (Exception ex) {
                ex.printStackTrace();
                throw new TException(ex);
            }
    }
    
    protected static String getShoulder(Identifier ark)
        throws TException
    {
        
        try {
                String arkS  = ark.getValue();
                String [] parts = arkS.split("/");
                String shoulder = parts[1];
                return shoulder;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    protected static String getLocalBase(String localID)
        throws TException
    {
        
        try {
                String [] parts = localID.split("/");
                String shoulder = parts[1];
                return shoulder;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
            
    public File writeMap(VersionMap map, String localID, String name)
        throws TException
    {
        try {
            String id = getLocalBase(localID);
            File group = new File(baseManifest, id);
            if (! group.exists()){
                group.mkdirs();
            }
            File mapFile = new File(group, name);

            ManifestStr.buildManifest(map, mapFile);
            return group;

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
    }
    
    protected ArrayList<String> getFileArray(String fileName)
        throws TException
    {
        try {
            ArrayList<String> lines = new ArrayList<>(Files.readAllLines(Paths.get(fileName)));
            return lines;
        }
        catch (Exception e) {
            throw new TException(e);
        }
    }
    
    protected void log(int lvl, String msg)
        throws TException
    {
        logger.logMessage(msg, lvl, true);
    }
}
