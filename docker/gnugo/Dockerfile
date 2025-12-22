FROM ubuntu:noble@sha256:c35e29c9450151419d9448b0fd75374fec4fff364a27f176fb458d472dfc9e54

RUN apt-get update && \
    apt-get install -y \
    gnugo=3.8-11build2 \
    socat=1.8.0.0-4build3 && \
    rm -rf /var/lib/apt/lists/*

EXPOSE 8001

CMD socat TCP-LISTEN:8001,fork EXEC:"/usr/games/gnugo --mode gtp --chinese-rules"
