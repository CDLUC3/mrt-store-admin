/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdlib.mrt.fix.tools;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Properties;
import org.cdlib.mrt.cloud.VersionMap;

import org.cdlib.mrt.core.Identifier;
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
public class NodeIOExt 
{
    protected static final String NAME = "NodeIOExt";
    protected static final String MESSAGE = NAME + ": ";
    protected static final boolean DEBUG = false;
    protected NodeIO nodeIO = null;
    protected String nodeIOPath = null;
    protected LoggerInf logger = null;
    
    public NodeIOExt(String nodeIOPath, LoggerInf logger)
        throws TException
    {
        this.nodeIOPath = nodeIOPath;
        this.logger = logger;
        setNodeIO();
    } 
    
    protected void setNodeIO()
        throws TException
    {
            try {
            if (nodeIOPath == null) {
                throw new TException.INVALID_OR_MISSING_PARM(MESSAGE + "nodeIOPath missing");
            }
                nodeIO = NodeIO.getNodeIOConfig(nodeIOPath, logger);
                setNodeIO(nodeIO);
            
        } catch (TException tex) {
            throw tex;
            
        } catch (Exception ex) {
            throw new TException(ex);
        }
    }

    public NodeIO getNodeIO() {
        return nodeIO;
    }

    protected void setNodeIO(NodeIO nodeIO)
        throws TException
    {
        if (nodeIO == null) {
            throw new TException.INVALID_OR_MISSING_PARM(MESSAGE + "NodeIO required");
        }
        this.nodeIO = nodeIO;
    }
    

    public NodeIO.AccessNode getAccessNode(long node) 
    {
        return nodeIO.getAccessNode(node);
    }

    public String getNodeName() {
        if (nodeIO == null) return null;
        return nodeIO.getNodeName();
    }
}
