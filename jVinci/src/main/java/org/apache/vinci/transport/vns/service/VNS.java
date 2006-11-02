/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.vinci.transport.vns.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.apache.vinci.debug.Debug;
import org.apache.vinci.transport.BaseClient;
import org.apache.vinci.transport.BaseServer;
import org.apache.vinci.transport.BaseServerRunnable;
import org.apache.vinci.transport.ErrorFrame;
import org.apache.vinci.transport.Frame;
import org.apache.vinci.transport.KeyValuePair;
import org.apache.vinci.transport.ServiceException;
import org.apache.vinci.transport.Transportable;
import org.apache.vinci.transport.VinciFrame;
import org.apache.vinci.transport.VinciServableAdapter;
import org.apache.vinci.transport.vns.VNSConstants;

/**
 * VNS (Vinci Naming Service) provides the "directory" of all available Vinci services. VNS must
 * be running somewhere on the network for VinciClient and VinciServer classes to function. These
 * classes consult org.apache.vinci.transport.vns.client.VNSConfig to determine the location of the
 * VNS service.
 */
public class VNS extends VinciServableAdapter {

  private HashMap            cachedResults       = new HashMap();

  public final static String dirCmdAddService    = "addservice";
  public final static String dirCmdAddAlias      = "addalias";
  public final static String dirCmdDelService    = "delservice";
  public final static String dirCmdDelAlias      = "delalias";
  public final static String dirCmdUpdateService = "updateservice";
  public final static String dirCmdGetList       = "getlist";
  public final static String dirCmdGetNames      = "getnames";
  public final static String dirCmdGetHits       = "gethits";

  public String              ENV_PROXY           = "vinci.environment.proxy";

  // Static variables set up during the start of the program
  private static String      configFile, backupFile, workspacesFile, counterFile;
  private static String      logFile             = null;
  private static String      configDir           = null;
  private static int         backupInterval      = 120;
  private static int         srvPort             = 9000;
  private static boolean     logFlag             = false;

  private static String      bindAddress         = null;
  private static int         backlog             = 100;
  private static int         maxThreads          = 200;

  /* Instance variables */
  int                        port;
  String                     myLogFile           = null;
  Thread                     backupThread;
  BackupThread               backupThreadRunnable;
  Hashtable                  hits;                                               // for keeping track of the counters
  int                        totalhits;
  ServiceRegistry            SR;
  WorkspaceConfig            WS;
  Writer                     log;                                                // for writing to the logfile
  String                     starttime;
  static File                quitFile;

  static private void setConfigDir(String path) {
    if (path.charAt(path.length() - 1) == File.separatorChar) {
      path = path.substring(0, path.length() - 1);
    }
    configDir = path;
    configFile = path + File.separatorChar + "vns.services";
    backupFile = path + File.separatorChar + "vns.services.bak";
    workspacesFile = path + File.separatorChar + "vns.workspaces";
    counterFile = path + File.separatorChar + "vns.counter";
    quitFile = new File(configDir + File.separatorChar + "quit");
    quitFile.delete();
  }

  public static void main(String[] args) throws IOException {
    Debug.setThreadNameOutput(true);
    System.setProperty("sun.net.inetaddr.ttl", "30");
    // ^^ JDK1.3
    java.security.Security.setProperty("networkaddress.cache.ttl", "30");
    // ^^ JDK1.4
    // ^^ REQUIRED to keep Java from caching "null" IP addresses for domain names
    setConfigDir(".");
    // Parse the commandline options
    String s;
    for (int i = 0; i < args.length; i++) {
      s = args[i].trim();
      if (s.equals("-h") || s.equals("--help")) {
        printUsage();
        return;
      }
      if (s.equals("-l") || s.equals("--logfile")) {
        logFile = args[++i];
        logFlag = true;
        Debug.p("Log file set to: " + logFile);
        continue;
      }
      if (s.equals("-p") || s.equals("--port")) {
        srvPort = Integer.parseInt(args[++i]);
        Debug.p("Service port set to: " + srvPort);
        continue;
      }
      if (s.equals("-b") || s.equals("--bind")) {
        bindAddress = args[++i];
        Debug.p("Bind address set to: " + bindAddress);
        continue;
      }
      if (s.equals("-g") || s.equals("--backlog")) {
        backlog = Integer.parseInt(args[++i]);
        Debug.p("Backlog set to: " + backlog);
        continue;
      }
      if (s.equals("-m") || s.equals("--maxthreads")) {
        maxThreads = Integer.parseInt(args[++i]);
        Debug.p("Max threads set to: " + maxThreads);
        continue;
      }
      if (s.equals("-i") || s.equals("--interval")) {
        backupInterval = Integer.parseInt(args[++i]);
        Debug.p("Backup interval set to: " + backupInterval);
        continue;
      }
      if (s.equals("-a") || s.equals("--logging")) {
        Debug.p("Access logging enabled.");
        logFlag = true;
        continue;
      }
      if (s.equals("-c") || s.equals("--config")) {
        String dir = args[++i];
        Debug.p("Config directory set to: " + dir);
        setConfigDir(dir);
        continue;
      }

      System.out.println("Unrecognized option : " + args[i]);
      printUsage();
      return;
    }

    // Start the server
    startServing();
  }

