# docker build -t bench-cuki .
From maven:3.8.5-openjdk-8

COPY . /wss-estimation

RUN cd /wss-estimation && \
    mvn assembly:assembly \
      -T 4C \
      -Dmaven.javadoc.skip=true \
      -DskipTests \
      -Dlicense.skip=true \
      -Dcheckstyle.skip=true \
      -Dfindbugs.skip=true

WORKDIR /wss-estimation
