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

int
main(int argc, char **argv)
{
    int     sockfd, n;
    char    recvline[MAXLINE + 1];
    char ipstr[MAXLINE];
    struct sockaddr_in servaddr;
    //for the use of the getaddrinfo function
    struct addrinfo hints;
    struct addrinfo *result, *rp;
    socklen_t lensock;
    int k; //to hold the getnameinfo return
    char sername[MAXLINE]; //to store the server name
    int sfd, s, j;
    if (argc != 3) {
        printf("usage: client <IPaddress> and <portnumber>\n");
        exit(1);
    }

    /* Obtain address(es) matching host/port */

    //memset(&hints, 0, sizeof(struct addrinfo));
    bzero(&hints, sizeof(hints));
    hints.ai_family = AF_INET; // Allow only IPv4
    hints.ai_flags = AI_CANONNAME; //tell the function to return the canonical name of the host
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
          lensock = sizeof(*ipv4);
          k = getnameinfo(rp->ai_addr, rp->ai_addrlen, sername, sizeof(sername), NULL, 0, NI_NAMEREQD);
          //k = getnameinfo((struct sockaddr *) &ipv4, lensock, sername, sizeof(sername), NULL, 0, NI_NAMEREQD);
          if (k == 0){
            printf("Server Name: %s\n", sername);
          }
          else{
            printf("cannot reverse the address\n");
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



/*
    if ( (sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0) {
        printf("socket error\n");
        exit(1);
    }

    int portNum = atoi(argv[2]);

    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(portNum);
    if (inet_pton(AF_INET, argv[1], &servaddr.sin_addr) <= 0) {
        printf("inet_pton error for %s\n", argv[1]);
        exit(1);
    }

    if (connect(sockfd, (struct sockaddr *) &servaddr, sizeof(servaddr)) < 0) {
        printf("connect error\n");
        exit(1);
    }

*/
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