  private static void printUsage() {
    System.out.println("Usage: java org.apache.vinci.transport.vns.service.VNS [options]");
    System.out.println("  -p N  --port N");
    System.out.println("    Serve on port number N [default=" + srvPort + "]");

    System.out.println("  -b ip_address  --bind ip_address");
    System.out.println("    Force socket server to bind only to ip_address [default is bind to all]");

    System.out.println("  -c dirname --config dirname");
    System.out.println("    Look for & write config files in this directory [default=.]");

    System.out.println("  -i N  --interval N");
    System.out.println("    Backup every N seconds [default=" + backupInterval + "]");

    System.out.println("  -l fname  --logfile fname");
    System.out.println("    Access log pathname [default=<configdir>/vns.log]");

    System.out.println("  -a  --logging");
    System.out.println("    Enable access logging (implied by -l/--logfile option)");

    System.out.println("  -g N  --backlog N");
    System.out.println("    Set the ServerSocket backlog [default=" + backlog + "]");

    System.out.println("  -m N  --maxthreads N");
    System.out.println("    Set the maximum size of the VNS thread pool [default=" + maxThreads + "].");

    System.out.println("  -h  --help");
    System.out.println("    This help message");

    System.exit(0);
  }

  /* Main run method to initialize the server and start running it */
  public static void startServing() throws IOException {
    VNS vns;
    if (logFlag) {
      if (logFile == null) {
        logFile = configDir + File.separatorChar + "vns.log";
      }
      vns = new VNS(srvPort, logFile);
    } else {
      vns = new VNS(srvPort);
    }

    // Load the settings files
    if (!vns.loadConfig(configFile)) {
      System.err.println("failed to load config file: " + configFile);
      System.err.println("Check its path and validity. If necessary, restore from backup.");
      System.exit(1);
    }

    vns.loadWorkspaces(workspacesFile);
    Debug.p("VNS Workspace : " + vns.WS.workspace);

    vns.loadCounters(counterFile);

    // Configure and start the backup thread
    Debug.p("Starting backup thread, using files " + backupFile + " and " + configFile);
    vns.backupThreadRunnable = new BackupThread(vns, backupFile, configFile, backupInterval, counterFile);

    vns.backupThread = new Thread(vns.backupThreadRunnable);
    vns.backupThread.start();

    // Start accepting connections and processing stuff
    Debug.p("Serving on port : " + vns.port);
    BaseServer server = null;
    if (bindAddress != null) {
      server = new BaseServer(vns) {
        protected ServerSocket createServerSocket(int port) throws IOException {
          return new ServerSocket(port, backlog, InetAddress.getByName(bindAddress));
        }
      };
    } else {
      server = new BaseServer(vns) {
        protected ServerSocket createServerSocket(int port) throws IOException {
          return new ServerSocket(port, backlog);
        }
      };
    }
    server.setSocketTimeout(5000);
    exitThread exitT = new exitThread(server);
    exitT.start();
    try {
      server.setThreadPoolSize(5, maxThreads);
      server.serve(vns.port);
    } catch (Throwable e) {
      Debug.reportException(e);
    } finally {
      Debug.p("Exitting.");
      System.exit(0);
    }
  }

  /* Constructors */
  public VNS() {
    SR = new ServiceRegistry();
    WS = new WorkspaceConfig(this);
    hits = new Hashtable();
    totalhits = 0;
    starttime = (new Date()).toString();
  }

  public VNS(int port) {
    this();
    this.port = port;
  }

  public VNS(int port, String logFile) throws IOException {
    this();
    this.port = port;
    this.myLogFile = new String(logFile);
    this.log = new BufferedWriter(new FileWriter(myLogFile, true));
  }

  /* Methods to load data from various files */
  public boolean loadConfig(String cFile) {
    File f = new File(cFile);
    if (!f.exists()) {
      Debug.p("WARNING: Config file doesn't exist, creating a new empty config file!");
      try {
        FileWriter writer = new FileWriter(f);
        writer.write("<CONFIGURATION></CONFIGURATION>");
        writer.close();
      } catch (IOException e) {
        Debug.reportException(e);
        return false;
      }
    }

    Debug.p("Loading config file : " + cFile);

    synchronized (SR) {
      try {
        SR.load(cFile);
        return true;
      } catch (Exception e) {
        Debug.reportException(e);
      }
    }
    return false;
  }

  public void loadWorkspaces(String wFile) {
    Debug.p("Loading workspaces file : " + wFile);

    FileReader F = null;
    try {
      F = new FileReader(wFile);
      WS.load(F);
      F.close();
    } catch (Exception e) {
      Debug.reportException(e);
      Debug.p("WARNING: failed to load workspace.");
    } finally {
      try {
        F.close();
      } catch (Exception e) {
      }
    }

  }

