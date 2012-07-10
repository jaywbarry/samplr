package com.logicstyle.samplr;

import java.io.File;
import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.openide.util.Exceptions;



/**
 * Unit test for simple App.
 */


public class RequestManagerTest 
{


    
    
    class TestProcessingThread extends Thread {
        private final int maxIterations;
        private boolean keepRunning=true;

        public TestProcessingThread(int maxIterations) {
            super("Test Request Processing Thread");
            this.maxIterations=maxIterations;
        }

        @Override
        public void run() {
            
            Random random=new Random();
            
            for(int i=0;i<maxIterations && keepRunning;i++) {
                int randomInt=random.nextInt(10);
                
                switch (randomInt) {
                    case 0:
                    case 1:
                    case 2:
                        methodA();
                        break;
                    case 3:
                    case 4:
                    case 5:
                    case 6:
                        methodB();
                        break;
                    default:
                        methodC(100);
                        
                        
                }
            }
            
        }
        
        public void stopRunning() {
            keepRunning=false;
        }
        
        
        private void methodA() {
            
            Random random=new Random();
            for(int i=0;i<50000;i++) {
                System.err.println(random.nextInt());
            }
            
        }
        
        private void methodB() {
            
            Random random=new Random();
            int randomInt=random.nextInt();
            
            for(int j=1;j<randomInt/2;j++) {
                if(randomInt%j==0) {
                    System.err.println("Not prime!");
                    return;
                }
            }
            
            System.err.println("Prime!!!!!");
            
            
        }
        
        private void methodC(int maxDepth) {
            
            if(maxDepth==0) {
                System.err.println("Max depth reached!");
                return;
            }
            try {
                sleep(new Random().nextInt(300));
            } catch (InterruptedException ex) {
               
            }
            methodC(maxDepth-1);
           
        }
        
        
    }
    
    
    @BeforeClass
    public static void initOutputDirectory() {
        
         File testOutputDir=new File("target/test-output");
        if(testOutputDir.exists())
           deleteDir(testOutputDir);
        
        testOutputDir.mkdir();
            
    }
    
    public RequestManager  initRequestManager(long requestTimeout) {
        
        
        
      
       
        RequestManager requestManager=new RequestManager()
                .withRequestProcessor(new ThreadSamplingRequestProcessor()
                                           .withRequestLengthSamplingThreshold(5000))
                .withResultsProcessor(new FileResultsArchiver()
                                          .withOutputDirectory(new File("target/test-output")))
                .withRequestTimeout(requestTimeout);
                
           
        requestManager.start();
        return requestManager;
    }
   
    private static boolean deleteDir(File dir) {
        
        if(dir.isDirectory()) {
            String[] children=dir.list();
            for(int i=0;i<children.length;i++) {
                if(!children[i].equals(".") && !children[i].equals("..")) {
                    boolean success=deleteDir(new File(dir,children[i]));
                    if(!success)
                        return false;
                }
            }
            
        }
        
        return dir.delete();
        
        
    }
    @Test
    public void testRequestManager()
    {
        RequestManager requestManager=initRequestManager(0);
        
        TestProcessingThread testThread=new TestProcessingThread(5);
        
        Request testRequest=new Request();
        testRequest.setThreadId(testThread.getId());
        
        testThread.start();
        
        requestManager.requestStarting(testRequest);
        try {
            testThread.join();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        
        requestManager.requestFinished(testRequest);
        requestManager.shutdown();
        requestManager.awaitTermination(30000);
        File resultsFile=new File("target/test-output/"+testRequest.getId()+"/request-sampling.nps");
        assertTrue(resultsFile.exists());
        
        
        
    }
    
    @Test
    public void testRequestTimeout() {
        
        RequestManager requestManager=initRequestManager(15000);
        
        TestProcessingThread testThread=new TestProcessingThread(5000); // no way this will end in 15 seconds
        
        Request testRequest=new Request();
        testRequest.setThreadId(testThread.getId());
        
        testThread.start();
        
        requestManager.requestStarting(testRequest);
        try {
            Thread.sleep(15000);
        } catch (InterruptedException ex) {
           
        }
        
        requestManager.shutdown();
        RequestManager.Status status=requestManager.awaitTermination(30000);
        
        testThread.stopRunning();
        
        assertEquals(RequestManager.Status.STOPPED,status);
        
        
        File resultsFile=new File("target/test-output/"+testRequest.getId()+"/request-sampling.nps");
        assertTrue(resultsFile.exists());
        
        
    }
}
