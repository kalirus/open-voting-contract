FROM anapsix/alpine-java:jdk8 as builder

RUN apk add --no-cache --update ca-certificates wget git && \
    update-ca-certificates

RUN wget -O - https://github.com/sbt/sbt/releases/download/v1.2.8/sbt-1.2.8.tgz \
    | gunzip \
    | tar -x -C /usr/local

ENV PATH="/usr/local/sbt/bin:${PATH}"

COPY project/assembly.sbt /contract/project/assembly.sbt
COPY project/Dependencies.scala /contract/project/Dependencies.scala
COPY src /contract/src
COPY build.sbt /contract/build.sbt

RUN cd /contract/ && sbt clean assembly

RUN mv `find /contract/target/scala-2.12 -name '*.jar'` /contract.jar && chmod -R 744 /contract.jar

FROM java:openjdk-8u111-jre-alpine
MAINTAINER Ruslan Kalimullin <RKalimullin@wavesplatform.com>
COPY --from=builder /contract.jar /app/contract.jar
ADD run.sh /
RUN chmod +x run.sh
RUN chmod +x /app/contract.jar
CMD ["/bin/sleep", "6000"]