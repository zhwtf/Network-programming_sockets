#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <stdlib.h>
#include <strings.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <netdb.h>  //for getaddrinfo and getnameinfo function

#define MAXLINE     4096    /* max text line length */
#define DAYTIME_PORT 3333


int main(int argc, char **argv)
{
    int     sockfd, n;
    char    recvline[MAXLINE + 1];
    char ipstr[MAXLINE];
    struct sockaddr_in servaddr;
    //for the use of the getaddrinfo function
    struct addrinfo hints;
    struct addrinfo *result, *rp;
    char servname[MAXLINE];
    int sfd, s, j;
    if (argc == 3) {
        //printf("usage: client <IPaddress> and <portnumber>\n");
        //exit(1);

        // only 2 arguments so the client directly connects to server
        /* Obtain address(es) matching host/port */

        //memset(&hints, 0, sizeof(struct addrinfo));
        bzero(&hints, sizeof(hints));
        hints.ai_family = AF_INET; // Allow only IPv4
        hints.ai_flags = 0; //tell the function to return the canonical name of the host
        hints.ai_socktype = SOCK_STREAM; //TCP socket
        hints.ai_protocol = 0; //Any protocol

        s = getaddrinfo(argv[1], argv[2], &hints, &result);
        if (s != 0){
          fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(s));
          exit(1);
        }
        /* getaddrinfo() returns a list of address structures.
                  Try each address until we successfully connect(2).
                  If socket(2) (or connect(2)) fails, we (close the socket
                  and) try the next address. */

        for (rp = result; rp != NULL; rp = rp->ai_next){
          //try to build the valid socket
          sfd = socket(rp->ai_family, rp->ai_socktype, rp->ai_protocol);
          if (sfd == -1){ //try the next addrinfo node
              continue;
          }

          if (connect(sfd, rp->ai_addr, rp->ai_addrlen) != -1){//connect successfully
              //now we can print the name and address of the server
              //printf("Server Name: %s\n", rp->ai_canonname);
              void *addr;
              struct sockaddr_in *ipv4 = (struct sockaddr_in *)rp->ai_addr;
              addr = &(ipv4->sin_addr); //get the numeric addrss
              if (getnameinfo(rp->ai_addr, rp->ai_addrlen, servname, sizeof(servname), NULL, 0, NI_NUMERICSERV) == 0){
                printf("Server Name: %s\n", servname);
              }
              else{
                printf("cannot map the address\n");
              }
              //convert the ip to a string
              inet_ntop(rp->ai_family, addr, ipstr, sizeof(ipstr));
              printf("IP Address: %s\n", ipstr);
              break;
          }
          close(sfd);

        }

        //No address succeeded
        if (rp == NULL) {
          fprintf(stderr, "Could not connect to the server\n");
          exit(1);
        }

        //free the result
        freeaddrinfo(result);

        while ( (n = read(sfd, recvline, MAXLINE)) > 0) {
            recvline[n] = 0;        /* null terminate */
            if (fputs(recvline, stdout) == EOF) {
                printf("fputs error\n");
                exit(1);
            }
        }
        if (n < 0) {
            printf("read error\n");
            exit(1);
        }

        exit(0);

    }



// if the arguments are 4, which means the client connects to the tunnel
   else if (argc == 5){
     //first connect to the tunnel. The same process
     bzero(&hints, sizeof(hints));
     hints.ai_family = AF_INET; // Allow only IPv4
     hints.ai_flags = 0; //tell the function to return the canonical name of the host
     hints.ai_socktype = SOCK_STREAM; //TCP socket
     hints.ai_protocol = 0; //Any protocol

     s = getaddrinfo(argv[1], argv[2], &hints, &result);
     if (s != 0){
       fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(s));
       exit(1);
     }
     /* getaddrinfo() returns a list of address structures.
               Try each address until we successfully connect(2).
               If socket(2) (or connect(2)) fails, we (close the socket
               and) try the next address. */

     for (rp = result; rp != NULL; rp = rp->ai_next){
       //try to build the valid socket
       sfd = socket(rp->ai_family, rp->ai_socktype, rp->ai_protocol);
       if (sfd == -1){ //try the next addrinfo node
           continue;
       }

       if (connect(sfd, rp->ai_addr, rp->ai_addrlen) != -1){//connect successfully
           //now we can print the name and address of the server
           //printf("Server Name: %s\n", rp->ai_canonname);
           void *addr;
           struct sockaddr_in *ipv4 = (struct sockaddr_in *)rp->ai_addr;
           addr = &(ipv4->sin_addr); //get the numeric addrss
           if (getnameinfo(rp->ai_addr, rp->ai_addrlen, servname, sizeof(servname), NULL, 0, NI_NUMERICSERV) == 0){
             printf("Via Tunnel: %s\n", servname);

           }
           else{
             printf("cannot map the address\n");
           }
           //convert the ip to a string
           inet_ntop(rp->ai_family, addr, ipstr, sizeof(ipstr));
           printf("IP Address: %s\n", ipstr);
           break;
       }
       close(sfd);

     }

     //No address succeeded
     if (rp == NULL) {
       fprintf(stderr, "Could not connect to the server\n");
       exit(1);
     }

     //free the result
     freeaddrinfo(result);

     //now write the server IP and portNum to the tunnel
     int datalen = strlen(argv[3]);
     int tmp = htonl(datalen);
     n = write(sfd, (char*)&tmp, sizeof(tmp));
     //if (n < 0) error("ERROR writing to socket");
     n = write(sfd, argv[3], datalen);
     //if (n < 0) error("ERROR writing to socket");

     datalen = strlen(argv[4]);
     tmp = htonl(datalen);
     n = write(sfd, (char*)&tmp, sizeof(tmp));
     //if (n < 0) error("ERROR writing to socket");
     n = write(sfd, argv[4], datalen);
     //if (n < 0) error("ERROR writing to socket");


     //now read the data passed by tunnel
     char holdserverinfo[MAXLINE][MAXLINE];
     int buflen;
     n = read(sfd, (char*)&buflen, sizeof(buflen));
     //if (n < 0) error("ERROR reading from socket");
     buflen = ntohl(buflen);
     n = read(sfd, holdserverinfo[0], buflen);
     //if (n < 0) error("ERROR reading from socket");


     n = read(sfd, (char*)&buflen, sizeof(buflen));
     //if (n < 0) error("ERROR reading from socket");
     buflen = ntohl(buflen);
     n = read(sfd, holdserverinfo[1], buflen);
     //if (n < 0) error("ERROR reading from socket");


     printf("Server Name: %s\n", holdserverinfo[0]);
     printf("IP Address: %s\n", holdserverinfo[1]);

     n = read(sfd, holdserverinfo[2], MAXLINE);
     //if (n < 0) error("ERROR reading from socket");
     printf("Time: %s\n\n", holdserverinfo[2]);

     //print the tunnel info
     printf("Via Tunnel: %s\n", servname);
     printf("Port Number: %s\n", argv[2]);
     exit(0);
   }



   else{
     printf("usage: server <IPaddress> and <portnumber> or tunnel <IPaddress> and <portnumber> plus server <IPaddress> and <portnumber>\n");
     exit(1);
   }












   exit(0);
}
