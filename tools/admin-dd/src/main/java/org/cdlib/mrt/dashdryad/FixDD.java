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
import org.cdlib.mrt.cloud.MatchMap;
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
import org.cdlib.mrt.s3.service.NodeIO;
/**
 * 
 *
 * @author replic
 */
public class FixDD 
{
    protected static final String NAME = "FixDD";
    protected static final String MESSAGE = NAME + ": ";
    protected static final boolean DEBUG = false;
    protected static final String yamlName="jar:nodes-wasabi";
    protected Long node = null;
    protected Identifier ark = null;
    protected LoggerInf logger = null;
    protected File currentManifest = null;
    protected File builtManifest = null;
    protected File existingManifest = null;
    protected CloudStoreInf service = null;
    protected String bucket = null;
    protected NodeIO.AccessNode nodeAccess =  null;
    
    public FixDD(Long node, Identifier ark, File currentManifest, File builtManifest, LoggerInf logger)
        throws TException
    {
        this.node = node;
        this.ark = ark;
        this.builtManifest = builtManifest;
        this.currentManifest = currentManifest;
        this.logger = logger;
        try {
            
            NodeIO nodeIO = NodeIO.getNodeIOConfig(yamlName, logger);
            this.nodeAccess = nodeIO.getAccessNode(node);
            service = nodeAccess.service;
            bucket = nodeAccess.container;
            
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
    
    public FixDD(NodeIO.AccessNode nodeAccess, Identifier ark, File currentManifest, File builtManifest, LoggerInf logger)
        throws TException
    {
        this.node = node;
        this.ark = ark;
        this.builtManifest = builtManifest;
        this.currentManifest = currentManifest;
        this.logger = logger;
        this.nodeAccess = nodeAccess;
        this.service = nodeAccess.service;
        this.bucket = nodeAccess.container;
    }
    
    public static void main(String[] args) 
            throws IOException,TException 
    {
        String path = "/home/loy/MRTMaven/github/admin/mrt-store-admin/tasks/210420-current/test/manifests/D10D4Q";
        File source = new File(path);
        File oldMan = new File(source, "old.xml");
        File currentMan = new File(source, "current.xml");
        File builtMan = new File(source, "build.xml");
        long node = 2003L;
        Identifier ark = new Identifier("ark:/13030/m5hj2651");
        
        LoggerInf logger = new TFileLogger(NAME, 50, 50);
        FixDD fixDD = new FixDD(node, ark, currentMan, builtMan, logger);
        fixDD.replace();
        fixDD.validateUpdate();
    }
    
    
    protected void replace()
            throws TException 
    {
        try {
            log(0, ">>>FixDD replace: start:" + ark.getValue());
            log(0, " - currentMan=" + currentManifest.getCanonicalPath() + "\n");
            log(0, " - buildMan=" + builtManifest.getCanonicalPath() + "\n");
            existingManifest = FileUtil.getTempFile("work", ".xml");
            getExistingManifest(existingManifest);
            if (isBuildManifest()) {
                log(0, "<<<FixDD Already converted - return:" + ark.getValue());
                return;
            }
            saveBackup();
            replaceManifest();
            validateUpdate();
            log(0, "<<<FixDD replace: end:" + ark.getValue());
     
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            throw tex;
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            throw new TException(ex);
            
        } finally {
            try {
                existingManifest.delete();
            } catch (Exception ex) { }
        }
    }
    protected void test()
            throws TException 
    {
        try {
            log(0, "***test:"
                    + " - currentMan=" + currentManifest.getCanonicalPath() + "\n"
                    + " - buildMan=" + builtManifest.getCanonicalPath() + "\n"
                    + " - node=" + node  + "\n"
                    + " - ark=" + ark.getValue()  + "\n"
                    + " - bucket=" + bucket  + "\n"
                    + " - nodeDescription=" + nodeAccess.nodeDescription  + "\n"
            );
            existingManifest = FileUtil.getTempFile("work", ".xml");
            getExistingManifest(existingManifest);
            if (isBuildManifest()) {
                System.out.println("Is buildManifest - return:" + ark.getValue());
                return;
            }
            saveBackupTest();
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            throw tex;
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            throw new TException(ex);
            
        } finally {
            try {
                existingManifest.delete();
            } catch (Exception ex) { }
        }
    }
    
    protected boolean match(File fromManifest, File toManifest)
        throws TException
    {
        return match(fromManifest, toManifest, false);
    }
    
    protected boolean match(File fromManifest, File toManifest, boolean debug)
        throws TException
    {
        if (debug) System.out.println("***match");
        VersionMap fromMap = getVersionMap(fromManifest);
        VersionMap toMap = getVersionMap(toManifest);
        MatchMap match = new MatchMap(fromMap, toMap, logger);
        match.compare();
        if (debug) System.out.println(match.dump("FixDD"));
        if (debug) System.out.println("isDifferent:" + match.isDifferent());
        return match.isDifferent();
    }
    
    
    protected void getExistingManifest(File existingManifest)
        throws TException
    {
        InputStream manifestStream = null;
        try {
            if (existingManifest == null)  {
                throw new TException.INVALID_OR_MISSING_PARM("getExistingManifest existing Manifest file required");
            }
            CloudResponse response = CloudResponse.get(bucket, ark);
            manifestStream = service.getManifest(bucket, ark, response);
            Exception ex = response.getException();
            if (ex != null) {
                if (ex instanceof TException) {
                    throw (TException) ex;
                } else {
                    throw new TException(ex);
                }
            }
            if (manifestStream == null) {
                throw new TException.REQUESTED_ITEM_NOT_FOUND("unable to locate manifest:" + ark.getValue());
            }
            FileUtil.stream2File(manifestStream, existingManifest);
     
            
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
    
    protected void saveBackupTest()
        throws TException
    {
        try {
            if ((existingManifest == null) || !existingManifest.exists()) {
                throw new TException.INVALID_OR_MISSING_PARM("getExistingManifest existing Manifest file required");
            }
            String keySave = ark.getValue() + "|manifest.save";
            log(0, "***saveBackupTest:"
                    + " - key=" + keySave+ "\n"
                 //   + " - existingManifestS=" + existingManifestS + "\n"
            );
            boolean doNotMatch = match(existingManifest, currentManifest);
            if (doNotMatch) {
                throw new TException.INVALID_OR_MISSING_PARM("getExistingManifest existing Manifest does not match current");
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
    
    
    
    protected boolean isBuildManifest()
        throws TException
    {
        try {
            if ((existingManifest == null) || !existingManifest.exists()) {
                throw new TException.INVALID_OR_MISSING_PARM("getExistingManifest existing Manifest file required");
            }
            boolean isDifferent =  match(existingManifest, builtManifest);
            return !isDifferent;
     
            
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
    
    protected void saveBackup()
        throws TException
    {
        try {
            if ((existingManifest == null) || !existingManifest.exists()) {
                throw new TException.INVALID_OR_MISSING_PARM("getExistingManifest existing Manifest file required");
            }
            boolean doNotMatch = match(existingManifest, currentManifest);
            if (doNotMatch) {
                throw new TException.INVALID_OR_MISSING_PARM("saveBackup existing Manifest does not match current");
            }
            String keySave = ark.getValue() + "|manifest.save";
            CloudResponse response = service.putObject(bucket, keySave, existingManifest);
            if (response == null) {
                throw new TException.GENERAL_EXCEPTION("saveBackup - response not found");
            }
            Exception ex = response.getException();
            if (ex != null) {
                if (ex instanceof TException) {
                    throw (TException) ex;
                } else {
                    throw new TException(ex);
                }
            }
            log(0,"saveBackup complete:" + ark.getValue());
     
            
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
    
    protected void replaceManifest()
        throws TException
    {
        try {
            CloudResponse response = service.deleteManifest(bucket, ark);
            if (response == null) {
                throw new TException.GENERAL_EXCEPTION("replaceManifest delete - response not found");
            }
            Exception ex = response.getException();
            if (ex != null) {
                if (ex instanceof TException) {
                    throw (TException) ex;
                } else {
                    throw new TException(ex);
                }
            }
            response = service.putManifest(bucket, ark, builtManifest);
            if (response == null) {
                throw new TException.GENERAL_EXCEPTION("replaceManifest put - response not found");
            }
            if (ex != null) {
                if (ex instanceof TException) {
                    throw (TException) ex;
                } else {
                    throw new TException(ex);
                }
            }
     
            log(0,"replaceManifest complete:" + ark.getValue());
            
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
    
    protected void validateUpdate()
        throws TException
    {
        try {
            String keySave = ark.getValue() + "|manifest.save";
            // OK String keySave = ark.getValue() + "|manifest.save";
            // OK boolean matchCurrent = testUpdate("Match currentManifest - manifest.save", currentManifest, keySave);
            boolean matchCurrent = testUpdate("Match currentManifest - manifest.save", currentManifest, keySave);
            
            if (!matchCurrent) {
                throw new TException.INVALID_DATA_FORMAT("validateUpdate: fail save match"
                        + " - key=" + keySave
                );
            }
            String keyMan = ark.getValue() + "|manifest";
            boolean matchBuilt = testUpdate("Match buildManifest - manifest", builtManifest, keyMan);
            
            if (!matchBuilt) {
                throw new TException.INVALID_DATA_FORMAT("validateUpdate: fail replace match"
                        + " - key=" + keyMan
                );
            }
     
            log(0,"validateUpdate complete:" + ark.getValue());
     
            
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
    
    protected boolean testUpdate(String hdr, File test, String key)
        throws TException
    {
        File keyTmp = FileUtil.getTempFile("tst", ".txt");
        try {
            CloudResponse response = new CloudResponse(bucket, key);
            service.getObject(bucket, key, keyTmp, response);
            if (response == null) {
                throw new TException.GENERAL_EXCEPTION("replaceManifest delete - response not found");
            }
            Exception ex = response.getException();
            if (ex != null) {
                if (ex instanceof TException) {
                    throw (TException) ex;
                } else {
                    throw new TException(ex);
                }
            }
            boolean diff = match(test, keyTmp);
            log(0, "validate - " + hdr + ":" 
                    + " - key=" + key
                    + " - match=" + !diff);
            return !diff;
     
            
        } catch (TException tex) {
            System.out.println("TException:" + tex);
            tex.printStackTrace();
            throw tex;
            
        } catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            throw new TException(ex);
            
        } finally {
            try {
                keyTmp.delete();
                
            } catch (Exception e) { }
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
    
    protected void log(int lvl, String msg)
        throws TException
    {
        logger.logMessage(msg, lvl, true);
    }
    
}