  public void loadCounters(String cFile) {
    Debug.p("Loading counter file : " + cFile);

    FileReader F;
    BufferedReader br;
    int line = 1;

    try {
      F = new FileReader(cFile);
    } catch (FileNotFoundException e) {
      Debug.p("Could not load the counter file : " + cFile);
      return;
    }
    br = new BufferedReader(F);

    try {
      String s = br.readLine();
      if (s == null || s.trim().equals("")) {
        Debug.p("Invalid counter file format at line 1: " + cFile);
        br.close();
        F.close();
        return;
      }

      StringTokenizer str = new StringTokenizer(s);
      if (!(str.countTokens() > 1)) {
        Debug.p("Invalid counter file format at line 1: " + cFile);
        br.close();
        F.close();
        return;
      }

      if (!str.nextToken().equals("TOTAL")) {
        throw new Exception("First line invalid - does not start with TOTAL");
      }

      totalhits = Integer.parseInt(str.nextToken());
      hits = new Hashtable();
      line++;

      while ((s = br.readLine()) != null) {
        str = new StringTokenizer(s);
        hits.put(str.nextToken(), new Integer(str.nextToken()));
        line++;
      }

      br.close();
      F.close();
    } catch (IOException e2) {
      Debug.p("IO Problem while reading from counter file : " + cFile);
      Debug.p("Exception was : " + e2);
    } catch (NumberFormatException e3) {
      Debug.p("Invalid number specified in counter file " + cFile + " at line " + line);
    } catch (Exception e4) {
      Debug.p("Problem while parsing counter file : " + cFile);
      Debug.p("Exception generated : " + e4);
    } finally {
      try {
        br.close();
      } catch (IOException e5) {
      }
      try {
        F.close();
      } catch (IOException e6) {
      }
    }
  }

  /* Method to save data to various files */
  public void saveConfig(String cFile) {
    Debug.p("Saving to config file : " + cFile);
    long startTime = System.currentTimeMillis();
    synchronized (SR) {
      Writer F = null;
      try {
        F = new BufferedWriter(new FileWriter(cFile + ".rename"));
        try {
          SR.save(F);
        } finally {
          F.close();
        }
        File real_file = new File(cFile);
        real_file.delete();
        if (!new File(cFile + ".rename").renameTo(real_file)) {
          throw new IOException("FAILED to rename services config file!!!");
        }
      } catch (IOException e) {
        Debug.reportException(e);
        Debug.p("Could not save config file : " + cFile);
      }
    }
    startTime = System.currentTimeMillis() - startTime;
    Debug.p("Config save required " + startTime + " millis.");
    // flush the log too if it needs it...
    if (log != null) {
      synchronized (log) {
        try {
          log.flush();
        } catch (IOException e) {
          Debug.reportException(e);
        }
      }
    }
  }

  public void saveCounters(String cFile) {
    Debug.p("Saving counter file : " + cFile);

    FileWriter F;

    try {
      F = new FileWriter(cFile);
    } catch (IOException e) {
      Debug.p("Could not save the counter file : " + cFile);
      Debug.p("Exception generated : " + e);
      return;
    }

    try {
      F.write("TOTAL " + totalhits + "\n");
      if (hits == null) {
        F.close();
        return;
      }
      Enumeration keys = hits.keys();
      Integer value;
      String key;
      while (keys.hasMoreElements()) {
        key = (String) keys.nextElement();
        value = (Integer) hits.get(key);
        F.write(key.trim() + " " + value + "\n");
      }
    } catch (IOException e2) {
      Debug.reportException(e2);
      Debug.p("IO Problem while writing to counter file : " + cFile);
    } finally {
      try {
        if (F != null) {
          F.close();
        }
      } catch (IOException e3) {
        Debug.reportException(e3);
      }
    }
  }

  public void saveWorkspaces(String wFile) {
    Debug.p("Loading workspaces file : " + wFile);

    Writer F = null;
    try {
      F = new BufferedWriter(new FileWriter(wFile));
      WS.save(F);
      F.close();
    } catch (IOException e) {
      Debug.p("Could not save workspaces file : " + wFile);
      Debug.p("Exception generated : " + e);
    } catch (RuntimeException e2) {
      Debug.p("Problem while saving workspace file : " + wFile);
      Debug.p("Exception generated: " + e2);
    } finally {
      try {
        if (F != null) {
          F.close();
        }
      } catch (IOException e) {
        Debug.reportException(e);
      }
    }

  }

