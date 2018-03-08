FROM mojix/base-tomcat

#Install basic repo and dependencies for image
RUN curl -o /etc/yum.repos.d/public-yum-ol7.repo http://public-yum.oracle.com/public-yum-ol7.repo
RUN yum install wget tar vim gzip -y

ENV MOJIX_HOME /app/
ENV JAR_HOME /jar/
ENV JPROFILER_VERSION="9_2_1"
RUN mkdir -p "$MOJIX_HOME" && rm -rf $CATALINA_HOME/webapps/*

# Installing JProfiler.
ENV JPROFILER_VERSION="9_2_1"
COPY download-jprofiler.sh /tmp/download-jprofiler.sh
RUN /tmp/download-jprofiler.sh && \
    tar -xzf /tmp/jprofiler_linux_${JPROFILER_VERSION}.tar.gz -C /app && \
    rm /tmp/jprofiler_linux_${JPROFILER_VERSION}.tar.gz

RUN cd /app && \
    ln -s ./jprofiler9 jprofiler

COPY server.xml $CATALINA_HOME/conf/server.xml
COPY context.xml $CATALINA_HOME/conf/context.xml
COPY riot-core-services.xml $CATALINA_HOME/conf/Catalina/localhost/
COPY app $MOJIX_HOME

COPY build/libs/riot-core-services-all* $JAR_HOME
COPY run.sh /run.sh

ENTRYPOINT ["/run.sh"]
CMD ["catalina.sh", "run"]
