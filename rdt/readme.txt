/* hao zheng
hza89@sfu.ca
*/


1. How to run the code
In the command line
 (1) run the server first with the client_hostname, client_port, and server_local_port arguments
 (2)  run the client with server_hostname, server_port, and client_local_port arguments
 (3) when testing a new testcase, need to use a new server port number and client port number
 (4) When testing on the same machine, the host name is: localhost and the IP address is 127.0.0.1

I also include the test case java files(6 test cases) in rdt folder in case something goes wrong, since
all the test cases run well on my machine.

Based on my implementation of the receive() function, the "System.out.println("");" in the while loop should
be removed, such like this(in the TestServer.java file)
while (true) {
    int size = rdt.receive(buf, RDT.MSS);
    for (int i=0; i<size; i++)
        System.out.println(buf[i]);

    //System.out.println("");
    System.out.flush();

}
otherwise, the real output would be hard to observe since the System.out.println("") would push the commandline up


2. Provided many indications of the process and println() to show the steps which have finished

3. Close the connection gracefully, use TCP-style connection teardown.