  /* Main processing routine */
  public Transportable eval(Transportable inp) throws ServiceException {
    VinciFrame in = (VinciFrame) inp;
    VinciFrame out = null;

    Debug.p("Request from " + in.fgetString("vinci:REMOTEIP"));

    String command;
    command = in.fgetString("vinci:COMMAND");
    if (command == null) {
      out = new VinciFrame();
      out.fadd("vinci:ERROR", "Malformed request");
      return out;
    }

    try {
      if (VNSConstants.RESOLVE_COMMAND.equals(command)) {
        out = resolve(in);
      } else if (VNSConstants.SERVEON_COMMAND.equals(command)) {
        out = serveon(in);
      } else if (command.equals(dirCmdAddService))
        out = addService(in);
      else if (command.equals(dirCmdAddAlias))
        out = addAlias(in);
      else if (command.equals(dirCmdUpdateService))
        out = updateService(in);
      else if (command.equals(dirCmdDelService))
        out = delService(in);
      else if (command.equals(dirCmdDelAlias))
        out = delAlias(in);
      else if (command.equals(dirCmdGetList))
        out = getList(in);
      else if (command.equals(dirCmdGetNames))
        out = getNames(in);
      else if (command.equals(dirCmdGetHits))
        out = getHits(in);
      else {
        out = new VinciFrame();
        out.fadd("vinci:ERROR", "Unrecognized command");
      }
    } catch (Exception e) {
      // Exact translation of the Python code [may not be necessary]
      out = new VinciFrame();
      out.fadd("vinci:ERROR", "Critical error :\n" + e
          + "\nThe server MAY not respond to further requests.\nPlease notify the administrator\n");
      e.printStackTrace();
    }

    return out;
  }

  /* Various processing routines */
  VinciFrame resolveLocal(VinciFrame in) {

    Debug.p("Local resolve");

    VinciFrame out = new VinciFrame();

    String name = in.fgetString("SERVICE");
    if (strip(name) == null)
      return new ErrorFrame("No service name specified");

    String host = strip(in.fgetString("HOST"));
    String realhost = strip(in.fgetString("IP"));
    if (strip(realhost) == null && strip(host) != null) {
      try {
        // Try to resolve the IP if possible
        realhost = (InetAddress.getByName(host)).getHostAddress();
      } catch (Exception e) {
        //realhost = null;

        //return error message
        return new ErrorFrame("Could not resolve IP address for service " + name);
      }
    }

    String instance = in.fgetString("INSTANCE");
    int inst = -1;
    try {
      inst = Integer.parseInt(instance);
    } catch (Exception e) {
      inst = -1;
    }

    String level = in.fgetString("LEVEL");

    // Default as specified in SPEC.txt
    if (strip(level) == null || level.trim().toLowerCase().equals("none"))
      level = "-1";
    else if (level.toLowerCase().equals("all")) {
      Debug.p("Specific level must be given (not all)");
      out.fadd("vinci:ERROR", "Specific level must be given (not all)");
      return out;
    }

    synchronized (SR) {
      // Construct a valid number from the level string specified
      level = "" + SR.getLevel(name, level);
    }
    Service[] servicesList;

    // Check the cache for matches
    CachedItem citem = (CachedItem) checkCache(name + "[" + level + "]");
    if (citem == null) {
      synchronized (SR) {
        // Find all services that match the specified name and are <= level specified
        // Also, resolve all aliases to actual services
        servicesList = SR.getServices(name, level, true);
      }
      // Cache the result (if any matches found)
      if (servicesList != null && servicesList.length > 0) {
        // Cache the resolve result under the actual name and level returned
        cache(name + "[" + servicesList[0].level + "]", new CachedItem(servicesList));
        // Have a proxy pointer to the entry created if it is not the same
        if (!servicesList[0].level.equals(level)) {
          cache(name + "[" + level + "]", new ProxyCachedItem(name + "[" + servicesList[0].level + "]"));
        }
      }
    } else
      servicesList = citem.servicesList;

    if (servicesList == null) {
      Debug.p("Service " + name + " not found");
      out.fadd("vinci:ERROR", "Service " + name + " not found");
      return out;
    }

    Debug.p("Number of services found with name = " + name + ", and level = " + level + " : " + servicesList.length);

    if (servicesList.length == 0)
      System.err.println("NO SERVICES FOUND WITH REALHOST = " + realhost + " name = " + name);

    ArrayList v = new ArrayList();

    // Filter the services with matching realhost and instance
    for (int i = 0; i < servicesList.length; i++) {
      if (realhost != null && !servicesList[i].realhost.equals(realhost))
        continue;
      if (inst > -1 && inst != servicesList[i].instance)
        continue;
      v.add(servicesList[i]);
    }

    servicesList = new Service[v.size()];

    for (int i = 0; i < v.size(); i++)
      servicesList[i] = (Service) v.get(i);

    if (servicesList == null || servicesList.length == 0) {
      Debug.p("Service " + name + " not found");
      out.fadd("vinci:ERROR", "Service " + name + " not found");
      return out;
    }

    VinciFrame temp;
    for (int j = 0; j < servicesList.length; j++) {
      temp = new VinciFrame();

      temp.fadd("HOST", (String) servicesList[j].getAttr("host"));
      temp.fadd("PORT", "" + servicesList[j].port);
      temp.fadd("INSTANCE", "" + servicesList[j].instance);

      if (servicesList[j].meta) {
        Object o = servicesList[j].dict.get("META");
        if (o instanceof String)
          temp.fadd("META", (String) o);
        if (o instanceof Frame)
          temp.fadd("META", (Frame) o);
      }

      out.fadd("SERVER", temp);
    }

    out.fadd("LEVEL", servicesList[0].level); // should all be the same
    return out;
  }

