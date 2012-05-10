#!/usr/bin/env bash

# Set Spark environment variables for your site in this file. Some useful
# variables to set are:
# - MESOS_HOME, to point to your Mesos installation
# - SCALA_HOME, to point to your Scala installation
# - SPARK_CLASSPATH, to add elements to Spark's classpath
# - SPARK_JAVA_OPTS, to add JVM options
# - SPARK_MEM, to change the amount of memory used per node (this should
#   be in the same format as the JVM's -Xmx option, e.g. 300m or 1g).
# - SPARK_LIBRARY_PATH, to add extra search paths for native libraries.

export SCALA_HOME=/root/scala-2.9.0.1
export MESOS_HOME=/root/mesos

# Set Spark's memory per machine -- you might want to increase this
export SPARK_MEM=4g
export SPARK_CLASSPATH=/root/SparkOpt/lib/parallelcolt-0.9.4.jar:/root/SparkOpt/lib/json.jar:/root/SparkOpt/lib/jtransforms.jar:/root/SparkOpt/lib/jtransforms-2.4.jar:/root/SparkOpt/target/scala-2.9.1/classes
