/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cdlib.mrt.dashdryad;
import java.io.IOException;
import org.cdlib.mrt.utility.TException;
/**
 * 
 *
 * @author replic
 */
public class MainProdTest 
{
    protected static final String NAME = "MainProdUpdate";
    protected static final String MESSAGE = NAME + ": ";
    protected static final boolean DEBUG = false;
    
    
    public static void main(String[] args) 
            throws IOException,TException 
    {
        String sourcePath = "/home/loy/MRTMaven/github/admin/mrt-store-admin/tasks/210420-current/prod";
        DDCurrent dd = new DDCurrent(sourcePath);
        dd.setProcessList("dd-test.txt");
        DDConfig config = dd.getConfig();
        System.out.println(config.dump(MESSAGE));
        dd.processList();
    }
}