  VinciFrame resolveProxy(VinciFrame in, String workspace) {
    Debug.p("Proxy resolve");
    if (workspace == null)
      workspace = in.fgetString("WORKSPACE");

    if (workspace == null || workspace.equals(WS.workspace)) {
      // New spec - assume it is local
      return resolveLocal(in);
    }

    Service[] services = SR.getServices(ENV_PROXY);
    VinciFrame out = new VinciFrame();
    if (services == null || services.length == 0) {
      out.fadd("vinci:ERROR", "Cannot locate proxy service");
      return out;
    }

    // Iterate through all the registered proxy services and try to resolve at each
    for (int i = 0; i < services.length; i++) {
      Service S = services[i];
      if (S.realhost == null) {
        continue; // Can't do anything if realhost isn't specified
      }

      try {
        // If the host specified is the local one, then use resolveLocal() 
        // else it will have an infinite loop
        if (S.port == srvPort && InetAddress.getLocalHost().getHostAddress().equals(S.realhost))
          out = resolveLocal(in);
        else {
          Debug.p("Resolving with VNS: " + S.realhost + ":" + S.port);
          out = BaseClient.rpc(in, S.realhost, S.port);
        }
        System.out.println(out.toXML());
        if (out == null || out instanceof ErrorFrame || strip(out.fgetString("LEVEL")) == null
            || out.fgetString("vinci:ERROR") != null) {
          continue; // Check if resolve actually worked
        }
        return out;
      } catch (ServiceException se) {
        // Don't need to worry - caused due to an ErrorFrame as the server was unable to
        // resolve
        System.out.println(se);
        se.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
        out.fadd("vinci:ERROR", "Proxy forwarding failed due to " + e);
        return out;
      }
    }
    return new ErrorFrame("Proxy forwarding failed");
  }

  VinciFrame resolveDefaults(VinciFrame in) {
    String name = in.fgetString("SERVICE");
    String workspace;

    VinciFrame out;

    if (WS.workspace == null || WS.search.size() == 0)
      return resolveLocal(in); // Use resolveLocal() if WS was not defined

    for (int i = 0; i < WS.search.size(); i++) {
      workspace = ((String) WS.search.get(i));

      if (workspace.equals(WS.workspace)) {
        Debug.p("Resolving on local workspace ...");

        out = resolveLocal(in);
      } else {
        Debug.p("Resolving on workspace " + workspace + " ...");
        out = resolveProxy(in, workspace);
      }

      if (out.fgetString("vinci:ERROR") == null)
        return out;
    }

    Debug.p("Resolution failed");
    return new ErrorFrame("Could not find service " + name + " in default workspaces");
  }

  VinciFrame resolve(VinciFrame in) {
    logRequest(VNSConstants.RESOLVE_COMMAND, in.fgetString("vinci:REMOTEIP"), in.fgetString("SERVICE"));

    if (in.fgetString("WORKSPACE") == null) {
      return resolveDefaults(in);
    } else {
      Debug.p("Resolving on workspace " + in.fgetString("WORKSPACE") + " ...");
      return resolveProxy(in, null);
    }
  }

