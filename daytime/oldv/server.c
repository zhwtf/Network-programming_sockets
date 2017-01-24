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

int
main(int argc, char **argv)
{
    int     listenfd, connfd;
    struct sockaddr_in servaddr;
    struct sockaddr_in cliaddr;
    socklen_t len;
    char    buff[MAXLINE];
    time_t ticks;
    int s;
    char hos[MAXLINE];
    //char cli[MAXLINE];

    if (argc != 2) {
        printf("usage: server <portnumber>\n");
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
        if (s == 0){
          printf("Client Name: %s\n",hos);
        }
        else{
          printf("cannot map the address\n");
        }
        //print the ip address of the client
        //char ipstr[MAXLINE];
        //inet_ntop(AF_INET, &cliaddr.sin_addr, ipstr, sizeof(ipstr));
        //printf("IP Address: %s\n", ipstr);
        ticks = time(NULL);
        snprintf(buff, sizeof(buff), "%.24s\r\n", ctime(&ticks));
        write(connfd, buff, strlen(buff));
        printf("Sending response: %s", buff);

        close(connfd);
    }
}
