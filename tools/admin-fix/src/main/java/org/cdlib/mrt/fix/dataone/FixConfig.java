/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdlib.mrt.fix.dataone;
import org.cdlib.mrt.fix.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;
import org.cdlib.mrt.cloud.VersionMap;

import org.cdlib.mrt.core.Identifier;
import org.cdlib.mrt.fix.tools.NodeIOExt;
import org.cdlib.mrt.fix.tools.VersionMapTools;
import org.cdlib.mrt.utility.LoggerAbs;
import org.cdlib.mrt.utility.LoggerInf;
import org.cdlib.mrt.utility.StringUtil;
import org.cdlib.mrt.utility.TException;
import org.cdlib.mrt.tools.YamlParser;

import org.json.JSONObject;
import org.json.JSONArray;
import org.cdlib.mrt.s3.service.NodeIO;
/**
 * 
 *
 * @author replic
 */
public class FixConfig 
{
    protected static final String NAME = "FixConfig";
    protected static final String MESSAGE = NAME + ": ";
    protected static final boolean DEBUG = false;
    
    protected File manifestsPath = null;
    protected LoggerInf logger = null;
    protected File processList = null;
    protected Long node = null;
    protected File sourcePath = null;
    
    protected ArrayList <Long> fixNodes = null;
    protected String baseURI = null;
    //protected String supportURI = null;
    protected URL storeLink = null;
    protected NodeIOExt nIO = null;
    protected String nodeIOPath = null;
    //protected NodeIO.AccessNode archiveAccessNode = null;
    
    
    public FixConfig(File sourcePath)
        throws TException
    {
        this.sourcePath = sourcePath;
        this.manifestsPath = new File(this.sourcePath, "manifests");
    } 
    public static FixConfig useYaml(String pathS)
        throws TException
    {
        try {
            File sourcePath = new File(pathS);
            String propName = "config.yml";
            File propFile = new File(sourcePath, propName);
            
            InputStream propStream =  new FileInputStream(propFile);
            String propYaml = StringUtil.streamToString(propStream, "utf8");
            FixConfig fixConfig = new FixConfig(sourcePath);

            YamlParser yamlParser = new YamlParser();
            yamlParser.parseString(propYaml);
            yamlParser.resolveValues();
            String jsonS = yamlParser.dumpJson();
            
            if (false) System.out.append("jsonS:" + jsonS);
            JSONObject jS = new JSONObject(jsonS);
            JSONObject jobj = jS.getJSONObject("fix-info");
            System.out.println(jobj.toString(3));
            JSONObject jLogger = jobj.getJSONObject("fileLogger");
            LoggerInf logger = fixConfig.setLogger(jLogger);
            fixConfig.setLogger(logger);
            fixConfig.setBaseURI(jobj.getString("baseURI"));
            fixConfig.setProcessList(jobj.getString("processList"));
            fixConfig.setNode(jobj.getLong("node"));
            fixConfig.setNodeIOPath(jobj.getString("nodeIOPath"));
            fixConfig.setnIO();
            
            JSONArray jarr = jobj.getJSONArray("fixNodes");
            
            ArrayList <Long> nodes = new ArrayList();
            for (int i=0; i < jarr.length(); i++) {
                long node = jarr.getLong(i);
                nodes.add(node);
            }
            fixConfig.setFixNodes(nodes);
            //fixConfig.testFixNodes();
            
            return fixConfig;
            
        } catch (TException tex) {
            tex.printStackTrace();
            throw tex;
            
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TException(ex);
        }
        
    }
    public String getBaseURI() {
        return baseURI;
    }

    public void setBaseURI(String baseURI) {
        this.baseURI = baseURI;
    }

    public URL getManifestLink(long node, Identifier ark) {
        return storeLink;
    }
    
    public LoggerInf getLogger() {
        return logger;
    }

    public File getProcessList() {
        return processList;
    }

    public void setProcessList(String processListS) {
        this.processList = new File(sourcePath, processListS);
    }

    public File getSourcePath() {
        return sourcePath;
    }

    public File getManifestsPath() {
        return manifestsPath;
    }

    public void setSourcePath(File sourcePath) {
        this.sourcePath = sourcePath;
    }
    