  VinciFrame serveon(VinciFrame in) {
    logRequest(VNSConstants.SERVEON_COMMAND, in.fgetString("vinci:REMOTEIP"), null);

    Service S = null, srv = null;
    VinciFrame out = new VinciFrame();

    String name = in.fgetString("SERVICE");
    if (strip(name) == null)
      return new ErrorFrame("Invalid service name specified : " + name);
    String host = in.fgetString("HOST");

    if (strip(host) == null) {
      Debug.p("Getting host from socket peer info");
      host = in.fgetString("vinci:REMOTEHOST");
      if (host == null)
        host = in.fgetString("vinci:REMOTEIP");
      if (host == null)
        return new ErrorFrame("Host could not be parsed - specify HOST");
      Debug.p("Peer host is : " + host);
    }

    String level = in.fgetString("LEVEL");

    // Default as specified in SPEC.txt
    if (strip(level) == null || level.trim().toLowerCase().equals("none"))
      level = "0";

    if (level.equals("all")) {
      out.fadd("vinci:ERROR", "Specific level must be given or none at all");
      return out;
    }

    String instance = in.fgetString("INSTANCE");
    try {
      instance = "" + Integer.parseInt(instance);
    } catch (Exception e) {
      instance = null;
    }

    if (instance == null)
      instance = "0";

    Debug.p("Host = " + host);
    String realhost = in.fgetString("IP");
    try {
      if (realhost == null)
        realhost = InetAddress.getByName(host).getHostAddress();
    } catch (Exception e) {
      out.fadd("vinci:ERROR", "Could not resolve IP due to Exception " + e);
      return out;
    }

    Debug.p("search: realhost = " + realhost + " - instance = " + instance);

    synchronized (SR) {
      level = "" + SR.getLevel(name, level);
    }

    Debug.p("Level = " + level);

    // Update the cache
    updateCache(name + "[" + level + "]");

    Object[] services;
    synchronized (SR) {
      services = SR.getServices(name, level);
    }

    if (services != null) {
      for (int i = 0; i < services.length; i++) {
        S = (Service) services[i];
        Debug.p("current: realhost = " + S.realhost + " - instance = " + S.instance);
        if (S.realhost.equals(realhost) && instance.equals("" + S.instance) && S.level.equals(level)) {
          srv = S;
          break;
        }
      }
    }

    if (srv == null) {
      Debug.p("Creating now");
      Hashtable H = new Hashtable();
      H.put("NAME", name);
      H.put("HOST", host);
      H.put("LEVEL", level);
      H.put("INSTANCE", instance);
      H.put("IP", realhost);
      srv = new Service(H);
      boolean ok = false;
      synchronized (SR) {
        Debug.p("Adding service : " + H.get("NAME") + ", lvl=" + H.get("LEVEL") + ",instance=" + H.get("INSTANCE")
            + ",ip=" + H.get("IP"));
        ok = SR.addService(srv);
      }
      if (!ok) {
        out.fadd("vinci:ERROR", "COuld not find or add service " + name);
        return out;
      }
    } else {
      if (srv.minport != srv.maxport) { // if it isn't running on a fixed port
        final String s_host = srv.host;
        final int s_port = srv.port;
        new Thread(new Runnable() {
          public void run() {
            Debug.p("Trying to shutdown old service ...");
            VinciFrame shutdown = new VinciFrame();
            shutdown
                .fadd(
                    "vinci:SHUTDOWN",
                    "Identical service started on this host. Use the INSTANCE tag to run multiple instances of the same service on a single host.");
            try {
              VinciFrame f = BaseClient.rpc(shutdown, s_host, s_port, 10000);
              Debug.p("Shutdown response received: " + f.toXML());
            } catch (Exception e) {
              Debug.p("Old service already closed: " + e);
            }
          }
        }).start();
      }
    }

    srv.updatePort();
    out.fadd("HOST", srv.host);
    out.fadd("PORT", srv.port);
    out.fadd("LEVEL", srv.level);
    out.fadd("INSTANCE", srv.instance);
    out.fadd("IP", srv.realhost);
    return out;
  }

  VinciFrame addService(VinciFrame in) {
    logRequest(dirCmdAddService, in.fgetString("vinci:REMOTEIP"), null);

    VinciFrame service = in.fgetVinciFrame("SERVICE");

    Hashtable H = new Hashtable();

    int total = service.getKeyValuePairCount();
    KeyValuePair P;
    for (int i = 0; i < total; i++) {
      P = service.getKeyValuePair(i);
      if (P.isValueALeaf()) {
        H.put(P.getKey(), P.getValueAsString());
      } else {
        H.put(P.getKey(), P.getValue());
      }
    }

    String level = (String) H.get("LEVEL");
    if (strip(level) == null) {
      H.put("LEVEL", "0"); // default level of 0
    }

    boolean ok = false;

    synchronized (SR) {
      Service S;
      ok = SR.addService(S = new Service(H));
      updateCache(S);
    }

    return getFrame(ok, "Add Service request failed");
  }

  VinciFrame addAlias(VinciFrame in) {
    logRequest(dirCmdAddAlias, in.fgetString("vinci:REMOTEIP"), null);

    VinciFrame service = in.fgetVinciFrame("SERVICE");

    boolean ok = true;

    if (service.fgetString("NAME") == null || service.fgetString("TARGET") == null)
      getFrame(false, "Malformed request");
    else {
      synchronized (SR) {
        ok = SR.addAlias(new ServiceAlias(service.fgetString("NAME"), service.fgetString("TARGET")));
      }
    }

    return getFrame(ok, "Add alias request failed");
  }

  VinciFrame delService(VinciFrame in) {
    logRequest(dirCmdDelService, in.fgetString("vinci:REMOTEIP"), null);

    VinciFrame service = in.fgetVinciFrame("SERVICE");

    Hashtable H = new Hashtable();

    int total = service.getKeyValuePairCount();
    KeyValuePair P = null;
    for (int i = 0; i < total; i++) {
      P = service.getKeyValuePair(i);

      if (P.isValueALeaf()) {
        H.put(P.getKey(), P.getValueAsString());
      } else {
        H.put(P.getKey(), P.getValue());
      }
    }

    String level = (String) H.get("LEVEL");
    if (strip(level) == null)
      H.put("LEVEL", "0"); // default level of 0

    boolean ok = false;

    synchronized (SR) {
      Service S;
      ok = SR.delService(S = new Service(H));
      updateCache(S);
    }

    return getFrame(ok, "Delete Service request failed");
  }

