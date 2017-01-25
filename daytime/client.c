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
    int     n;
    char    recvline[MAXLINE + 1];
    char ipstr[MAXLINE];

    //for the use of the getaddrinfo function
    struct addrinfo hints;
    struct addrinfo *result, *rp;
    char servname[MAXLINE];
    int sfd, s;
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
    if (argc == 5) {
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
              //printf("Via Tunnel: %s\n", servname);

            }
            else{
              printf("cannot map the address\n");
            }
            //convert the ip to a string
            inet_ntop(rp->ai_family, addr, ipstr, sizeof(ipstr));
            //printf("Tunnel IP Address: %s\n", ipstr);
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

      char serIP[MAXLINE];
      strcpy(serIP, argv[3]);

      //now write the server IP and portNum to the tunnel
      int datalen = strlen(argv[3]);
      //printf("%s\n", serIP);
      //printf("%d\n", datalen);

      int tmp = htonl(datalen);
      write(sfd, (char*)&tmp, sizeof(tmp));
      //if (n < 0) error("ERROR writing to socket");
      write(sfd, serIP, datalen);
      //if (n < 0) error("ERROR writing to socket");


      //now pass the server port number to the tunnel
      char serPort[MAXLINE];
      strcpy(serPort, argv[4]);
      int datalen2 = strlen(argv[4]);
      int tmp2 = htonl(datalen2);
      write(sfd, (char*)&tmp2, sizeof(tmp2));
      write(sfd, serPort, datalen2);

      //now client read info from tunnel
      //read the server name from tunnel
      char servName[MAXLINE];
      int buflen;
      read(sfd, (char*)&buflen, sizeof(buflen));
      buflen = ntohl(buflen);
      read(sfd, servName, buflen);
      printf("Server Name: %s\n", servName);

      //read the server IP from tunnel
      char serverIP[MAXLINE];
      int buflen2;
      read(sfd, (char*)&buflen2, sizeof(buflen2));
      buflen2 = ntohl(buflen2);
      read(sfd, serverIP, buflen2);
      printf("IP Address: %s\n", serverIP);

      char serTime[MAXLINE];
      int buflen3;
      read(sfd, (char*)&buflen3, sizeof(buflen3));
      buflen3 = ntohl(buflen3);
      read(sfd, serTime, buflen3);
      printf("Time: %s\n\n", serTime);

      //print the tunnel name, IP and prot number
      printf("Via Tunnel: %s\n", servname);
      printf("IP Address: %s\n", ipstr);
      printf("Port Number: %s\n", argv[2]);






      exit(0);
    }



    printf("Invalid arguments. usage:server <IP address> and <port number>  or  tunnel <IP address> and <portnumber> plus server <IP address> and <port number>");

    exit(1);
}
