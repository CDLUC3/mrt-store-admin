/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdlib.mrt.fix.dataone;
import org.cdlib.mrt.fix.*;
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
import org.cdlib.mrt.fix.tools.ReplaceManifest;
import org.cdlib.mrt.fix.tools.MatchMapKey;
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
public class FixCurrent 
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
    protected File processList = null;
    protected FixConfig config = null;
    
    public FixCurrent(String sourcePath)
        throws TException
    {
        config = FixConfig.useYaml(sourcePath);
        this.source = config.getSourcePath();
        this.baseManifest = config.getManifestsPath();
        this.processList = config.getProcessList();
        this.logger = config.getLogger();
    }
    
    
    public void processList()
            throws TException 
    {
        System.out.println("**start processList:" + processList);
        try {
            FileInputStream fstream = new FileInputStream(processList);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            int cnt = 0;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                cnt++;
                if (strLine.startsWith("#")) continue;
                String [] parts = strLine.split("\\|");
                if (DEBUG) System.out.println (strLine);
                String collection = parts[0];
                String dateS = parts[1];
                String localID = parts[2];
                String arkS = parts[3];
                String versionS = parts[4];
                
                String id = getLocalBase(localID);
                File groupDir = new File(baseManifest, id);
                int setVersion = Integer.parseInt(versionS);
                
                process(localID, arkS, setVersion);
            }
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            
        }
    }
   
    protected File process(String localID, String ark, int originalCurrent)
            throws TException 
    {
        File groupDir = null;
        try {
            System.out.println("process:"
                    + " - localID=" + localID
                    + " - oldArk=" + ark
            );
            long currentNode = config.getNode();
            VersionMap currentMap = config.getVersionMap(currentNode, ark);
            writeMap(currentMap, localID, "current.xml");
            
            int startCurrent = currentMap.getCurrent();
            if (startCurrent < originalCurrent) {
                throw new TException.INVALID_DATA_FORMAT("StartCurrent < OriginalCurrent:"
                        + " - startCurrent:" + startCurrent
                        + " - originalCurrent:" + originalCurrent
                );
            }
            
            while (true) {
                int current = currentMap.getCurrent();
                if (current <= originalCurrent) break;
                currentMap.deleteCurrent();
            }
            writeMap(currentMap, localID, "built.xml");
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
    
    public void updateList()
            throws TException 
    {
        try {
            List<Long> nodes = config.getFixNodes();
            FileInputStream fstream = new FileInputStream(processList);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            int cnt = 0;

            //Read File Line By Line
            while ((strLine = br.readLine()) != null)   {
                cnt++;
                if (strLine.startsWith("#")) continue;
                if (DEBUG) System.out.println("strLine:" + strLine);
                String [] parts = strLine.split("\\|");
                if (DEBUG) System.out.println (strLine);
                String id = getLocalBase(parts[2]);
                File groupDir = new File(baseManifest, id);
                File currentMan = new File(groupDir, "current.xml");
                File builtMan = new File(groupDir, "built.xml");
                
                Identifier ark = new Identifier(parts[3]);
                System.out.println("Update ark=" + ark.getValue());
                update(nodes, ark, currentMan, builtMan);
                //if (cnt > 0) break;
            }

         
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            
        }
    }
    
    protected void update(List<Long> nodes, Identifier ark, File currentManifest, File builtManifest)
            throws TException 
    {
        try {
            
            for (Long node: nodes) {
                try {
                    fix(node, ark, currentManifest, builtManifest);
                } catch (Exception ex) {
                    log(0, "***Fail update node=" + node + " - ark=" + ark.getValue());
                    log(0, ex.toString());
                    continue;
                }
            }
            
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
    
    protected void fix(Long node, Identifier ark, File currentManifest, File builtManifest)
            throws TException 
    {
        ReplaceManifest repMan= null;
        try {
            NodeIO.AccessNode accessNode = config.getAccessNode(node);
            repMan = new ReplaceManifest(accessNode, ark, currentManifest, builtManifest, logger);
            repMan.replace();
            repMan.validateUpdate();
            
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
    
    
    protected static LoggerInf setLogger(File source)
        throws Exception
    {
        String qualifier = "yyMMdd";
        Properties logprop = new Properties();
        logprop.setProperty("fileLogger.message.maximumLevel", "5");
        logprop.setProperty("fileLogger.error.maximumLevel", "10");
        logprop.setProperty("fileLogger.name", "ddCur");
        logprop.setProperty("fileLogger.qualifier", "yyMMdd");

        File log = new File(source, "logs");
        if (!log.exists()) log.mkdir();
        String logPath = log.getCanonicalPath() + '/';
        
        if (DEBUG) System.out.println(PropertiesUtil.dumpProperties("LOG", logprop)
            + "\nlogpath:" + logPath
        );
        LoggerInf logger = LoggerAbs.getTFileLogger(qualifier, log.getCanonicalPath() + '/', logprop);
        return logger;
    }
    
    public void setProcessList(String processList)
    { 
        this.processList = new File(source, processList);
    }

    public FixConfig getConfig() {
        return config;
    }
    
    protected void log(int lvl, String msg)
        throws TException
    {
        logger.logMessage(msg, lvl, true);
    }
}