  VinciFrame delAlias(VinciFrame in) {
    logRequest(dirCmdDelAlias, in.fgetString("vinci:REMOTEIP"), null);
    VinciFrame service = in.fgetVinciFrame("SERVICE");
    boolean ok = true;
    if (service.fgetString("NAME") == null || service.fgetString("TARGET") == null) {
      getFrame(false, "Malformed request");
    } else {
      synchronized (SR) {
        ok = SR.delAlias(service.fgetString("NAME"));
      }
    }
    return getFrame(ok, "Delete alias request failed");
  }

  VinciFrame updateService(VinciFrame in) {
    logRequest(dirCmdUpdateService, in.fgetString("vinci:REMOTEIP"), null);

    VinciFrame service = in.fgetVinciFrame("SERVICE");

    Hashtable H = new Hashtable();

    int total = service.getKeyValuePairCount();
    KeyValuePair P;
    for (int i = 0; i < total; i++) {
      P = service.getKeyValuePair(i);
      if (P.isValueALeaf()) {
        H.put(P.getKey(), P.getValueAsString());
      } else {
        H.put(P.getKey(), P.getValue());
      }
    }

    boolean ok = false;

    synchronized (SR) {
      Service S;
      ok = SR.updateService(S = new Service(H));
      updateCache(S);
    }

    return getFrame(ok, "Update Service request failed");
  }

  VinciFrame getList(VinciFrame in) {
    String level = in.fgetString("LEVEL");

    // Default as specified in SPEC.txt
    if (level == null || level.trim().toLowerCase().equals("none")) {
      level = "-1";
    }

    String prefix = in.fgetString("PREFIX");
    if (prefix == null) {
      prefix = "";
    }

    logRequest(dirCmdGetList, in.fgetString("vinci:REMOTEIP"), prefix);

    Object[] servicelist;
    synchronized (SR) {
      Debug.p("Getting list for level : " + level);
      servicelist = SR.listServices(prefix, level);
      if (servicelist == null)
        servicelist = new Object[0]; // For safety
      Debug.p("Matches for getList : " + servicelist.length);
    }

    VinciFrame F = new VinciFrame();
    for (int i = 0; i < servicelist.length; i++) {
      if (ServiceAlias.isAlias(servicelist))
        F.fadd("SERVICE", ((ServiceAlias) servicelist[i]).toFrame());
      else
        F.fadd("SERVICE", ((Service) servicelist[i]).toFrame());
    }

    return F;
  }

  VinciFrame getNames(VinciFrame in) {
    String level = in.fgetString("LEVEL");

    // Default as specified in SPEC.txt
    if (level == null || level.trim().toLowerCase().equals("none"))
      level = "-1";

    String prefix = in.fgetString("PREFIX");
    if (prefix == null)
      prefix = "";

    logRequest(dirCmdGetNames, in.fgetString("vinci:REMOTEIP"), prefix);

    String[] servicelist;
    synchronized (SR) {
      Debug.p("Getting names list for level : " + level);
      servicelist = SR.listNames(prefix, level);
      if (servicelist == null)
        servicelist = new String[0]; // For safety
      Debug.p("Matches for getNames : " + servicelist.length);
    }

    VinciFrame F = new VinciFrame();
    for (int i = 0; i < servicelist.length; i++) {
      F.fadd("SERVICE", servicelist[i]);
    }

    return F;
  }

  VinciFrame getHits(VinciFrame in) {
    String type = in.fgetString("TYPE");

    VinciFrame F = new VinciFrame();
    VinciFrame temp;
    String key;

    if (type != null) {
      if (type.equals("all")) {
        Enumeration keys = hits.keys();
        while (keys.hasMoreElements()) {
          key = (String) keys.nextElement();
          temp = new VinciFrame();
          temp.fadd("TYPE", key);
          temp.fadd("COUNT", hits.get(key).toString());
          F.fadd("HITS", temp);
        }
        F.fadd("TOTAL", "" + totalhits);
      } else {
        if (hits.get(type) != null)
          F.fadd("HITS", hits.get(type).toString());
        else {
          F.fadd("vinci:ERROR", "No such type: " + type);
          return F;
        }
      }
    } else {
      F.fadd("HITS", "" + totalhits);
    }

    F.fadd("STARTED", "" + starttime);

    return F;
  }

  /* Caching routines */
  private void cache(String s, Object o) {
    synchronized (cachedResults) {
      cachedResults.put(s, o);
    }
  }

  private void updateCache(String s) {
    synchronized (cachedResults) {
      cachedResults.remove(s);
    }
  }

  private Service updateCache(Service S) {
    String name = (String) S.getAttr("name");
    String level = "";
    synchronized (SR) {
      level = "" + SR.getLevel(name, (String) S.getAttr("level"));
    }

    updateCache(name + "[" + level + "]");
    return S;
  }

  private Object checkCache(String s) {
    synchronized (cachedResults) {
      Object o = null;
      do {
        o = cachedResults.get(s);
        if (o == null)
          return null;
        if (o instanceof ProxyCachedItem)
          s = ((ProxyCachedItem) o).altKey;
      } while (!(o instanceof CachedItem));
      return o;
    }
  }

