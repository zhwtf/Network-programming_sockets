#include <netinet/in.h>
#include <time.h>
#include <strings.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h> //for getaddrinfo and getnameinfo function

#define MAXLINE     4096    /* max text line length */
#define LISTENQ     1024    /* 2nd argument to listen() */
#define DAYTIME_PORT 3333


int main(int argc, char **argv)
{
    int     listenfd, connfd;
    struct sockaddr_in servaddr;
    struct sockaddr_in cliaddr;
    char    recvline[MAXLINE + 1];
    socklen_t len;
    char    buff[MAXLINE];

    time_t ticks;
    int s, n;
    char hos[MAXLINE];

    char servname[MAXLINE];
    char ipstr[MAXLINE];
    struct addrinfo hints;
    struct addrinfo *result, *rp;
    int k, sfd;

    if (argc != 2) {
        printf("usage: tunnel <portnumber>\n");
        exit(1);
    }

    int portNum = atoi(argv[1]);
    listenfd = socket(AF_INET, SOCK_STREAM, 0);

    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(portNum); /* daytime server */

    bind(listenfd, (struct sockaddr *) &servaddr, sizeof(servaddr));

    listen(listenfd, LISTENQ);

    for ( ; ; ) {
        len = sizeof(cliaddr);
        connfd = accept(listenfd, (struct sockaddr *) &cliaddr, &len);

        s = getnameinfo((struct sockaddr *) &cliaddr, len, hos, sizeof(hos), NULL, 0, NI_NUMERICSERV);
        ticks = time(NULL);
        snprintf(buff, sizeof(buff), "%.24s\r\n", ctime(&ticks));
        //read the server IP and portNum
        char servIP[MAXLINE];
        int buflen;
        read(connfd, (char*)&buflen, sizeof(buflen));
        buflen = ntohl(buflen);
        read(connfd, servIP, buflen);
        printf("servIP: %s\n\n", servIP);


        //read the server Port number from client
        char serPort[MAXLINE];
        int buflen2;
        read(connfd, (char*)&buflen2, sizeof(buflen2));
        buflen2 = ntohl(buflen2);
        read(connfd, serPort, buflen2);
        k = atoi(serPort);
        printf("server Portnumber: %d\n\n", k);

        //write(connfd, buff, strlen(buff));
        //printf("Sending response: %s", buff);
        printf("client Name: %s\n\n", hos);
        //close(connfd);









        // now the tunnel serves as a client and makes connection with the server
        // first it needs to take the 2 arguments(server IP and port number) so it needs to
        // read from the client


        // now create the sockaddr and socket to make connection with server
        bzero(&hints, sizeof(hints));
        hints.ai_family = AF_INET; // Allow only IPv4
        hints.ai_flags = 0;
        hints.ai_socktype = SOCK_STREAM; //TCP socket
        hints.ai_protocol = 0; //Any protocol

        s = getaddrinfo(servIP, serPort, &hints, &result);
        if (s != 0){
          fprintf(stderr, "getaddrinfo: %s\n", gai_strerror(s));
          exit(1);
        }



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
              //convert the ip to a string
              inet_ntop(rp->ai_family, addr, ipstr, sizeof(ipstr));
              printf("Server IP Address: %s\n", ipstr);
              break;
          }
          close(sfd);

        }

        //No address succeeded
        if (rp == NULL) {
          fprintf(stderr, "Could not connect to the server\n");
          //exit(1);
          continue;
        }

        //free the result
        freeaddrinfo(result);


        //now read the time from the server and send it to the client
        n = read(sfd, recvline, MAXLINE);
        if(n < 0){
          printf("read error\n");
          close(sfd);
          close(connfd);
          continue;
        }

        //Now tunnel writes server name, sever IP and time to the client

        //write the server name to the client
        int datalen = strlen(servname);
        int tmp = htonl(datalen);
        write(connfd, (char*)&tmp, sizeof(tmp));
        write(connfd, servname, datalen);

        //write the server IP to the client
        int datalen2 = strlen(ipstr);
        int tmp2 = htonl(datalen2);
        write(connfd, (char*)&tmp2, sizeof(tmp2));
        write(connfd, ipstr, datalen2);

        //write the time to the client;
        int datalen3 = strlen(recvline);
        int tmp3 = htonl(datalen3);
        write(connfd, (char*)&tmp3, sizeof(tmp3));
        write(connfd, recvline, datalen3);


        printf("Time sent from server: %s\n", recvline);
        close(sfd);
        close(connfd);





    }
}
