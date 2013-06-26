
MAIL = [ "javax.mail:mail:jar:1.4.6" ]
# IMAP = [ 'com.sun.mail:imap:jar:1.5.0' ] + MAIL

LOGGER = [
    'org.slf4j:slf4j-api:jar:1.6.4',
    'org.slf4j:slf4j-log4j12:jar:1.6.4',
    'log4j:log4j:jar:1.2.16',
    'log4j:apache-log4j-extras:jar:1.1'
    ]

JAVAMAILDIR = [ "net.ukrpost.javamaildir:javamaildir:jar:0.6" ] + MAIL

ACTIVATION = [ "javax.activation:activation:jar:1.1" ]

COMMONS_CODEC       = [ "commons-codec:commons-codec:jar:1.7" ]
COMMONS_COLLECTIONS = [ "commons-collections:commons-collections:jar:3.2.1" ]
COMMONS_FILEUPLOAD  = [ "commons-fileupload:commons-fileupload:jar:1.2.1" ]
COMMONS_IO          = [ "commons-io:commons-io:jar:2.1" ]
COMMONS_LANG        = [ "commons-lang:commons-lang:jar:2.6" ]
COMMONS_LANG3       = [ 'org.apache.commons:commons-lang3:jar:3.1' ]
COMMONS_LOGGING     = [ "commons-logging:commons-logging:jar:1.1.1" ]
COMMONS_POOL        = [ "commons-pool:commons-pool:jar:1.6" ]
COMMONS_NET         = [ "commons-net:commons-net:jar:2.0" ]
COMMONS_DISCOVERY   = [ "commons-discovery:commons-discovery:jar:0.4" ]
COMMONS_CLI         = [ "commons-cli:commons-cli:jar:1.2" ]
COMMONS_EL          = [ "commons-el:commons-el:jar:1.0" ]
COMMONS_CSV         = [ "commons-csv:commons-csv:jar:1.0" ]
COMMONS_DAEMON      = [ "commons-daemon:commons-daemon:jar:1.0.8" ]
COMMONS_CONFIGURATION = [ "commons-configuration:commons-configuration:jar:1.9" ]

HTTPCLIENT = [
    "commons-httpclient:commons-httpclient:jar:3.1",
    ] + COMMONS_CODEC + COMMONS_LOGGING

JUNIT = [ "junit:junit:jar:4.10" ]

ANT = [ "org.apache.ant:ant:jar:1.8.0" ]

JETTY_VERSION_6 = "6.1.14"
JETTY_6 = [
    "org.mortbay.jetty:servlet-api:jar:2.5-20081211",
    "org.mortbay.jetty:jetty-util:jar:#{JETTY_VERSION_6}",
    "org.mortbay.jetty:jetty:jar:#{JETTY_VERSION_6}",
    "org.mortbay.jetty:jsp-2.1:jar:#{JETTY_VERSION_6}",
    "org.mortbay.jetty:jsp-api-2.1:jar:#{JETTY_VERSION_6}",
    ]

HADOOP_API = [
    "javax.activation:activation:jar:1.1",
    "javax.xml.bind:jaxb-api:jar:2.2.2",
    "javax.xml.stream:stax-api:jar:1.0-2",
    "javax.ws.rs:jsr311-api:jar:1.1.1"
    ]

HADOOP_DEPS = [
    "hsqldb:hsqldb:jar:1.8.0.10",
    "net.java.dev.jets3t:jets3t:jar:0.7.1",
    "net.sf.kosmosfs:kfs:jar:0.3",
    "org.eclipse.jdt:core:jar:3.1.1",
    "oro:oro:jar:2.0.8",
    "tomcat:jasper-compiler:jar:5.5.12",
    "tomcat:jasper-runtime:jar:5.5.12",
    "xmlenc:xmlenc:jar:0.52"
    ] + HADOOP_API

GUAVA    = [ "com.google.guava:guava:jar:11.0.2" ]
AVRO     = [ "org.apache.avro:avro:jar:1.7.3" ]
PROTOBUF = [ "com.google.protobuf:protobuf-java:jar:2.4.0a" ]

CDH_VERSION       = "cdh4.2.1"
HADOOP_MR_VERSION = "2.0.0-mr1-#{CDH_VERSION}"
HADOOP_VERSION    = "2.0.0-#{CDH_VERSION}"
HADOOP_ZK_VERSION = "3.4.5-#{CDH_VERSION}"

HADOOP = [
    "org.apache.hadoop:hadoop-core:jar:#{HADOOP_MR_VERSION}",
    "org.apache.hadoop:hadoop-tools:jar:#{HADOOP_MR_VERSION}",
    "org.apache.hadoop:hadoop-annotations:jar:#{HADOOP_VERSION}",
    "org.apache.hadoop:hadoop-auth:jar:#{HADOOP_VERSION}",
    "org.apache.hadoop:hadoop-client:jar:#{HADOOP_VERSION}",
    "org.apache.hadoop:hadoop-common:jar:#{HADOOP_VERSION}",
    "org.apache.hadoop:hadoop-hdfs:jar:#{HADOOP_VERSION}",
    "org.apache.zookeeper:zookeeper:jar:#{HADOOP_ZK_VERSION}"
] + ANT + COMMONS_CLI + COMMONS_CODEC + COMMONS_EL + COMMONS_LOGGING +
    COMMONS_NET + JETTY_6 + JUNIT + HADOOP_DEPS + HTTPCLIENT + GUAVA +
    COMMONS_CONFIGURATION + COMMONS_LANG + AVRO + PROTOBUF + COMMONS_COLLECTIONS

SEWER = [ "net.pixelcop.sewer:sewer:jar:0.5.2" ] + HADOOP

class Buildr::Artifact
  def <=>(other)
    self.id <=> other.id
  end
end

def add_artifacts(*args)
  args = [args].flatten
  arts = args.find_all{ |a| a.kind_of? Buildr::Artifact }
  arts += artifacts( args.reject{ |a| a.kind_of? Buildr::Artifact }.reject{|j| j =~ /:pom:/}.sort.uniq )
  arts.sort
end