    public String getManifestURL(long node, String ark) 
        throws Exception
    {
        String encArk = URLEncoder.encode(ark, "utf-8");
        String manU = baseURI + "/manifest/"  + node + "/" + encArk;
        return manU;
    }
    
    public VersionMap getVersionMap(long node, String ark)
        throws TException
    {;
        try {
            String encArk = URLEncoder.encode(ark, "utf-8");
            String manU = getManifestURL(node, ark);
            
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
            return VersionMapTools.getUrl(manifestXMLUrlS);
    }
    
    public void setStoreLink(URL storeLink) {
        this.storeLink = storeLink;
    }
    

    public void setLogger(LoggerInf logger) {
        this.logger = logger;
    }

    public Long getNode() {
        return node;
    }

    public void setNode(Long node) {
        this.node = node;
    }

    /**
     * set local logger to node/log/...
     * @param path String path to node
     * @return Node logger
     * @throws Exception process exception
     */
    protected LoggerInf setLogger(JSONObject fileLogger)
        throws Exception
    {
        String qualifier = fileLogger.getString("qualifier");
        Properties logprop = new Properties();
        logprop.setProperty("fileLogger.message.maximumLevel", "" + fileLogger.getInt("messageMaximumLevel"));
        logprop.setProperty("fileLogger.error.maximumLevel", "" + fileLogger.getInt("messageMaximumError"));
        logprop.setProperty("fileLogger.name", fileLogger.getString("name"));
        //logprop.setProperty("fileLogger.trace", "" + fileLogger.getInt("trace"));
        logprop.setProperty("fileLogger.qualifier", fileLogger.getString("qualifier"));
        if (sourcePath == null) {
            throw new TException.INVALID_OR_MISSING_PARM(
                    MESSAGE + "sourcePath not supplied");
        }

        File logsPath = new File(sourcePath, "logs");
        if (!logsPath.exists()) logsPath.mkdir();
        LoggerInf logger = LoggerAbs.getTFileLogger(qualifier, logsPath.getCanonicalPath() + '/', logprop);
        return logger;
    }
    
    
    public String dump(String header)
        throws TException
    {
        try {
        StringBuffer buf = new StringBuffer("fixNodes\n");
        
        for (long fixNode : getFixNodes()) {
            buf.append(" - " + fixNode + "\n");
        }
        
        String retString = header  + "\n"
                + " - sourcePath=" + getSourcePath().getCanonicalPath()  + "\n"
                + " - manifestsPath=" + getManifestsPath().getCanonicalPath()  + "\n"
                + " - processList=" + getProcessList().getCanonicalPath()  + "\n"
                + " - node=" + getNode()  + "\n"
                + buf.toString() + "\n"
                ;
        
        return retString;
        } catch (Exception ex) {
            throw new TException(ex);
        }
    }

    public ArrayList<Long> getFixNodes() {
        return fixNodes;
    }

    public void setFixNodes(ArrayList<Long> fixNodes) {
        this.fixNodes = fixNodes;
    }

    public String getNodeIOPath() {
        return nodeIOPath;
    }

    public void setNodeIOPath(String nodeIOPath) {
        this.nodeIOPath = nodeIOPath;
    }

    public void setnIO() 
        throws TException
    {
        this.nIO = new NodeIOExt(nodeIOPath, logger);
    }

    public NodeIO.AccessNode getAccessNode(long node) 
    {
        return nIO.getAccessNode(node);
    }
    
    public void testFixNodes()
        throws TException
    {
        try {
            for (long node : fixNodes) {
                NodeIO.AccessNode accessNode = nIO.getAccessNode(node);
                if (accessNode == null) {
                    System.out.println("Node:" + node + " - not found");
                } else  {
                    System.out.println("Node:" + node + " - found");
                }
            }
        }  catch (Exception ex) {
            System.out.println("TException:" + ex);
            ex.printStackTrace();
            throw new TException(ex);
        }
    }

    
    public static void main(String[] argv) {
    	
    	try {
            String sourcePath = "/home/loy/MRTMaven/github/admin/mrt-store-admin/tasks/210420-current/fix";
            FixConfig fixConfig = FixConfig.useYaml(sourcePath);
            System.out.println(fixConfig.dump("test"));
            fixConfig.testFixNodes();
            
        } catch (Exception ex) {
                // TODO Auto-generated catch block
                System.out.println("Exception:" + ex);
                ex.printStackTrace();
        }
    }
}