  /* Helper method to return the correct VinciFrame */
  VinciFrame getFrame(boolean ok, String err) {
    VinciFrame F = null;
    if (ok) {
      F = new VinciFrame();
      F.fadd("STATUS", "OK");
    } else
      return new ErrorFrame(err);

    return F;
  }

  /* Helper methods */
  public static String strip(String s) {
    if (s == null || s.trim().equals(""))
      return null;
    return s;
  }

  public static String emptyString(String s) {
    if (strip(s) == null)
      return "";
    return s;
  }

  /* Logging routines */
  void logRequest(String type, String ip, String text) {
    if (ip == null) {
      ip = BaseServerRunnable.getSocket().getInetAddress().getHostAddress();
      if (ip == null) {
        ip = "UNKNOWN";
      }
    }
    if (text == null)
      text = "";

    // Update hit counter
    synchronized (hits) {
      Integer I = (Integer) hits.get(type);
      if (I == null)
        I = new Integer(0);
      I = new Integer(I.intValue() + 1);

      hits.put(type, I);
      totalhits++;
    }
    String ts = (new Date()).toString();
    String write_me = ip + " - [" + ts + "] " + type + " " + text + "\n";
    Debug.p(write_me);

    if (log == null)
      return;

    synchronized (log) {
      // Make an entry in the log file
      try {
        log.write(write_me);
      } catch (IOException e) {
        Debug.reportException(e);
      }
    }
  }

  public void cleanExit() {
    Debug.p("Exitting now ...");
    quitFile.delete();
    try {
      backupThreadRunnable.stop = true;
      backupThread.interrupt();
    } catch (Exception e) {
      Debug.reportException(e);
    }

    backupThreadRunnable.forceWrite();

    if (log != null) {
      try {
        synchronized (log) {
          log.close();
        }
      } catch (IOException e) {
        Debug.reportException(e);
      }
    }
  }

}

/* Class for the backup thread */
class BackupThread implements Runnable {
  VNS                        parent;
  String                     backupFile;
  String                     configFile  = null;
  String                     counterFile = null;
  int                        interval;

  volatile protected boolean stop        = false;

  public BackupThread(VNS parent, String backupFile, String configFile, int interval, String counterFile) {
    // We create new strings so that we can start another VNS instance without disturbing this one
    this.parent = parent;
    this.backupFile = new String(backupFile);
    if (configFile != null)
      this.configFile = new String(configFile);
    if (counterFile != null)
      this.counterFile = new String(counterFile);
    this.interval = interval * 1000; // Convert secs to millisecs
  }

  public void forceWrite() {
    parent.saveConfig(backupFile);
    if (configFile != null)
      parent.saveConfig(configFile);
    if (counterFile != null)
      parent.saveCounters(counterFile);
    Debug.p("All files written to disk");
  }

  public void run() {
    Debug.p("Backup thread started");
    while (true) {
      parent.saveConfig(backupFile);
      if (configFile != null) {
        parent.saveConfig(configFile);
      }
      if (counterFile != null) {
        parent.saveCounters(counterFile);
      }
      try {
        Thread.sleep(interval);
      } catch (InterruptedException e) {
        break;
      }
      if (stop) {
        break;
      }
    }
    Debug.p("Backup thread exitted");
  }

}

/* Thread to check for quit command */
class exitThread extends Thread {
  BaseServer parent = null;
  boolean    run    = false;

  public exitThread(BaseServer parent) {
    this.parent = parent;
    run = true;
  }

  public void run() {
    if (parent == null)
      return;

    int i = 0;

    char[] cmd = { 'q', 'u', 'i', 't' };
    System.out.println(">>>>>>>>>>>>> VNS is up and running! <<<<<<<<<<<<<<<<<");
    System.out.println(">>>>>>>>>>>>> Type \'quit\' and hit ENTER to terminate VNS <<<<<<<<<<<<<");
    while (run) {
      i = 0;

      try {
        if (System.in.available() > 0) {
          while (System.in.available() > 0) {
            if ((System.in.read()) != cmd[i])
              i = 0;
            else
              i++;
            if (i >= cmd.length) {
              run = false;
              break;
            }
          }
        }
      } catch (Exception e) {
      }

      try {
        Thread.sleep(1000);
        if (VNS.quitFile.exists()) {
          run = false;
        }
      } catch (InterruptedException e2) {
        run = false;
      }
    }

    System.out.println("Got quit command from user");
    parent.shutdownServing();
    System.out.println("Request handling thread successfully shutdown");
  }
}

/* Encapsulating class for cached item */
class CachedItem {

  CachedItem(Service[] s) {
    servicesList = s;
  }

  protected Service[] servicesList = null;
}

/* Points to some other cached entry */
class ProxyCachedItem {

  protected String altKey;

  ProxyCachedItem(String s) {
    altKey = s;
  }
}
