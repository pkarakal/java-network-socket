# Network Socket Programming in Java
This is a project developed for the Networking II class of ECE department in AUTH. 

##Prerequisites
1.  OpenJDK 11 or higher (it may work with an earlier version as well)
2.  Maven

## Instructions
1. Clone the repository
    
   ```shell
    $ git clone git@github.com:pkarakal/java-network-socket.git
    ```
2. Go to that directory
     ```shell
    $ cd java-network-socket
     ```
3. Build it using Maven
    ```shell
   $ mvn package
    ```
4. Run it by using the following instruction
    ```shell
   $ java -jar ./target/networking-1.0.jar --serverIP=<IP_OF_SERVER> \
    --serverPort=<SERVER_LISTENING_PORT> -- clientPort=<CLIENT_LISTENING_PORT \
    --request-code=<REQUEST_CODE> 
    ```
   and add other arguments if necessary

## Message Dispatcher
The Message Dispatcher is the base class for UDP request. This is used for the message and thermo
requests. 
For those jobs, it just sends one request, receives one UDP packet with the answer and logs it 
to the `Network.log` file which is created in the directory the job started. (Usually java-network-socket)


## Image & Video Receiver
The image and video receiver extend the message dispatcher. It uses 2 threads one to receive and one
send requests to the server. It can also take multiple parameters depending on the job you want to 
run which are listed below:
1. CAM: It takes an image from a different camera. The accepted values are `PTZ` and `FIX`
2. DIR: When CAM=PTZ is defined, the user can control the camera by moving it on XY axes. 
The accepted values are: 
*  U-> Up,
*  D-> Down,
*  L-> Left,
*  R-> Right,
*  C-> Center
*  M -> Remember
3. FLOW: When `flow` is enabled, there is a constant interaction with the server and once 
a packet gets received from the ReceiveThread, it puts `true` to a blocking queue which the 
SendThread gets, and sends `NEXT` to the server to get the next UDP packet. Once complete, 
the program exits. Accepted values are `ON` and `OFF` 
4. UDP: The length of the UDP packets. Accepted values are 128,256,512,1024. Default=128

Once the complete image/video gets obtained, a new file is created with .jpg or .mjpeg format. 

## Audio Streaming
The audio streaming extends the MessageDispatcher class. It gets UDP packets from the server
which correspond to 30s of audio playback. It also uses two threads, one to create the audio 
buffer and another one to play it back. It takes the following CLI arguments
1. audioDPCM: Defines whether to use adaptive, non-adaptive or adaptive-quantiser DPCM.
Accepted values are: AD and AQ.
2. soundCode: This controls the source and number of packets of the sound. The first character
can only be F or T. When F is used a random audio clip from the server is sent and when T is
used it generates a random sound in the 200 - 4000 Hz frequency range. The other 3 characters 
control the number of audio packats to get

## TCP Receiver
It implements the Request interface, and gets the /index.html of the serverIP defined in the CLI 
arguments. To use it tcp, you have to define the IP of your computer. To get your IP, either 
search it in your router admin page or use `ifconfig` to get your IP

## Ithakicopter 
Ithakicopter extends TCPReceiver. It used to control a small copter which is in the laboratory. 
It sends TCP packets to the server, which control the PWM of the left and right motors. 

## OBD-II Statistics
OBDStatistics extends TCPReceiver. It is used to get some diagnostic information of a vehicle
in the laboratory. It sends a request to the server to get info which are equal to 4 minutes of 
the vehicle operation and store it in a csv file with the name of the operation. It takes a CLI 
argument `--obdOpCode=` with the OBD code to send to the server. The accepted values are
*  1 -> Engine runtime
*  2 -> Intake air temperature
*  3 -> Throttle position
*  4 -> Engine RPM
*  5 -> Vehicle speed
*  6 -> Coolant temperature
