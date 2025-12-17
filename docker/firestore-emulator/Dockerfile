FROM ubuntu:24.04

#==============================
# Install firebase
#==============================
RUN apt update
RUN apt -y install sudo curl openjdk-21-jre
RUN curl -sL https://firebase.tools | bash



#==============================
# Copy files
#==============================
COPY ./docker/firestore-emulator/start.sh /opt/firestore-emulator/start.sh
COPY ./docker/firestore-emulator/firebase.json /opt/firestore-emulator/firebase.json



#==============================
# Create user
#==============================
WORKDIR /opt/firestore-emulator

RUN useradd -ms /bin/bash firestore-emulator \
  && chown -R firestore-emulator:firestore-emulator /opt/firestore-emulator

USER firestore-emulator

ENTRYPOINT ["./start.sh"]
